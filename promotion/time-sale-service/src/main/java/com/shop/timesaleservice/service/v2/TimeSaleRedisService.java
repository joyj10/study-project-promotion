package com.shop.timesaleservice.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.timesaleservice.domain.Product;
import com.shop.timesaleservice.domain.TimeSale;
import com.shop.timesaleservice.domain.TimeSaleOrder;
import com.shop.timesaleservice.domain.TimeSaleStatus;
import com.shop.timesaleservice.dto.TimeSaleDto;
import com.shop.timesaleservice.exception.TimeSaleException;
import com.shop.timesaleservice.repository.ProductRepository;
import com.shop.timesaleservice.repository.TimeSaleOrderRepository;
import com.shop.timesaleservice.repository.TimeSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSaleRedisService {
    private static final String TIME_SALE_KEY = "time-sale:";
    private static final String TIME_SALE_LOCK = "time-sale-lock:";
    private static final long WAIT_TIME = 3L;
    private static final long LEASE_TIME = 3L;

    private final TimeSaleRepository timeSaleRepository;
    private final ProductRepository productRepository;
    private final TimeSaleOrderRepository timeSaleOrderRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public TimeSale createTimeSale(TimeSaleDto.CreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        TimeSale timeSale = TimeSale.builder()
                .product(product)
                .quantity(request.getQuantity())
                .remainingQuantity(request.getQuantity())
                .discountPrice(request.getDiscountPrice())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(TimeSaleStatus.ACTIVE)
                .build();

        TimeSale savedTimeSale = timeSaleRepository.save(timeSale);
        saveToRedis(savedTimeSale);
        return savedTimeSale;
    }

    private void saveToRedis(TimeSale timeSale) {
        try {
            String json = objectMapper.writeValueAsString(timeSale);

            String key = TIME_SALE_KEY + timeSale.getId();
            redissonClient.getBucket(key).set(json);
            log.info("TimeSale saved to Redis: {}", key);
        } catch (Exception e) {
            log.error("Failed to save TimeSale to Redis: {}", timeSale.getId(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<TimeSale> getOngoingTimeSales(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return timeSaleRepository.findAllByStartAtBeforeAndEndAtAfterAndStatus(
                now, TimeSaleStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public TimeSale getTimeSale(Long timeSaleId) {
        return getFromRedis(timeSaleId);
    }

    private TimeSale getFromRedis(Long timeSaleId) {
        try {
            String key = TIME_SALE_KEY + timeSaleId;
            String json = (String) redissonClient.getBucket(key).get();

            if (json != null) {
                return objectMapper.readValue(json, TimeSale.class);
            }

            // Redis 없는 경우 DB 조회
            TimeSale timeSale = timeSaleRepository.findById(timeSaleId)
                    .orElseThrow(() -> new IllegalArgumentException("TimeSale not found"));
            // Redis에 저장
            saveToRedis(timeSale);
            return timeSale;
        } catch (JsonProcessingException e) {
            throw new TimeSaleException("Failed to parse TimeSale from Redis", e);
        }
    }

    @Transactional
    public TimeSale purchaseTimeSale(Long timeSaleId, TimeSaleDto.PurchaseRequest request) {
        RLock lock = redissonClient.getLock(TIME_SALE_LOCK + timeSaleId);
        if (lock == null) {
            throw new TimeSaleException("Failed to create lock for TimeSale: " + timeSaleId);
        }

        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new TimeSaleException("Could not acquire lock for TimeSale: " + timeSaleId);
            }

            TimeSale timeSale = getFromRedis(timeSaleId);
            timeSale.purchase(request.getQuantity());

            // DB에 남은 수량 업데이트
            timeSale = timeSaleRepository.save(timeSale);

            TimeSaleOrder order = TimeSaleOrder.builder()
                    .userId(request.getUserId())
                    .timeSale(timeSale)
                    .quantity(request.getQuantity())
                    .discountPrice(timeSale.getDiscountPrice())
                    .build();

            timeSaleOrderRepository.save(order);
            saveToRedis(timeSale);
            return timeSale;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TimeSaleException("Interrupted while trying to acquire lock for TimeSale: " + timeSaleId, e);
        } finally {
            if (isLocked) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("Failed to unlock : " + timeSaleId, e);
                }
            }
        }
    }

}

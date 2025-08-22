package com.shop.couponservice.service.v2;

import com.shop.couponservice.config.UserIdInterceptor;
import com.shop.couponservice.dto.v1.CouponDto;
import com.shop.couponservice.entity.Coupon;
import com.shop.couponservice.entity.CouponPolicy;
import com.shop.couponservice.exception.CouponIssueException;
import com.shop.couponservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponRedisService {
    private final RedissonClient redissonClient;
    private final CouponRepository couponRepository;
    private final CouponPolicyService couponPolicyService;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_LOCK_KEY = "coupon:lock:";
    private static final long LOCK_WAIT_TIME = 3;
    private static final long LOCK_LEASE_TIME = 5;

    @Transactional
    public Coupon issueCoupon(CouponDto.IssueRequest request) {
        String quantityKey = COUPON_QUANTITY_KEY + request.getCouponPolicyId();
        String lockKey = COUPON_LOCK_KEY + request.getCouponPolicyId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new CouponIssueException("쿠폰 발급 요청이 많아 처리할 수 업습니다. 잠시 후 다시 시도해주세요.");
            }

            CouponPolicy couponPolicy = couponPolicyService.getCouponPolicy(request.getCouponPolicyId());

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
                throw new IllegalStateException("쿠폰 발급 기간이 아닙니다.");
            }

            // 수량 체크 및 감소
            RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
            long remainingQuantity = atomicQuantity.decrementAndGet();

            if (remainingQuantity < 0) {
                atomicQuantity.incrementAndGet(); // 수량이 음수가 되면 다시 증가시킴
                throw new CouponIssueException("쿠폰이 모두 소진되었습니다.");
            }

            // 쿠폰 발급
            return couponRepository.save(
                    Coupon.builder()
                            .couponPolicy(couponPolicy)
                            .userId(UserIdInterceptor.getCurrentUserId())
                            .couponCode(generateCouponCode())
                            .build()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CouponIssueException("쿠폰 발급 중 오류가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String generateCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

}

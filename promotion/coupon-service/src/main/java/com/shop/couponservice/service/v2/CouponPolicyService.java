package com.shop.couponservice.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.couponservice.dto.v1.CouponDto;
import com.shop.couponservice.dto.v1.CouponPolicyDto;
import com.shop.couponservice.entity.CouponPolicy;
import com.shop.couponservice.exception.CouponPolicyNotFoundException;
import com.shop.couponservice.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("couponPolicyServiceV2")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CouponPolicyService {
    private final CouponPolicyRepository couponPolicyRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_POLICY_KEY = "coupon:policy:";

    @Transactional
    public CouponPolicy createCouponPolicy(CouponPolicyDto.CreateRequest request) throws JsonProcessingException {
        CouponPolicy couponPolicy = request.toEntity();
        CouponPolicy savedPolicy = couponPolicyRepository.save(couponPolicy);

        // == Redis에 초기 수량 설정
        String quantityKey = COUPON_QUANTITY_KEY + savedPolicy.getId();

        // Redisson의 분산 카운터 객체(RAtomicLong)를 사용해 Redis에 접근
        RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
        // 쿠폰 총 수량(totalQuantity)을 Redis에 초기값으로 세팅
        atomicQuantity.set(savedPolicy.getTotalQuantity());

        // == Reidis에 쿠폰 정책 정보 저장
        String policyKey = COUPON_POLICY_KEY + savedPolicy.getId();
        String policyJson = objectMapper.writeValueAsString(CouponPolicyDto.Response.from(savedPolicy));
        // Redisson의 RBucket 객체를 사용해 쿠폰 정책 정보를 Redis에 저장
        RBucket<String> bucket = redissonClient.getBucket(policyKey);
        bucket.set(policyJson);

        return savedPolicy;
    }

    public CouponPolicy getCouponPolicy(Long id){
        String policyKey = COUPON_POLICY_KEY + id;
        RBucket<String> bucket = redissonClient.getBucket(policyKey);
        String policyJson = bucket.get();
        if (policyJson != null) {
            try {
                return objectMapper.readValue(policyJson, CouponPolicy.class);

            } catch (JsonProcessingException e) {
                log.error("쿠폰 정책 정보 JSON 파싱 오류: {}", policyJson, e);
            }
        }

        // Redis에 쿠폰 정책 정보가 없으면 DB에서 조회
        return couponPolicyRepository.findById(id)
                .orElseThrow(() -> new CouponPolicyNotFoundException("쿠폰 정책을 찾을 수 없습니다."));
    }

    public List<CouponPolicy> getAllCouponPolicies() {
        return couponPolicyRepository.findAll();
    }
}

package com.shop.couponservice.service.v1;

import com.shop.couponservice.dto.v1.CouponPolicyDto;
import com.shop.couponservice.entity.CouponPolicy;
import com.shop.couponservice.exception.CouponPolicyNotFoundException;
import com.shop.couponservice.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponPolicyService {
    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public CouponPolicy createCouponPolicy(CouponPolicyDto.CreateRequest request) {
        CouponPolicy couponPolicy = request.toEntity();
        return couponPolicyRepository.save(couponPolicy);
    }

    public CouponPolicy getCouponPolicy(Long id) {
        return couponPolicyRepository.findById(id)
                .orElseThrow(() -> new CouponPolicyNotFoundException("쿠폰 정책을 찾을 수 없습니다."));
    }

    public List<CouponPolicy> getAllCouponPolicies() {
        return couponPolicyRepository.findAll();
    }
}

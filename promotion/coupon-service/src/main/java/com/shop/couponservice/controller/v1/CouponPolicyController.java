package com.shop.couponservice.controller.v1;

import com.shop.couponservice.dto.v1.CouponPolicyDto;
import com.shop.couponservice.entity.CouponPolicy;
import com.shop.couponservice.service.v1.CouponPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupon-policies")
@RequiredArgsConstructor
public class CouponPolicyController {
    private final CouponPolicyService couponPolicyService;

    @PostMapping
    public ResponseEntity<CouponPolicyDto.Response> createCouponPolicy(@RequestBody CouponPolicyDto.CreateRequest request) {
        CouponPolicy couponPolicy = couponPolicyService.createCouponPolicy(request);
        return ResponseEntity.ok(CouponPolicyDto.Response.from(couponPolicy));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponPolicyDto.Response> getCouponPolicy(@PathVariable Long id) {
        CouponPolicy couponPolicy = couponPolicyService.getCouponPolicy(id);
        return ResponseEntity.ok(CouponPolicyDto.Response.from(couponPolicy));
    }

    @GetMapping
    public ResponseEntity<List<CouponPolicyDto.Response>> getAllCouponPolicies() {
        return ResponseEntity.ok(
                couponPolicyService.getAllCouponPolicies().stream()
                        .map(CouponPolicyDto.Response::from)
                        .toList()
        );
    }
}

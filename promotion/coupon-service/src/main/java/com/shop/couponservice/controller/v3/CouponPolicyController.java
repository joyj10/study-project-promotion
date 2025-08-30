package com.shop.couponservice.controller.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.shop.couponservice.dto.v1.CouponPolicyDto;
import com.shop.couponservice.service.v2.CouponPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("couponPolicyControllerV3")
@RequestMapping("/api/v3/coupon-policies")
@RequiredArgsConstructor
public class CouponPolicyController {

    private final CouponPolicyService couponPolicyService;

    @PostMapping
    public ResponseEntity<CouponPolicyDto.Response> createCouponPolicy(@RequestBody CouponPolicyDto.CreateRequest request) throws JsonProcessingException {
        return ResponseEntity.ok()
                .body(CouponPolicyDto.Response.from(couponPolicyService.createCouponPolicy(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponPolicyDto.Response> getCouponPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(CouponPolicyDto.Response.from(couponPolicyService.getCouponPolicy(id)));
    }

    @GetMapping
    public ResponseEntity<List<CouponPolicyDto.Response>> getAllCouponPolicies() {
        return ResponseEntity.ok(couponPolicyService.getAllCouponPolicies().stream()
                .map(CouponPolicyDto.Response::from)
                .toList());
    }
}
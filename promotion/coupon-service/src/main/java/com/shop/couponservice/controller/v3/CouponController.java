package com.shop.couponservice.controller.v3;

import com.shop.couponservice.dto.v3.CouponDto;
import com.shop.couponservice.service.v3.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("couponControllerV3")
@RequiredArgsConstructor
@RequestMapping("/api/v3/coupons")
public class CouponController {
    private final CouponService couponService;

    @PostMapping("/issue")
    public ResponseEntity<Void> issueCoupon(@RequestBody CouponDto.IssueRequest request) {
        couponService.requestCouponIssue(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponDto.CouponResponse> useCoupon(
            @PathVariable Long couponId,
            @RequestParam Long orderId
    ) {
        CouponDto.CouponResponse response = CouponDto.CouponResponse.from(couponService.useCoupon(couponId, orderId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<Void> cancelCoupon(@PathVariable Long couponId) {
        CouponDto.CouponResponse.from(couponService.cancelCoupon(couponId));
        return ResponseEntity.ok().build();
    }
}
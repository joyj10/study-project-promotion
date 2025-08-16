package com.shop.couponservice.controller.v1;

import com.shop.couponservice.dto.v1.CouponDto;
import com.shop.couponservice.entity.Coupon;
import com.shop.couponservice.service.v1.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @PostMapping("/issue")
    public ResponseEntity<CouponDto.Response> issueCoupon(@RequestBody CouponDto.IssueRequest request) {
        Coupon coupon = couponService.issueCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CouponDto.Response.from(coupon));
    }

    @PostMapping("/{couponId}/use")
    public ResponseEntity<CouponDto.Response> useCoupon(@PathVariable Long couponId,
                                                        @RequestBody CouponDto.UseRequest request) {
        Coupon coupon = couponService.useCoupon(couponId, request.getOrderId());
        return ResponseEntity.ok(CouponDto.Response.from(coupon));
    }

    @PostMapping("/{couponId}/cancel")
    public ResponseEntity<CouponDto.Response> cancelCoupon(@PathVariable Long couponId) {
        Coupon coupon = couponService.cancelCoupon(couponId);
        return ResponseEntity.ok(CouponDto.Response.from(coupon));
    }

    @GetMapping
    public ResponseEntity<List<CouponDto.Response>> getCoupons(
            @RequestParam(value = "status", required = false) Coupon.Status status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        CouponDto.ListRequest request = CouponDto.ListRequest.builder()
                .status(status)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(couponService.getCoupons(request));
    }
}

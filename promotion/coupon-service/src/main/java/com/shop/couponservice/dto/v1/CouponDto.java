package com.shop.couponservice.dto.v1;


import com.shop.couponservice.entity.Coupon;
import com.shop.couponservice.entity.CouponPolicy;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

public class CouponDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueRequest {
        private Long couponPolicyId;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UseRequest {
        private Long orderId;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListRequest {
        private Coupon.Status status;
        private Integer page;
        private Integer size;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private String couponCode;
        private CouponPolicy.DiscountType discountType;
        private int discountValue;
        private int minimumOrderAmount;
        private int maximumDiscountAmount;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private Coupon.Status status;
        private Long orderId;
        private LocalDateTime usedAt;

        public static Response from(Coupon coupon) {
            CouponPolicy policy = coupon.getCouponPolicy();
            return Response.builder()
                    .id(coupon.getId())
                    .userId(coupon.getUserId())
                    .couponCode(coupon.getCouponCode())
                    .discountType(policy.getDiscountType())
                    .discountValue(policy.getDiscountValue())
                    .minimumOrderAmount(policy.getMinimumOrderAmount())
                    .maximumDiscountAmount(policy.getMaximumDiscountAmount())
                    .validFrom(policy.getStartTime())
                    .validUntil(policy.getEndTime())
                    .status(coupon.getStatus())
                    .orderId(coupon.getOrderId())
                    .usedAt(coupon.getUsedAt())
                    .build();
        }
    }
}

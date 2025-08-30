package com.shop.couponservice.entity;

import com.shop.couponservice.exception.CouponAlreadyUsedException;
import com.shop.couponservice.exception.CouponExpiredException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id")
    private CouponPolicy couponPolicy;

    private Long userId;
    private String couponCode;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Long orderId;
    private LocalDateTime usedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    public enum Status {
        AVAILABLE,
        USED,
        EXPIRED,
        CANCELLED
    }

    @Builder
    public Coupon(Long id, CouponPolicy couponPolicy, Long userId, String couponCode) {
        this.id = id;
        this.couponPolicy = couponPolicy;
        this.userId = userId;
        this.couponCode = couponCode;
        this.status = Status.AVAILABLE;
    }

    public void use(Long orderId) {
        if (status == Status.USED) {
            throw new CouponAlreadyUsedException("이미 사용된 쿠폰입니다.");
        }

        if (isExpired()) {
            throw new CouponExpiredException("만료된 쿠폰입니다.");
        }

        this.status = Status.USED;
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime());
    }

    public boolean isUsed() {
        return this.status == Status.USED;
    }

    public void cancel() {
        if (status != Status.USED) {
            throw new IllegalStateException("사용되지 않은 쿠폰입니다.");
        }
        this.status = Status.CANCELLED;
        this.orderId = null;
        this.usedAt = null;
    }
}

package com.shop.couponservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_policies")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CouponPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(nullable = false)
    private Integer discountValue;

    @Column(nullable = false)
    private Integer minimumOrderAmount;

    @Column(nullable = false)
    private Integer maximumDiscountAmount;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    @Setter
    private LocalDateTime endTime;

    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum DiscountType {
        FIXED_AMOUNT,       // 정액 할인
        PERCENTAGE          // 정률 할인
    }
}

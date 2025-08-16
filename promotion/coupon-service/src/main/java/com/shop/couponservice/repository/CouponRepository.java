package com.shop.couponservice.repository;

import com.shop.couponservice.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.couponPolicy.id = :policyId")
    Long countByCouponPolicyId(Long policyId);

    Page<Coupon> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Coupon.Status status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithLock(Long id);
}

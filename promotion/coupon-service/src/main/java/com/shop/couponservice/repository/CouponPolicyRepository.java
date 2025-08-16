package com.shop.couponservice.repository;

import com.shop.couponservice.entity.CouponPolicy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {

    // 비관적 lock: 동시성 문제 해결 위해 쓰기 잠금, PESSIMISTIC_WRITE: 해당 row를 배타적으로 잠금, 다른 트랜잭션은 대기상태로 블로킹 됨
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cp FROM CouponPolicy cp WHERE cp.id = :id")
    Optional<CouponPolicy> findByIdWithLock(Long id);
}

package com.shop.couponservice.service.v1;

import com.shop.couponservice.config.UserIdInterceptor;
import com.shop.couponservice.dto.v1.CouponDto;
import com.shop.couponservice.entity.Coupon;
import com.shop.couponservice.entity.CouponPolicy;
import com.shop.couponservice.exception.CouponIssueException;
import com.shop.couponservice.exception.CouponNotFoundException;
import com.shop.couponservice.repository.CouponPolicyRepository;
import com.shop.couponservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {
    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final UserIdInterceptor userIdInterceptor;

    /**
     * V1 이슈
     * 1. Race Condition 발생 가능
     * - findByIdWithLock으로 락을 걸지만, 실제 쿠폰 발급 시점에서 다른 트랜잭션이 먼저 쿠폰을 발급할 수 있음
     * - 여러 트랜잭션이 동시에 쿠폰을 발급하려고 할 때, 정책에 정의된 총 수량을 초과할 수 있음 (더 많은 쿠폰 이 발급될 수 있음)
     * 2. 성능 이슈
     * - 매 요청마다 발급 쿠폰 수 카운트 쿼리 실행, 쿠폰 수가 많아질수록 카운트 쿼리 성능 저하 될 수 있음
     * - PESSIMISTIC_WRITE 락을 사용하면 다른 트랜잭션이 대기하게 되어 성능 저하 발생 가능
     * 3. Deadlock 발생 가능성
     * - 여러 트랜잭션이 동시에 쿠폰을 발급하려고 할 때, 트랜잭션 타임아웃 발생 할 수 있음
     * 4. 정확한 수량 보장 어려움
     * - 분산 환경에서 여러 서버 동시에 쿠폰 발급할 경우 DB 레벨의 라만으로는 정확한 수량 제어 어려움
     */
    @Transactional
    public Coupon issueCoupon(CouponDto.IssueRequest request) {
        CouponPolicy couponPolicy = couponPolicyRepository.findByIdWithLock(request.getCouponPolicyId())
                .orElseThrow(() -> new CouponIssueException("쿠폰 정책을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())) {
            throw new CouponIssueException("쿠폰 발급 기간이 아닙니다.");
        }

        long issuedCouponCount = couponRepository.countByCouponPolicyId(couponPolicy.getId());
        if (issuedCouponCount >= couponPolicy.getTotalQuantity()) {
            throw new CouponIssueException("쿠폰 정책에 정의된 총 수량을 초과했습니다.");
        }

        Coupon coupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .userId(UserIdInterceptor.getCurrentUserId())
                .couponCode(generateCouponCode())
                .build();

        return couponRepository.save(coupon);
    }

    private String generateCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Transactional
    public Coupon useCoupon(Long couponId, Long orderId) {
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        Coupon coupon = couponRepository.findByIdAndUserId(couponId, currentUserId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없거나 접근 권한이 없습니다."));

        coupon.use(orderId);
        return coupon;
    }

    @Transactional
    public Coupon cancelCoupon(Long couponId) {
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        Coupon coupon = couponRepository.findByIdAndUserId(couponId, currentUserId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없거나 접근 권한이 없습니다."));

        coupon.cancel();
        return coupon;
    }

    public List<CouponDto.Response> getCoupons(CouponDto.ListRequest request) {
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        return couponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        currentUserId,
                        request.getStatus(),
                        PageRequest.of(
                                request.getPage() != null ? request.getPage() : 0,
                                request.getSize() != null ? request.getSize() : 10
                        )
                ).stream()
                .map(CouponDto.Response::from)
                .toList();
    }
}

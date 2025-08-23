package com.shop.couponservice.service.v2;

import com.shop.couponservice.config.UserIdInterceptor;
import com.shop.couponservice.dto.v1.CouponDto;
import com.shop.couponservice.entity.Coupon;
import com.shop.couponservice.entity.CouponPolicy;
import com.shop.couponservice.exception.CouponIssueException;
import com.shop.couponservice.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponRedisServiceTest {
    @InjectMocks
    private CouponRedisService couponRedisService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponPolicyService couponPolicyService;

    @Mock
    private RLock rLock;

    @Mock
    private RAtomicLong atomicLong;

    private CouponPolicy couponPolicy;
    private Coupon coupon;
    public static final Long TEST_USER_ID  = 1L;
    public static final Long TEST_COUPON_ID  = 1L;
    public static final Long TEST_POLICY_ID  = 1L;

    @BeforeEach
    void setUp() {
        couponPolicy = CouponPolicy.builder()
                .id(TEST_POLICY_ID)
                .name("테스트 쿠폰")
                .discountType(CouponPolicy.DiscountType.FIXED_AMOUNT)
                .discountValue(1000)
                .minimumOrderAmount(10000)
                .maximumDiscountAmount(1000)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        coupon = Coupon.builder()
                .id(TEST_COUPON_ID)
                .userId(TEST_USER_ID)
                .couponPolicy(couponPolicy)
                .couponCode("TEST123")
                .build();
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCoupon_Success() throws InterruptedException {
        // given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(TEST_POLICY_ID)
                .build();
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
        when(atomicLong.decrementAndGet()).thenReturn(99L);
        when(couponPolicyService.getCouponPolicy(TEST_POLICY_ID)).thenReturn(couponPolicy);
        when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);

            // when
            Coupon issuedCoupon = couponRedisService.issueCoupon(request);

            // then
            assertThat(issuedCoupon.getId()).isEqualTo(TEST_COUPON_ID);
            assertThat(issuedCoupon.getUserId()).isEqualTo(TEST_USER_ID);
            verify(couponRepository).save(any(Coupon.class));
            verify(rLock).unlock();

            // then
        }
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 락 획득 실패")
    void issueCoupon_Fail_LockNotAcquired() throws InterruptedException {
        // given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(TEST_POLICY_ID)
                .build();

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> couponRedisService.issueCoupon(request))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("쿠폰 발급 요청이 많아 처리할 수 업습니다. 잠시 후 다시 시도해주세요.");
    }
    
    @Test
    @DisplayName("쿠폰 발급 실패 - 수량 소진")
    void issueCoupon_Fail_NoQuantityLeft() throws InterruptedException {
        // given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(TEST_POLICY_ID)
                .build();

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
        when(atomicLong.decrementAndGet()).thenReturn(-1L);
        when(couponPolicyService.getCouponPolicy(TEST_POLICY_ID)).thenReturn(couponPolicy);

        // when & then
        assertThatThrownBy(() -> couponRedisService.issueCoupon(request))
                .isInstanceOf(CouponIssueException.class)
                .hasMessage("쿠폰이 모두 소진되었습니다.");

        verify(atomicLong).incrementAndGet();
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("쿠폰 발급 실패 - 발급 기간 아님")
    void issueCoupon() throws InterruptedException {
        // given
        CouponDto.IssueRequest request = CouponDto.IssueRequest.builder()
                .couponPolicyId(TEST_POLICY_ID)
                .build();

        CouponPolicy expiredPolicy = CouponPolicy.builder()
                .id(TEST_POLICY_ID)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(couponPolicyService.getCouponPolicy(TEST_POLICY_ID)).thenReturn(expiredPolicy);

        // when & then
        assertThatThrownBy(() -> couponRedisService.issueCoupon(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰 발급 기간이 아닙니다.");

        verify(rLock).unlock();
    }

}
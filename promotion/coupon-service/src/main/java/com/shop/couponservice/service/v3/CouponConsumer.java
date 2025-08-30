package com.shop.couponservice.service.v3;

import com.shop.couponservice.dto.v3.CouponDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponConsumer {
    private final CouponService couponService;

    @KafkaListener(topics = "coupon-issue-requests", groupId = "coupon-service", containerFactory = "kafkaListenerContainerFactory")
    public void consumeCouponIssueRequest(CouponDto.IssueMessage message) {
        try {
            log.info("Received coupon issue request. message: {}", message);
            couponService.issueCoupon(message);
        } catch (Exception e) {
            log.error("Error processing coupon issue request message: {}", message, e);
        }
    }
}

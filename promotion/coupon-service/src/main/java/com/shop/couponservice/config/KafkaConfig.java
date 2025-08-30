package com.shop.couponservice.config;

import com.shop.couponservice.dto.v3.CouponDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String GROUP_ID = "coupon-service";

    // Kafka ProducerFactory 설정 (Coupon 발급 이벤트 전송용)
    @Bean
    public ProducerFactory<String, CouponDto.IssueMessage> couponProducerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS); // Kafka 브로커 주소 설정 (여러 개면 콤마 구분)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // 메시지 key는 String 타입 직렬화
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // 메시지 value는 JSON 직렬화
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true); // JSON 직렬화 시 타입 정보를 헤더에 포함 → 역직렬화 시 유용

        // 안정성 관련 옵션
        config.put(ProducerConfig.ACKS_CONFIG, "all");  // "all": 리더+팔로워 모두 확인 시 ack → 데이터 유실 방지
        config.put(ProducerConfig.RETRIES_CONFIG, 3);   // 전송 실패 시 재시도 횟수
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);    // 재전송 시 메시지 순서 뒤섞임 방지 (1로 설정 시 순서 보장)

        // 최종적으로 ProducerFactory 생성하여 반환
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, CouponDto.IssueMessage> couponKafkaTemplate() {
        return new KafkaTemplate<>(couponProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, CouponDto.IssueMessage> couponConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 안정성을 위한 추가 설정
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        JsonDeserializer<CouponDto.IssueMessage> jsonDeserializer = new JsonDeserializer<>(CouponDto.IssueMessage.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeMapperForKey(true);  // 타입 매핑 활성화
        jsonDeserializer.setRemoveTypeHeaders(false);   // 헤더 유지

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponDto.IssueMessage> couponKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CouponDto.IssueMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(couponConsumerFactory());

        // 동시성 설정
        factory.setConcurrency(3); // 컨슈머 스레드 수 설정
        factory.getContainerProperties().setPollTimeout(3000); // 폴링 타임아웃 설정
        return factory;
    }
}

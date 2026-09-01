package com.example.msa.template.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * === Kafka Consumer 패턴 === 다른 서비스(또는 자기 자신)가 발행한 이벤트를 구독한다.
 *
 * <p>기본 설정에서는 inbound 토픽이 outbound 토픽과 같아, 이 서비스 하나만 실행해도
 * "발행 → 구독" 한 바퀴를 로그로 확인할 수 있다. 실제로는 다른 서비스의 토픽을 구독한다.
 */
@Slf4j
@Component
public class InboundEventConsumer {

    @KafkaListener(topics = "${kafka.topic.inbound}", groupId = "${spring.application.name:service-template}")
    public void onMessage(Map<String, Object> event) {
        log.info("[Kafka Consumer] 이벤트 수신: {}", event);
        // TODO: 실제 후속 처리 (상태 갱신, 알림, 저장 등)
    }
}

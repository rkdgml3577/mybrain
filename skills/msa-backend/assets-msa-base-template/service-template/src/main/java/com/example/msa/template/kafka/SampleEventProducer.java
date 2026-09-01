package com.example.msa.template.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** === Kafka Producer 패턴 === 이벤트를 outbound 토픽으로 발행한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.outbound}")
    private String outboundTopic;

    public void publish(SampleCreatedEvent event) {
        log.info("[Kafka Producer] '{}' 발행 - id={}, name={}", outboundTopic, event.getId(), event.getName());
        kafkaTemplate.send(outboundTopic, String.valueOf(event.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka Producer] 발행 실패: {}", ex.getMessage());
                    } else {
                        log.info("[Kafka Producer] 발행 성공 - offset={}", result.getRecordMetadata().offset());
                    }
                });
    }
}

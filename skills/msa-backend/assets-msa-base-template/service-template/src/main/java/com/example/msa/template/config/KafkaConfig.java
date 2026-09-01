package com.example.msa.template.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** 이 서비스가 발행하는 토픽을 자동 생성한다. 새 토픽이 필요하면 여기에 Bean 을 추가한다. */
@Configuration
public class KafkaConfig {

    @Bean
    NewTopic sampleCreatedTopic(@Value("${kafka.topic.outbound}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}

package com.example.msa.template.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 발행 이벤트 payload 예시. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleCreatedEvent {
    private Long id;
    private String name;
}

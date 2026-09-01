package com.example.msa.template.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** === @Scheduled 배치 작업 패턴 === 주기적으로 실행되는 작업 예시. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleScheduler {

    private final SampleService sampleService;

    @Scheduled(fixedDelayString = "${scheduler.fixed-delay:60000}")
    public void reportCount() {
        log.info("[Scheduler] 현재 리소스 수: {}", sampleService.count());
    }
}

package com.example.msa.template.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * === 서비스 간 동기 REST 호출 패턴 (WebClient + Eureka LoadBalancer) ===
 *
 * <p>http://{서비스이름}/... 형식의 URL 은 Eureka 로 해석된다.
 * 대상 서비스 이름은 application.yml 의 client.target-service 로 설정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${client.target-service:other-service}")
    private String targetService;

    /** 예시: 다른 서비스의 리소스를 조회한다. */
    public Map<String, Object> fetch(Long id) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("http://{service}/api/resources/{id}", targetService, id)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            log.error("[ExternalServiceClient] '{}' 호출 실패 - id={}, error={}", targetService, id, e.getMessage());
            throw new IllegalStateException(targetService + " 연결에 실패했습니다");
        }
    }
}

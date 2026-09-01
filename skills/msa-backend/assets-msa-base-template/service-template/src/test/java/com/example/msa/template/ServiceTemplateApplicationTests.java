package com.example.msa.template;

import com.example.msa.template.dto.SampleDto;
import com.example.msa.template.kafka.SampleEventProducer;
import com.example.msa.template.service.SampleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ServiceTemplateApplicationTests {

    @Autowired
    private SampleService sampleService;

    // Kafka 브로커 없이 실행하기 위해 이벤트 발행은 목으로 대체
    @MockBean
    private SampleEventProducer sampleEventProducer;

    @Test
    void contextLoads() {
    }

    @Test
    void createAndFind() {
        SampleDto.Response created = sampleService.create(SampleDto.CreateRequest.builder()
                .name("first").description("desc").build());

        assertNotNull(created.getId());
        assertEquals("first", sampleService.findById(created.getId()).getName());
        verify(sampleEventProducer).publish(any());
    }

    @Test
    void updateAndDelete() {
        SampleDto.Response created = sampleService.create(SampleDto.CreateRequest.builder()
                .name("a").build());
        sampleService.update(created.getId(), SampleDto.UpdateRequest.builder().name("b").build());
        assertEquals("b", sampleService.findById(created.getId()).getName());

        sampleService.delete(created.getId());
        assertEquals(0, sampleService.count());
    }
}

package com.example.msa.template.service;

import com.example.msa.template.dto.SampleDto;
import com.example.msa.template.entity.Sample;
import com.example.msa.template.kafka.SampleCreatedEvent;
import com.example.msa.template.kafka.SampleEventProducer;
import com.example.msa.template.repository.SampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleService {

    private final SampleRepository sampleRepository;
    private final SampleEventProducer eventProducer;

    @Transactional
    public SampleDto.Response create(SampleDto.CreateRequest request) {
        Sample sample = sampleRepository.save(Sample.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build());

        // 생성 후 도메인 이벤트 발행 (다른 서비스가 구독 가능)
        eventProducer.publish(SampleCreatedEvent.builder()
                .id(sample.getId())
                .name(sample.getName())
                .build());

        return SampleDto.Response.from(sample);
    }

    @Transactional(readOnly = true)
    public List<SampleDto.Response> findAll() {
        return sampleRepository.findAll().stream().map(SampleDto.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public SampleDto.Response findById(Long id) {
        return SampleDto.Response.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public long count() {
        return sampleRepository.count();
    }

    @Transactional
    public SampleDto.Response update(Long id, SampleDto.UpdateRequest request) {
        Sample sample = getOrThrow(id);
        sample.update(request.getName(), request.getDescription());
        return SampleDto.Response.from(sample);
    }

    @Transactional
    public void delete(Long id) {
        sampleRepository.delete(getOrThrow(id));
    }

    private Sample getOrThrow(Long id) {
        return sampleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리소스를 찾을 수 없습니다: " + id));
    }
}

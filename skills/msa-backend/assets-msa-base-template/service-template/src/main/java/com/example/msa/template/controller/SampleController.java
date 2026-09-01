package com.example.msa.template.controller;

import com.example.msa.template.dto.ApiResponse;
import com.example.msa.template.dto.SampleDto;
import com.example.msa.template.service.SampleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * === REST CRUD 패턴 ===
 * 경로 prefix(/api/resources)는 새 서비스에 맞게 바꾼다(예: /api/orders).
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class SampleController {

    private final SampleService sampleService;

    @PostMapping
    public ResponseEntity<ApiResponse<SampleDto.Response>> create(
            @Valid @RequestBody SampleDto.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sampleService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SampleDto.Response>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(sampleService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SampleDto.Response>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(sampleService.findById(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SampleDto.Response>> update(
            @PathVariable Long id, @Valid @RequestBody SampleDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sampleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        sampleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // === 서비스 간 내부 호출용 (다른 서비스가 이 서비스를 조회할 때) ===
    @GetMapping("/internal/exists/{id}")
    public boolean exists(@PathVariable Long id) {
        try {
            sampleService.findById(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

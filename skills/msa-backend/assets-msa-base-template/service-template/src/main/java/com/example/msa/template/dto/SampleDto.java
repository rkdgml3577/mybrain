package com.example.msa.template.dto;

import com.example.msa.template.entity.Sample;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SampleDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "name 은 필수입니다")
        @Size(max = 200)
        private String name;

        @Size(max = 1000)
        private String description;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        @Size(max = 200)
        private String name;

        @Size(max = 1000)
        private String description;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Sample sample) {
            return Response.builder()
                    .id(sample.getId())
                    .name(sample.getName())
                    .description(sample.getDescription())
                    .createdAt(sample.getCreatedAt())
                    .updatedAt(sample.getUpdatedAt())
                    .build();
        }
    }
}

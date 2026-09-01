package com.example.msa.template.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자리표시 도메인 엔티티. 새 서비스를 만들 때 이 클래스를 실제 도메인으로 교체한다.
 * (예: Sample -> Order, name -> productName 등)
 */
@Entity
@Table(name = "samples")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sample extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    public void update(String name, String description) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
    }
}

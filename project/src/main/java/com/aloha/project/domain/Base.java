package com.aloha.project.domain;

import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@MappedSuperclass
public class Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // PK 자동증가
    private Long no;                    // PK

    @Column(unique = true, nullable = false, updatable = false)
    private String id;                  // ID

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;             // 생성일

    @UpdateTimestamp
    private Date updatedAt;             // 수정일

    public Base() {
        this.id = UUID.randomUUID().toString();         // UUID로 ID 생성
    }

}

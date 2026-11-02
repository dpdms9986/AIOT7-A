package com.aloha.project.repository.tasks;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.WorkAssignmentResources;

public interface WorkAssignmentResourceRepository
        extends JpaRepository<WorkAssignmentResources, Long> {

    // 작업 배정 번호로 선택된 드론과 약제 조회
    Optional<WorkAssignmentResources> findByWorkAssignment_No(Long workAssignmentNo);

}
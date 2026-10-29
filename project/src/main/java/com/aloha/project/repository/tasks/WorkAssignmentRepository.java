package com.aloha.project.repository.tasks;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.WorkAssignments;

import java.util.List;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignments, Long> {
    // 특정 조종사의 배정 작업 목록 조회 (추후 PilotController에서 사용)
    List<WorkAssignments> findByPilotNo(Long pilotNo);
}
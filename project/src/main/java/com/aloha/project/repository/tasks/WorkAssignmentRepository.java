package com.aloha.project.repository.tasks;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.WorkAssignments;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignments, Long> {

    // 특정 조종사에게 배정된 작업 목록 조회
    List<WorkAssignments> findByPilot_No(Long pilotNo);

}
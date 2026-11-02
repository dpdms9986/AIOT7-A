package com.aloha.project.repository.tasks;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.Teams;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignments, Long> {

    // 특정 조종사에게 배정된 작업 목록 조회
    List<WorkAssignments> findByPilot_No(Long pilotNo);

    // 같은 팀 조종사에게 배정된 특정 상태의 작업 목록 조회
    List<WorkAssignments> findByPilot_TeamAndStatus(Teams team, String status);
}
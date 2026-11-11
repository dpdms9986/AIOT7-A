package com.aloha.project.repository.tasks;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.lands.Lands;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.Teams;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignments, Long> {

    // 특정 조종사에게 배정된 작업 목록 조회
    List<WorkAssignments> findByPilot_No(Long pilotNo);

    // 같은 팀 조종사에게 배정된 특정 상태의 작업 목록 조회
    List<WorkAssignments> findByPilot_TeamAndStatus(Teams team, String status);

    List<WorkAssignments> findByLand(Lands land);

    // 특정 조종사의 진행 중인 작업 중 가장 최근 작업 조회
    Optional<WorkAssignments> findFirstByPilot_NoAndStatusOrderByStartedAtDesc(
        Long pilotNo, String status);
}
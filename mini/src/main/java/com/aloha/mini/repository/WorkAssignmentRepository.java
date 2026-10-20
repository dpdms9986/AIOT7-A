package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.WorkAssignments;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignments, Long>{
    // 조종사 본인에게 할당된 필지 목록 조회 (PLT_004)
    List<WorkAssignments> findByPilot_NoOrderByCreatedAtDesc(Long usersNo);

    // 조종사 본인에게 할당된 특정 상태(ASSIGNED 등) 필지 조회
    List<WorkAssignments> findByPilot_NoAndStatus(Long usersNo, String status);

    // 방제단 전체 작업 이력 조회 (MNG_008)
    List<WorkAssignments> findByDispatchBatch_Team_NoOrderByCreatedAtDesc(Long teamsNo);
}

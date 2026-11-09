package com.aloha.project.repository.tasks;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.WorkAssignmentResources;

public interface WorkAssignmentResourceRepository
        extends JpaRepository<WorkAssignmentResources, Long> {

    // 작업 배정 번호로 선택된 드론과 약제 조회
    Optional<WorkAssignmentResources> findByWorkAssignment_No(Long workAssignmentNo);

    // 드론이 작업 배정에 사용 중인지 확인
    boolean existsByDrone_No(Long droneNo);

    // 약제가 작업 배정에 사용 중인지 확인
    boolean existsByChemical_No(Long chemicalNo);

}


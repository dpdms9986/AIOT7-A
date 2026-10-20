package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.DispatchBatches;

public interface DispatchBatchRepository extends JpaRepository<DispatchBatches, Long> {
    // 우리 방제단의 배정 묶음 내역 최신순 조회
    List<DispatchBatches> findByTeam_NoOrderByCreatedAtDesc(Long teamsNo);
}

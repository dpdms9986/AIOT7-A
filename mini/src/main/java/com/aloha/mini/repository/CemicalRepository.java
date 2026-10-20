package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Chemicals;

public interface CemicalRepository extends JpaRepository<Chemicals, Long> {
// 로그인한 사용자의 방제단(teams.no) 기준 약제 재고 목록 조회 (AST_006, AST_007)
    List<Chemicals> findByTeam_NoOrderByCreatedAtDesc(Long teamsNo);
}

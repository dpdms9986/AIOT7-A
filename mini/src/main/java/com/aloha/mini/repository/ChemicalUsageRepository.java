package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.ChemicalUsages;

public interface ChemicalUsageRepository extends JpaRepository<ChemicalUsages, Long> {
    // 우리 방제단의 약제 소모 이력 전체 조회 (AST_010)
    List<ChemicalUsages> findByChemical_Team_NoOrderByUsedAtDesc(Long teamsNo);
}

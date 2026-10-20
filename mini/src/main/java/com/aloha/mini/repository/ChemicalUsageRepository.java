package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.ChemicalUsages;

public interface ChemicalUsageRepository extends JpaRepository<ChemicalUsages, Long> {
    
}

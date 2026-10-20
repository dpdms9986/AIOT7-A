package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Chemicals;

public interface CemicalRepository extends JpaRepository<Chemicals, Long> {
    
}

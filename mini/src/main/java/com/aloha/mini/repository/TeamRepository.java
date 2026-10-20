package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Teams;

public interface TeamRepository extends JpaRepository<Teams, Long> {
    
}

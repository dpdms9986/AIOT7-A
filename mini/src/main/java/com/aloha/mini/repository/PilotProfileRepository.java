package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.PilotProfiles;

public interface PilotProfileRepository extends JpaRepository<PilotProfiles, Long> {
    
}

package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Regions;

public interface RegionRepository extends JpaRepository<Regions, Long> {
    
}

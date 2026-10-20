package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.HazardImages;

public interface HazardImageRepository extends JpaRepository<HazardImages, Long> {
    
}

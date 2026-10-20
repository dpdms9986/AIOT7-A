package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Lands;

public interface LandRepository extends JpaRepository<Lands, Long>{
    
}

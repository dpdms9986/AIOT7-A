package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Drones;

public interface DroneRepository extends JpaRepository<Drones, Long>{
    
}

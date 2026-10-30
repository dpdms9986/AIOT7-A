package com.aloha.project.repository.tasks;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.Drones;

public interface DroneRepository extends JpaRepository<Drones, Long> {
    
}

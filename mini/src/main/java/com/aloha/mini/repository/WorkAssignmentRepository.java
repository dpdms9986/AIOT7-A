package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.WorkAssignments;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignments, Long>{
    
}

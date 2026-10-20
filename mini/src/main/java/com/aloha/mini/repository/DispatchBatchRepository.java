package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.DispatchBatches;

public interface DispatchBatchRepository extends JpaRepository<DispatchBatches, Long> {
    
}

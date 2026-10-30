package com.aloha.project.repository.tasks;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.DispatchBatches;

public interface DispatchBatchRepository extends JpaRepository<DispatchBatches, Long> {
}
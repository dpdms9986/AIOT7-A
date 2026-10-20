package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.HazardComments;

public interface HazardCommentRepository extends JpaRepository<HazardComments, Long> {
    
}

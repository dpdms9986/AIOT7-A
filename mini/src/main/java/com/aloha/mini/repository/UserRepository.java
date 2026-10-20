package com.aloha.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Users;

public interface UserRepository extends JpaRepository<Users, Long>{
    
}

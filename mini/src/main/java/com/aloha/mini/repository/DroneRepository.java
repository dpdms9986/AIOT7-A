package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Drones;

public interface DroneRepository extends JpaRepository<Drones, Long>{
    // 특정 조종사가 보유한 드론 목록 조회 (GNB_007, USER_011)
    List<Drones> findByUser_No(Long usersNo);

    // 우리 방제단 소속 전체 드론 목록 조회 (AST_002)
    List<Drones> findByUser_Team_No(Long teamsNo);

    // 시리얼 번호 중복 확인 (AST_001)
    boolean existsBySerialNumber(String serialNumber);
}

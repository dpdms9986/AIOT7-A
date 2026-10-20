package com.aloha.mini.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.PilotProfiles;

public interface PilotProfileRepository extends JpaRepository<PilotProfiles, Long> {
    // 회원 고유 번호(users.no)로 조종사 프로필 단건 조회
    Optional<PilotProfiles> findByUser_No(Long usersNo);

    // 우리 방제단 소속 조종사들의 실시간 위치 일괄 폴링 (ADM_023)
    List<PilotProfiles> findByUser_Team_No(Long teamsNo);
}

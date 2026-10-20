package com.aloha.mini.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Users;

public interface UserRepository extends JpaRepository<Users, Long>{
    // 이메일 중복 확인 (USER_002)
    boolean existsByUserId(String userId);

    // 로그인 시 이메일로 회원 조회 (USER_008)
    Optional<Users> findByUserId(String userId);

    // 특정 방제단 소속 조종사 목록 조회 (ADM_017, MNG_005)
    List<Users> findByTeam_NoAndRole(Long teamsNo, String role);
}

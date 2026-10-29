package com.aloha.project.repository.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.users.Teams;

public interface TeamRepository extends JpaRepository<Teams, Long> {
    
    // 조종사 가입 시 6자리 초대 코드로 방제단 조회 (USER_005)
    Optional<Teams> findByInviteCode(String inviteCode);

    // 초대 코드 중복 생성 방지용
    boolean existsByInviteCode(String inviteCode);
}
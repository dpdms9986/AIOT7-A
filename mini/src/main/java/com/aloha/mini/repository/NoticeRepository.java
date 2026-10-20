package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.Notices;

public interface NoticeRepository extends JpaRepository<Notices, Long> {
    // 우리 방제단(teams.no)의 공지사항을 중요 고정(isPinned) 우선 + 최신순으로 조회 (BRD_008, BRD_009)
    List<Notices> findByTeam_NoOrderByIsPinnedDescCreatedAtDesc(Long teamsNo);
}

package com.aloha.mini.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.mini.domain.HazardComments;

public interface HazardCommentRepository extends JpaRepository<HazardComments, Long> {
    // 특정 위험 신고 게시글의 댓글 목록 작성순 조회 (BRD_007)
    List<HazardComments> findByHazardReport_NoOrderByCreatedAtAsc(Long hazardReportsNo);
}

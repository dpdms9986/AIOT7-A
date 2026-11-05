package com.aloha.project.repository.common;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.common.HazardComments;
import com.aloha.project.domain.common.HazardReports;

public interface HazardCommentRepository extends JpaRepository<HazardComments, Long> {

    // 특정 위험글에 달린 댓글들을 등록순(오래된 순)으로 조회
    @EntityGraph(attributePaths = {"user"})
    List<HazardComments> findByHazardOrderByCreatedAtAsc(HazardReports hazard);

    // 게시글 삭제 시 연결된 댓글도 일괄 삭제
    void deleteByHazard(HazardReports hazard);
}
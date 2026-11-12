
package com.aloha.project.repository.common;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aloha.project.domain.common.Inquiries;

public interface InquiryRepository extends JpaRepository<Inquiries, Long> {

    // 1. 내 문의내역 조회 (최신순)
    @EntityGraph(attributePaths = "user")
    List<Inquiries> findByUser_NoOrderByCreatedAtDesc(Long userNo);

    // 2. 내 문의 상세 조회 (본인 문의만 조회)
    @EntityGraph(attributePaths = "user")
    Optional<Inquiries> findByNoAndUser_No(Long no, Long userNo);

    // 3. 관리자 전체 문의 목록 조회 및 검색
    // 상태, 문의 유형, 작성자 이름 또는 문의 제목으로 검색
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT i
            FROM Inquiries i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:category IS NULL OR i.category = :category)
              AND (
                    :keyword IS NULL
                    OR LOWER(i.user.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    )
            ORDER BY CASE WHEN i.status = 'WAITING' THEN 0 ELSE 1 END,
                     i.createdAt DESC
            """)
    Page<Inquiries> findAllForAdmin(
            @Param("status") String status,
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 4. 답변 대기 문의 건수 조회
    long countByStatus(String status);

}

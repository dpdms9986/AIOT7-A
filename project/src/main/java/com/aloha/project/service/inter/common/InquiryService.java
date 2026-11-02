
package com.aloha.project.service.inter.common;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aloha.project.domain.common.Inquiries;

public interface InquiryService {

    // 1. 1:1 문의 등록
    boolean create(Inquiries inquiry);

    // 2. 내 문의내역 조회
    List<Inquiries> myList(Long userNo);

    // 3. 내 문의 상세 조회
    Optional<Inquiries> myDetail(Long no, Long userNo);

    // 4. 관리자 전체 문의 목록 조회
    Page<Inquiries> adminList(
            String status,
            String category,
            String keyword,
            Pageable pageable
    );

    // 5. 답변 대기 문의 건수 조회
    long countWaiting();

    // 6. 관리자 문의 답변 등록
    boolean answer(Long no, String answerContent);

}

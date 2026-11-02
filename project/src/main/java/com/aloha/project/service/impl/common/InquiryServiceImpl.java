
package com.aloha.project.service.impl.common;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.project.domain.common.Inquiries;
import com.aloha.project.repository.common.InquiryRepository;
import com.aloha.project.service.inter.common.InquiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;

    // 1. 1:1 문의 등록
    @Transactional
    @Override
    public boolean create(Inquiries inquiry) {
        try {
            inquiry.setStatus("WAITING");
            inquiryRepository.save(inquiry);

            log.info("고객센터 문의 등록 완료: no={}", inquiry.getNo());
            return true;

        } catch (Exception e) {
            log.error("고객센터 문의 등록 실패", e);
            return false;
        }
    }

    // 2. 내 문의내역 조회
    @Transactional(readOnly = true)
    @Override
    public List<Inquiries> myList(Long userNo) {
        return inquiryRepository.findByUser_NoOrderByCreatedAtDesc(userNo);
    }

    // 3. 내 문의 상세 조회
    @Transactional(readOnly = true)
    @Override
    public Optional<Inquiries> myDetail(Long no, Long userNo) {
        return inquiryRepository.findByNoAndUser_No(no, userNo);
    }

    // 4. 관리자 전체 문의 목록 조회
    @Transactional(readOnly = true)
    @Override
    public Page<Inquiries> adminList(
            String status,
            String category,
            String keyword,
            Pageable pageable) {

        // 빈 검색 조건은 null로 변환
        if (status != null && status.isBlank()) {
            status = null;
        }

        if (category != null && category.isBlank()) {
            category = null;
        }

        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        return inquiryRepository.findAllForAdmin(
                status,
                category,
                keyword,
                pageable
        );
    }

    // 5. 답변 대기 문의 건수 조회
    @Transactional(readOnly = true)
    @Override
    public long countWaiting() {
        return inquiryRepository.countByStatus("WAITING");
    }

    // 6. 관리자 문의 답변 등록
    @Transactional
    @Override
    public boolean answer(Long no, String answerContent) {
        try {
            Optional<Inquiries> optional = inquiryRepository.findById(no);

            if (optional.isEmpty()) {
                log.warn("답변할 문의를 찾을 수 없습니다: no={}", no);
                return false;
            }

            Inquiries inquiry = optional.get();

            // 이미 답변 완료된 문의인지 확인
            if ("COMPLETED".equals(inquiry.getStatus())) {
                log.warn("이미 답변 완료된 문의입니다: no={}", no);
                return false;
            }

            // 답변 내용 확인
            if (answerContent == null || answerContent.isBlank()) {
                log.warn("답변 내용이 비어 있습니다: no={}", no);
                return false;
            }

            inquiry.setAnswerContent(answerContent.trim());
            inquiry.setAnsweredAt(LocalDateTime.now());
            inquiry.setStatus("COMPLETED");

            inquiryRepository.save(inquiry);

            log.info("고객센터 문의 답변 등록 완료: no={}", no);
            return true;

        } catch (Exception e) {
            log.error("고객센터 문의 답변 등록 실패: no={}", no, e);
            return false;
        }
    }
}

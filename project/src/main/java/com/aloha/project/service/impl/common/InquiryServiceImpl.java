package com.aloha.project.service.impl.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

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
        log.info("## 고객센터 문의 등록 시작 ##");

        try {
            inquiryRepository.save(inquiry);

            log.info("고객센터 문의 등록 완료: no={}", inquiry.getNo());
            return true;

        } catch (Exception e) {
            log.error("고객센터 문의 등록 실패: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}
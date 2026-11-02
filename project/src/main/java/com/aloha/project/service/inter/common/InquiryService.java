package com.aloha.project.service.inter.common;

import com.aloha.project.domain.common.Inquiries;

public interface InquiryService {

    // 1:1 문의 등록
    boolean create(Inquiries inquiry);

}
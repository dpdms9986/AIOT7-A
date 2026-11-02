package com.aloha.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class InquiryCreateRequest {

    private String name;        // 문의자 이름
    private String replyEmail;  // 답변 수신 이메일
    private String category;    // 문의 유형 (account / field / drone / etc)
    private String content;     // 문의 내용

}
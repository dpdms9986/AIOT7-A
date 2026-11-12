
package com.aloha.project.dto.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class InquiryCreateRequest {

    private String category;    // 문의 유형 (account / field / drone / etc)
    private String title;       // 문의 제목
    private String content;     // 문의 내용

}

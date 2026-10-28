package com.aloha.project.service.inter.common;

public interface EmailService {

    /**
     * 단순 텍스트 이메일 발송
     * @param to 수신자 이메일
     * @param subject 제목
     * @param text 본문
     */
    void sendSimpleMail(String to, String subject, String text);

    /**
     * HTML 이메일 발송
     * @param to 수신자 이메일
     * @param subject 제목
     * @param html HTML 본문
     */
    void sendHtmlMail(String to, String subject, String html);

    /**
     * 임시 비밀번호 안내 이메일 발송
     * @param to 수신자 이메일
     * @param username 사용자 아이디
     * @param tempPassword 임시 비밀번호
     */
    void sendTempPasswordMail(String to, String username, String tempPassword);

}

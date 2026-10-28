package com.aloha.project.service.impl.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.aloha.project.service.inter.common.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendSimpleMail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("이메일 발송 성공: {}", to);
        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}, error={}", to, e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    @Override
    public void sendHtmlMail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("HTML 이메일 발송 성공: {}", to);
        } catch (MessagingException e) {
            log.error("HTML 이메일 발송 실패: to={}, error={}", to, e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("HTML 이메일 발송 실패: to={}, error={}", to, e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    @Override
    public void sendTempPasswordMail(String to, String username, String tempPassword) {
        String subject = "[비밀번호 재설정] 임시 비밀번호 안내";
        String html = buildTempPasswordHtml(username, tempPassword);
        sendHtmlMail(to, subject, html);
        log.info("임시 비밀번호 이메일 발송 완료: username={}, to={}", username, to);
    }

    private String buildTempPasswordHtml(String username, String tempPassword) {
        return "<!DOCTYPE html>"
            + "<html lang=\"ko\">"
            + "<head><meta charset=\"UTF-8\"></head>"
            + "<body style=\"font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;\">"
            + "  <div style=\"max-width: 500px; margin: 20px auto; background-color: #ffffff;"
            + "              border-radius: 8px; padding: 40px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);\">"
            + "    <h2 style=\"color: #333333; border-bottom: 2px solid #4A90E2; padding-bottom: 10px;\">"
            + "      비밀번호 재설정 안내"
            + "    </h2>"
            + "    <p style=\"color: #555555;\">안녕하세요, <strong>" + username + "</strong> 님.</p>"
            + "    <p style=\"color: #555555;\">요청하신 임시 비밀번호가 발급되었습니다.</p>"
            + "    <div style=\"background-color: #f0f7ff; border: 1px solid #4A90E2; border-radius: 6px;"
            + "                padding: 20px; text-align: center; margin: 20px 0;\">"
            + "      <p style=\"margin: 0; font-size: 13px; color: #888;\">임시 비밀번호</p>"
            + "      <p style=\"margin: 8px 0 0; font-size: 24px; font-weight: bold; letter-spacing: 4px;"
            + "                color: #4A90E2; font-family: monospace;\">" + tempPassword + "</p>"
            + "    </div>"
            + "    <p style=\"color: #e74c3c; font-size: 13px;\">"
            + "      &#9888; 보안을 위해 로그인 후 반드시 비밀번호를 변경해 주세요."
            + "    </p>"
            + "    <hr style=\"border: none; border-top: 1px solid #eeeeee; margin: 30px 0;\">"
            + "    <p style=\"font-size: 12px; color: #aaaaaa; text-align: center;\">"
            + "      본 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해 주세요."
            + "    </p>"
            + "  </div>"
            + "</body>"
            + "</html>";
    }

}

package com.aloha.project.controller.common;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.GpsMessage;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DroneGpsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // 조종사 화면(JS)에서 stompClient.send("/pub/location", ...) 로 보낸 데이터 수신
    @MessageMapping("/location")
    public void handleLocation(GpsMessage message, Principal principal) {

        if (principal == null) return;

        // 1. 스프링 시큐리티 로그인 세션에서 현재 조종사 정보 추출
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CustomUser customUser = (CustomUser) token.getPrincipal();
        Users pilot = userRepository.findById(customUser.getUser().getNo()).orElse(null);

        if (pilot != null && pilot.getTeam() != null) {
            // 2. 클라이언트가 보낸 teamNo을 무시하고, DB에 등록된 진짜 내 팀의 고유 초대코드/팀번호 사용! 
            String safeTeamChannel = pilot.getTeam().getInviteCode(); // pilot.getTeam().getNo().toString()

            message.setPilotName(pilot.getName());  // 조종사 이름도 세션 이름으로 강제 변경
            message.setTeamNo(pilot.getTeam().getNo());

            
            log.info("📡 조종사 위치 수신: 팀={}, 조종사={}, 좌표=({}, {})",
                    message.getTeamNo(), message.getPilotName(), message.getLatitude(), message.getLongitude());
    
            // 3. 내 소속 방제단 채널로만 안전하게 브로드캐스팅(실시간 중계! )
            messagingTemplate.convertAndSend("/sub/team/" +safeTeamChannel , message);
            
        }
    }
}
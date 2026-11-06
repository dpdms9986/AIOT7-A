
package com.aloha.project.controller.common;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
<<<<<<< HEAD
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
=======
>>>>>>> develop
import org.springframework.stereotype.Controller;

import com.aloha.project.domain.users.Users;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.dto.GpsMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DroneGpsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // 조종사 화면에서 전송한 GPS 데이터 수신
    @MessageMapping("/location")
    public void handleLocation(GpsMessage message, Principal principal) {

        // 1. 로그인 사용자 확인
        if (principal == null) {
            log.warn("GPS 수신 거부: 로그인 사용자 정보가 없습니다.");
            return;
        }

        // 2. 로그인 사용자 정보로 실제 조종사 조회
        Users pilot = userRepository.findByUserName(principal.getName())
                .orElse(null);

        if (pilot == null) {
            log.warn("GPS 수신 거부: 사용자를 찾을 수 없습니다. 사용자={}",
                    principal.getName());
            return;
        }

        // 3. 소속 방제단 확인
        if (pilot.getTeam() == null
                || pilot.getTeam().getInviteCode() == null
                || pilot.getTeam().getInviteCode().isBlank()) {

            log.warn("GPS 수신 거부: 소속 방제단이 없습니다. 사용자={}",
                    principal.getName());
            return;
        }

        // 4. 서버 DB 기준으로 실제 방제단 채널과 조종사 정보 확정
        String channelKey = pilot.getTeam().getInviteCode();

        message.setPilotName(pilot.getName());
        message.setTeamNo(pilot.getTeam().getNo());

        log.info("📡 조종사 위치 수신: 사용자={}, 방제단={}, 조종사={}, 좌표=({}, {})",
                principal.getName(),
                channelKey,
                message.getPilotName(),
                message.getLatitude(),
                message.getLongitude());

        // 5. 실제 소속 방제단 채널로 GPS 중계
        messagingTemplate.convertAndSend(
                "/sub/team/" + channelKey,
                message
        );
    }
}

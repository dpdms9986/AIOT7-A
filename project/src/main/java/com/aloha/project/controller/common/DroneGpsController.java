
package com.aloha.project.controller.common;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import com.aloha.project.domain.users.CustomUser;
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
        if (principal == null) return;

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CustomUser customUser = (CustomUser) token.getPrincipal();
        Users pilot = userRepository.findById(customUser.getUser().getNo()).orElse(null);

        if (pilot != null && pilot.getTeam() != null) {
					// 단장 화면(#teamChannelKey)과 똑같이 초대코드로 발송!
					String safeTeamChannel = pilot.getTeam().getInviteCode();

           message.setPilotName(pilot.getName());
           message.setTeamNo(pilot.getTeam().getNo());

           log.info("[실시간 위치 중계] 팀채널 = /sub/team/{}, 조종사={}, 좌표=({}, {})",
                safeTeamChannel, message.getPilotName(), message.getLatitude(), message.getLongitude());

        // 단장화면(/sub/team/1)로 정확하게 발송
          messagingTemplate.convertAndSend("/sub/team/" + safeTeamChannel, message);

        }
		}
}

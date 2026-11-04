package com.aloha.project.controller.common;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.aloha.project.dto.GpsMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DroneGpsController {

    private final SimpMessagingTemplate messagingTemplate;

    // 조종사 화면(JS)에서 stompClient.send("/pub/location", ...) 로 보낸 데이터 수신
    @MessageMapping("/location")
    public void handleLocation(GpsMessage message) {
        log.info("📡 조종사 위치 수신: 팀={}, 조종사={}, 좌표=({}, {})",
                message.getTeamNo(), message.getPilotName(), message.getLatitude(), message.getLongitude());

        // 해당 방제단 채널(/sub/team/{teamNo})을 보고 있는 단장 대시보드로 실시간 중계!
        messagingTemplate.convertAndSend("/sub/team/" + message.getTeamNo(), message);
    }
}
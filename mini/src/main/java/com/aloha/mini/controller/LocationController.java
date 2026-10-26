package com.aloha.mini.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.aloha.mini.dto.DroneLocationDto;

@Controller
@RequiredArgsConstructor
public class LocationController {

    // 특정 채널(주파수)로 데이터를 쏴주는 스프링 부트의 확성기 도구입니다.
    private final SimpMessagingTemplate messagingTemplate;

    // 조종사가 "/pub/location"이라는 주소로 자기 위치를 보내면 이 메서드가 작동합니다.
    // (WebSocketConfig에서 설정한 "/pub"이 앞에 자동으로 붙습니다.)
    @MessageMapping("/location")
    public void sendLocation(DroneLocationDto locationDto) {
        
        // 잘 도착했는지 서버 콘솔창에 한 번 출력해 봅니다.
        // 백엔드 콘솔창에서 데이터 수신 확인용 로그
        System.out.println("수신된 위치: " + locationDto.getPilotName() 
                + " / 위도: " + locationDto.getLatitude() 
                + " / 경도: " + locationDto.getLongitude());

        // 해당 조종사가 속한 팀의 채널("/sub/team/팀번호")을 구독 중인 단장에게 위치 데이터를 그대로 쏴줍니다.
        messagingTemplate.convertAndSend("/sub/team/" + locationDto.getTeamNo(), locationDto);
    }
}
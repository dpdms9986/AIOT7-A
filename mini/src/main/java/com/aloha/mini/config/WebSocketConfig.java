package com.aloha.mini.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 웹소켓 메시지 브로커 기능을 켭니다.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 프론트엔드(JS)에서 처음에 파이프를 꽂을(연결할) 주소입니다.
        registry.addEndpoint("/ws-drone")
                .setAllowedOriginPatterns("*") // 모든 주소에서 연결 허용
                .withSockJS(); // 웹소켓을 지원하지 않는 구형 브라우저에서도 잘 돌아가게 해주는 안전장치
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 2. 단장(구독자)들이 위치 정보를 받아볼 '수신 채널'의 접두사입니다. (예: /sub/team/1)
        registry.enableSimpleBroker("/sub");

        // 3. 조종사(발신자)들이 자기 위치를 서버로 쏠 때 쓸 '발신 주소'의 접두사입니다. (예: /pub/location)
        registry.setApplicationDestinationPrefixes("/pub");
    }
}
package com.aloha.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketSecurity
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 단장이 조종사 위치를 구독(수신)할 경로: /sub/team/{teamNo}
        config.enableSimpleBroker("/sub");
        // 조종사가 위치 데이터를 서버로 보낼(송신) 경로: /pub/location
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // JS에서 new SockJS("/ws-drone") 로 연결하는 엔드포인트 개설!
        registry.addEndpoint("/ws-drone")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
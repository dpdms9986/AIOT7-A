package com.aloha.project.controller.common;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.aloha.project.domain.users.Users;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.impl.tasks.WorkAssignmentInterruptionServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DroneSessionEventListenerController {

    private final UserRepository userRepository;
    private final WorkAssignmentInterruptionServiceImpl interruptionService;

    /**
     * WebSocket 연결 종료 이벤트 감지
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {

        Principal principal = event.getUser();

        if (principal == null) {
            return;
        }

        // 정상 종료(1000), 페이지 이동 등에서 발생할 수 있는 종료(1001)는 제외
        int closeCode = event.getCloseStatus().getCode();

        if (closeCode == 1000 || closeCode == 1001) {
            log.info(
                    "[WebSocket 정상 종료] 사용자={}, 종료 코드={}",
                    principal.getName(),
                    closeCode
            );
            return;
        }

        log.warn(
                "[WebSocket 비정상 종료] 사용자={}, 종료 코드={}",
                principal.getName(),
                closeCode
        );

        // 인증된 사용자 이름으로 실제 회원 조회
        Users pilot = userRepository.findByUserName(principal.getName())
                .orElse(null);

        if (pilot == null || !"PILOT".equals(pilot.getRole())) {
            return;
        }

        // 조종사의 진행 중인 작업 강제 중단
        interruptionService.interruptInProgressAssignments(
                pilot.getNo(),
                "WEBSOCKET_DISCONNECTED"
        );
    }
}
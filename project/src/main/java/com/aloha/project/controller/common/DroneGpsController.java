package com.aloha.project.controller.common;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.GpsMessage;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DroneGpsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final JsonMapper objectMapper;

    // 조종사 화면에서 전송한 GPS 데이터 수신
    @Transactional
    @MessageMapping("/location")
    public void handleLocation(GpsMessage message, Principal principal) {

        // 1. 로그인 여부 확인
        if (principal == null || message == null) {
            return;
        }

        if (!(principal instanceof UsernamePasswordAuthenticationToken)) {
            log.warn("[GPS 수신 실패] 인증 정보 형식이 올바르지 않습니다.");
            return;
        }

        UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) principal;

        if (!(token.getPrincipal() instanceof CustomUser)) {
            log.warn("[GPS 수신 실패] 조종사 인증 정보가 없습니다.");
            return;
        }

        CustomUser customUser = (CustomUser) token.getPrincipal();

        // 2. 실제 로그인 조종사 조회
        Users pilot = userRepository.findById(
                customUser.getUser().getNo()
        ).orElse(null);

        if (pilot == null || pilot.getTeam() == null) {
            return;
        }

        // 3. GPS 좌표 유효성 검사
        Double latitude = message.getLatitude();
        Double longitude = message.getLongitude();

        if (latitude == null || longitude == null
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {

            log.warn("[GPS 수신 실패] 올바르지 않은 좌표입니다.");
            return;
        }

        // 4. 조종사의 진행 중인 작업 조회
        WorkAssignments assignment = workAssignmentRepository
                .findFirstByPilot_NoAndStatusOrderByStartedAtDesc(
                        pilot.getNo(), "IN_PROGRESS"
                )
                .orElse(null);

        // 5. 진행 중인 작업이 있다면 마지막 위치와 이동 경로 저장
        if (assignment != null) {

            // 마지막 GPS 위치 저장
            assignment.setLastLatitude(latitude);
            assignment.setLastLongitude(longitude);
            assignment.setLastLocationAt(LocalDateTime.now());

            // GPS 이동 경로 누적 저장
            appendFlightPath(assignment, latitude, longitude);

            workAssignmentRepository.save(assignment);

            log.info(
                    "[GPS 저장 완료] 작업 번호={}, 조종사={}, 좌표=({}, {})",
                    assignment.getNo(),
                    pilot.getName(),
                    latitude,
                    longitude
            );
        }

        // 6. 단장 화면으로 전송할 데이터 설정
        String safeTeamChannel = pilot.getTeam().getInviteCode();

        message.setPilotName(pilot.getName());
        message.setTeamNo(pilot.getTeam().getNo());

        log.info(
                "[실시간 위치 중계] 팀채널=/sub/team/{}, 조종사={}, 좌표=({}, {})",
                safeTeamChannel,
                message.getPilotName(),
                latitude,
                longitude
        );

        // 7. 기존 단장 화면 실시간 GPS 중계 기능 유지
        messagingTemplate.convertAndSend(
                "/sub/team/" + safeTeamChannel,
                message
        );
    }

    /**
     * GPS 좌표를 flightPath에 누적 저장
     *
     * 저장 형식:
     * [
     *   [위도, 경도],
     *   [위도, 경도]
     * ]
     */
    private void appendFlightPath(
            WorkAssignments assignment,
            Double latitude,
            Double longitude) {

        ArrayNode path;

        try {
            String existingPath = assignment.getFlightPath();

            if (existingPath == null || existingPath.isBlank()) {
                path = objectMapper.createArrayNode();
            } else {
                JsonNode parsedPath = objectMapper.readTree(existingPath);

                if (parsedPath != null && parsedPath.isArray()) {
                    path = (ArrayNode) parsedPath;
                } else {
                    log.warn(
                            "[GPS 경로 초기화] 기존 flightPath가 배열 형식이 아닙니다. 작업 번호={}",
                            assignment.getNo()
                    );
                    path = objectMapper.createArrayNode();
                }
            }

            // [위도, 경도] 형태로 좌표 추가
            ArrayNode point = objectMapper.createArrayNode();
            point.add(latitude);
            point.add(longitude);

            path.add(point);

            assignment.setFlightPath(
                    objectMapper.writeValueAsString(path)
            );

        } catch (JacksonException e) {
            log.error(
                    "[GPS 경로 저장 실패] 작업 번호={}",
                    assignment.getNo(),
                    e
            );
        }
    }
}
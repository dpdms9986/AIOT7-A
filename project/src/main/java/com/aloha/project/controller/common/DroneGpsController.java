
package com.aloha.project.controller.common;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Slf4j
@Controller
@RequiredArgsConstructor
public class DroneGpsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // 서버 메모리에 팀별, 조종사별 실시간 비행 궤적 보관
    // 구조: Map<팀초대코드, Map<조종사명, List<[위도, 경도]>>> 
    private final Map<String, Map<String, List<double[]>>> activeTracks = new ConcurrentHashMap<>();

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
          String pilotName = pilot.getName();

          message.setPilotName(pilotName);
          message.setTeamNo(pilot.getTeam().getNo());

          // 실시간 좌표를 서버 메모리 리스트에 차곡차곡 누적
          activeTracks.computeIfAbsent(safeTeamChannel, k -> new ConcurrentHashMap<>())
                      .computeIfAbsent(pilotName, k -> new CopyOnWriteArrayList<>())
                      .add(new double[]{message.getLatitude(), message.getLongitude()});

           message.setPilotName(pilot.getName());
           message.setTeamNo(pilot.getTeam().getNo());

           log.info("[실시간 위치 중계] 팀채널 = /sub/team/{}, 조종사={}, 좌표=({}, {})",
                safeTeamChannel, message.getPilotName(), message.getLatitude(), message.getLongitude());

        // 단장화면(/sub/team/1)로 정확하게 발송
          messagingTemplate.convertAndSend("/sub/team/" + safeTeamChannel, message);
        }
		}

    // 단장 대시보드가 새로고침될 대 현재 비행 중인 궤적을 복원하기 위한 조회 API
    @GetMapping("/api/drone/active-tracks")
    @ResponseBody
    public ResponseEntity<?> getActiveTracks(@RequestParam("teamCode") String teamCode) {
        Map<String, List<double[]>> teamTracks = activeTracks.getOrDefault(teamCode, Collections.emptyMap());
        return ResponseEntity.ok(teamTracks);
    }
    
}

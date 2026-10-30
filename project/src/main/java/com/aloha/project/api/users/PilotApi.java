
package com.aloha.project.api.users;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.PilotAssignmentResponse;
import com.aloha.project.dto.StatusUpdateRequest;
import com.aloha.project.dto.WorkActionRequest;
import com.aloha.project.service.inter.users.PilotService;
import com.aloha.project.service.inter.users.WorkAssignmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/pilot")
@RequiredArgsConstructor
public class PilotApi {

    private final PilotService pilotService;
    private final WorkAssignmentService workAssignmentService;

    // 1. 내 작업 목록 조회
    @GetMapping("/assignments")
    public ResponseEntity<?> getMyAssignments(
            @AuthenticationPrincipal CustomUser customUser) {

        Users loginPilot = getLoginPilot(customUser);

        if (loginPilot == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("조종사 로그인이 필요합니다.");
        }

        List<PilotAssignmentResponse> list =
                pilotService.getMyAssignments(loginPilot.getNo());

        return ResponseEntity.ok(list);
    }

    // 2. 작업 시작
    @PostMapping("/work/start")
    public ResponseEntity<?> startWork(
            @RequestBody WorkActionRequest req,
            @AuthenticationPrincipal CustomUser customUser) {

        Users loginPilot = getLoginPilot(customUser);

        if (loginPilot == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("조종사 로그인이 필요합니다.");
        }

        pilotService.startWork(loginPilot.getNo(), req);

        return ResponseEntity.ok("작업이 시작되었습니다.");
    }

    // 3. 작업 완료
    @PostMapping("/work/complete")
    public ResponseEntity<?> completeWork(
            @RequestBody WorkActionRequest req,
            @AuthenticationPrincipal CustomUser customUser) {

        Users loginPilot = getLoginPilot(customUser);

        if (loginPilot == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("조종사 로그인이 필요합니다.");
        }

        pilotService.completeWork(loginPilot.getNo(), req);

        return ResponseEntity.ok("작업 완료 보고가 처리되었습니다.");
    }

    // 4. 작업 상태 변경
    @PatchMapping("/assignments/{assignmentNo}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable("assignmentNo") Long assignmentNo,
            @RequestBody StatusUpdateRequest dto,
            @AuthenticationPrincipal CustomUser customUser) {

        Users loginPilot = getLoginPilot(customUser);

        if (loginPilot == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("조종사 로그인이 필요합니다.");
        }

        workAssignmentService.updateAssignmentStatus(
                assignmentNo,
                dto.getStatus(),
                loginPilot.getNo()
        );

        return ResponseEntity.ok(
                Map.of("message",
                        "작업 상태가 " + dto.getStatus() + "(으)로 변경되었습니다.")
        );
    }

    // 로그인한 조종사 확인
    private Users getLoginPilot(CustomUser customUser) {

        if (customUser == null || customUser.getUser() == null) {
            return null;
        }

        Users loginPilot = customUser.getUser();

        if (!"PILOT".equals(loginPilot.getRole())) {
            return null;
        }

        return loginPilot;
    }
}

package com.aloha.mini.api.users;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.mini.dto.PilotAssignmentResponse;
import com.aloha.mini.dto.StatusUpdateRequest;
import com.aloha.mini.dto.UserResponse;
import com.aloha.mini.dto.WorkActionRequest;
import com.aloha.mini.service.inter.users.PilotService;
import com.aloha.mini.service.inter.users.WorkAssignmentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pilot")
@RequiredArgsConstructor
public class PilotApi {

    private final PilotService pilotService;
    private final WorkAssignmentService workAssignmentService;

    // 1. 내 작업 목록 조회 (GET /api/pilot/assignments)
    @GetMapping("/assignments")
    public ResponseEntity<?> getMyAssignments(HttpServletRequest request) {
        UserResponse loginUser = getLoginPilot(request);
        if (loginUser == null) {
            return ResponseEntity.status(401).body("조종사 로그인이 필요합니다.");
        }

        List<PilotAssignmentResponse> list = pilotService.getMyAssignments(loginUser.getUserNo());
        return ResponseEntity.ok(list);
    }

    // 2. 작업 시작 (POST /api/pilot/work/start)
    @PostMapping("/work/start")
    public ResponseEntity<String> startWork(@RequestBody WorkActionRequest req, HttpServletRequest request) {
        UserResponse loginUser = getLoginPilot(request);
        if (loginUser == null) {
            return ResponseEntity.status(401).body("조종사 로그인이 필요합니다.");
        }

        pilotService.startWork(loginUser.getUserNo(), req);
        return ResponseEntity.ok("작업이 시작되었습니다.");
    }

    // 3. 작업 완료 (POST /api/pilot/work/complete)
    @PostMapping("/work/complete")
    public ResponseEntity<String> completeWork(@RequestBody WorkActionRequest req, HttpServletRequest request) {
        UserResponse loginUser = getLoginPilot(request);
        if (loginUser == null) {
            return ResponseEntity.status(401).body("조종사 로그인이 필요합니다.");
        }

        pilotService.completeWork(loginUser.getUserNo(), req);
        return ResponseEntity.ok("작업 완료 보고가 처리되었습니다.");
    }

    // 세션 검증 헬퍼 메서드
    private UserResponse getLoginPilot(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER") == null) {
            return null;
        }
        return (UserResponse) session.getAttribute("LOGIN_USER");
    }

    // 조종사 작업 상태 변경 API
    @PatchMapping("/assignments/{assignmentNo}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable("assignmentNo") Long assignmentNo, 
            @RequestBody StatusUpdateRequest dto) {
        
        workAssignmentService.updateAssignmentStatus(assignmentNo, dto.getStatus());
        
        return ResponseEntity.ok(Map.of("message", "작업 상태가 " + dto.getStatus() + "(으)로 변경되었습니다."));
    }
}
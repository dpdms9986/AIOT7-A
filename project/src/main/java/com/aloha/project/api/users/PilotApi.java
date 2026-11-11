
package com.aloha.project.api.users;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.InterruptedWorkResponse;
import com.aloha.project.dto.PilotAssignmentResponse;
import com.aloha.project.dto.StatusUpdateRequest;
import com.aloha.project.dto.WorkActionRequest;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.tasks.WorkAssignmentService;
import com.aloha.project.service.inter.users.PilotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/pilot")
@RequiredArgsConstructor
public class PilotApi {

    private final PilotService pilotService;
    private final WorkAssignmentService workAssignmentService;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final UserRepository userRepository;

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

    // 1-1. 로그인 조종사의 강제 중단 작업 조회
    @Transactional(readOnly = true)
    @GetMapping("/assignments/interrupted")
    public ResponseEntity<?> getInterruptedAssignments(
            @AuthenticationPrincipal CustomUser customUser) {

        Users loginPilot = getLoginPilot(customUser);

        if (loginPilot == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("조종사 로그인이 필요합니다.");
        }

        List<InterruptedWorkResponse> list = workAssignmentRepository
                .findByPilot_No(loginPilot.getNo())
                .stream()
                .filter(assignment -> "INTERRUPTED".equals(assignment.getStatus())
                        || isReassigned(assignment))
                .map(assignment -> "INTERRUPTED".equals(assignment.getStatus())
                        ? InterruptedWorkResponse.from(assignment)
                        : InterruptedWorkResponse.reassignedFrom(assignment))
                .toList();

        return ResponseEntity.ok(list);
    }

    // 1-2. 강제 중단 작업 재개
    @PostMapping("/assignments/{assignmentNo}/resume")
    public ResponseEntity<?> resumeInterruptedAssignment(
            @PathVariable("assignmentNo") Long assignmentNo,
            @AuthenticationPrincipal CustomUser customUser) {

        Users loginPilot = getLoginPilot(customUser);

        if (loginPilot == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("조종사 로그인이 필요합니다.");
        }

        try {
            workAssignmentService.resumeInterruptedAssignment(
                    assignmentNo, loginPilot.getNo());

            return ResponseEntity.ok(
                    Map.of("message", "중단된 작업을 이어서 진행합니다."));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
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

        if (dto == null || dto.getStatus() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "변경할 작업 상태가 필요합니다."));
        }

        String status = dto.getStatus();

        try {

            // 작업 재개 요청
            if ("IN_PROGRESS".equals(status)) {

                WorkAssignments assignment = workAssignmentRepository
                        .findById(assignmentNo)
                        .orElse(null);

                if (assignment == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "존재하지 않는 작업 배정입니다."));
                }

                // 일시정지된 작업만 이 API에서 재개
                if ("PAUSED".equals(assignment.getStatus())) {

                    workAssignmentService.resumeAssignment(
                            assignmentNo,
                            loginPilot.getNo()
                    );

                    return ResponseEntity.ok(
                            Map.of("message", "일시정지된 작업을 재개합니다."));
                }

                // ASSIGNED 작업은 드론과 약제를 선택한 후 시작해야 함
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "작업 시작은 드론과 약제를 선택한 후 진행할 수 있습니다."
                        ));
            }

            // 일시정지 및 완료 등 일반 상태 변경
            workAssignmentService.updateAssignmentStatus(
                    assignmentNo,
                    status,
                    loginPilot.getNo()
            );

            return ResponseEntity.ok(
                    Map.of("message",
                            "작업 상태가 " + status + "(으)로 변경되었습니다.")
            );

        } catch (IllegalArgumentException | IllegalStateException e) {

            log.warn("작업 상태 변경 실패: 작업 번호={}, 조종사 번호={}, 사유={}",
                    assignmentNo, loginPilot.getNo(), e.getMessage());

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // STOPPED 작업이 같은 배정 묶음/농지로 다른 조종사에게 재배정되었는지 확인
    private boolean isReassigned(WorkAssignments assignment) {

        if (!"STOPPED".equals(assignment.getStatus())
                || assignment.getPilot() == null
                || assignment.getLand() == null
                || assignment.getDispatchBatch() == null) {
            return false;
        }

        Long previousPilotNo = assignment.getPilot().getNo();
        Long batchNo = assignment.getDispatchBatch().getNo();

        return workAssignmentRepository.findByLand(assignment.getLand())
                .stream()
                .anyMatch(other -> !other.getNo().equals(assignment.getNo())
                        && other.getDispatchBatch() != null
                        && batchNo.equals(other.getDispatchBatch().getNo())
                        && other.getPilot() != null
                        && !previousPilotNo.equals(other.getPilot().getNo()));
    }

    // 로그인한 조종사 확인
    private Users getLoginPilot(CustomUser customUser) {

        if (customUser == null || customUser.getUser() == null) {
            return null;
        }

        Users loginPilot = userRepository.findById(customUser.getUser().getNo())
                .orElse(null);

        if (loginPilot == null || !"PILOT".equals(loginPilot.getRole())) {
            return null;
        }

        return loginPilot;
    }
}

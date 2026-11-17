
package com.aloha.project.api.users;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.domain.tasks.WorkAssignmentImages;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.tasks.WorkAssignmentImageGroupResponse;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.tasks.WorkAssignmentImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/leader")
@RequiredArgsConstructor
public class LeaderWorkImageApi {

    private final WorkAssignmentRepository workAssignmentRepository;
    private final UserRepository userRepository;
    private final WorkAssignmentImageService imageService;

    // 로그인한 방제단장 확인
    private Users getLoginLeader(CustomUser customUser) {

        if (customUser == null || customUser.getUser() == null) {
            return null;
        }

        Users leader = userRepository.findById(
                customUser.getUser().getNo()).orElse(null);

        if (leader == null || !"LEADER".equals(leader.getRole())) {
            return null;
        }

        return leader;
    }

    // 작업 사진 조회
    @Transactional(readOnly = true)
    @GetMapping("/task-assignment/{no}/images")
    public ResponseEntity<?> getImages(
            @PathVariable("no") Long assignmentNo,
            @AuthenticationPrincipal CustomUser customUser) {

        Users leader = getLoginLeader(customUser);

        if (leader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "방제단장 로그인이 필요합니다."));
        }

        if (leader.getTeam() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "소속 방제단 정보가 없습니다."));
        }

        WorkAssignments assignment = workAssignmentRepository
                .findById(assignmentNo)
                .orElse(null);

        if (assignment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "작업을 찾을 수 없습니다."));
        }

        // 완료된 작업만 조회
        if (!"COMPLETED".equals(assignment.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "완료된 작업만 조회할 수 있습니다."));
        }

        // 작업 배정에 연결된 방제단 확인
        if (assignment.getDispatchBatch() == null
                || assignment.getDispatchBatch().getTeam() == null
                || !Objects.equals(
                        leader.getTeam().getNo(),
                        assignment.getDispatchBatch().getTeam().getNo())) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "해당 작업을 조회할 권한이 없습니다."));
        }

        List<WorkAssignmentImages> images =
                imageService.getImages(assignmentNo);

        return ResponseEntity.ok(
                WorkAssignmentImageGroupResponse.from(
                        assignmentNo, images));
    }
}

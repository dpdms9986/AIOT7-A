
package com.aloha.project.api.users;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.tasks.WorkAssignmentImageGroupResponse;
import com.aloha.project.domain.tasks.WorkAssignmentImages;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.tasks.WorkAssignmentImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/pilot")
@RequiredArgsConstructor
public class PilotWorkImageApi {

    private final WorkAssignmentRepository workAssignmentRepository;
    private final UserRepository userRepository;
    private final WorkAssignmentImageService imageService;

    // 로그인한 조종사 확인
    private Users getLoginPilot(CustomUser customUser) {

        if (customUser == null || customUser.getUser() == null) {
            return null;
        }

        Users pilot = userRepository.findById(
                customUser.getUser().getNo()).orElse(null);

        if (pilot == null || !"PILOT".equals(pilot.getRole())) {
            return null;
        }

        return pilot;
    }

    // 본인에게 배정된 완료 작업 확인
    private WorkAssignments getMyCompletedAssignment(
            Long assignmentNo, Users pilot) {

        WorkAssignments assignment = workAssignmentRepository
                .findById(assignmentNo)
                .orElse(null);

        if (assignment == null
                || assignment.getPilot() == null
                || !pilot.getNo().equals(assignment.getPilot().getNo())) {

            return null;
        }

        if (!"COMPLETED".equals(assignment.getStatus())) {
            throw new IllegalStateException(
                    "완료된 작업만 사진을 등록할 수 있습니다.");
        }

        return assignment;
    }

    // 작업 사진 조회
    @GetMapping("/assignments/{no}/images")
    public ResponseEntity<?> getImages(
            @PathVariable("no") Long assignmentNo,
            @AuthenticationPrincipal CustomUser customUser) {

        Users pilot = getLoginPilot(customUser);

        if (pilot == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "조종사 로그인이 필요합니다."));
        }

        try {

            WorkAssignments assignment =
                    getMyCompletedAssignment(assignmentNo, pilot);

            if (assignment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "작업을 찾을 수 없습니다."));
            }

            List<WorkAssignmentImages> images =
                    imageService.getImages(assignmentNo);

            return ResponseEntity.ok(
                    WorkAssignmentImageGroupResponse.from(
                            assignmentNo, images));

        } catch (IllegalStateException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // 최초 사진 등록
    @PostMapping(
        value = "/assignments/{no}/images",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadImages(
            @PathVariable("no") Long assignmentNo,
            @RequestParam(value = "beforeImages", required = false)
                    List<MultipartFile> beforeImages,
            @RequestParam(value = "afterImages", required = false)
                    List<MultipartFile> afterImages,
            @AuthenticationPrincipal CustomUser customUser) {

        Users pilot = getLoginPilot(customUser);

        if (pilot == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "조종사 로그인이 필요합니다."));
        }

        try {

            WorkAssignments assignment =
                    getMyCompletedAssignment(assignmentNo, pilot);

            if (assignment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "작업을 찾을 수 없습니다."));
            }

            imageService.registerImages(
                    assignment, beforeImages, afterImages);

            List<WorkAssignmentImages> images =
                    imageService.getImages(assignmentNo);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(WorkAssignmentImageGroupResponse.from(
                            assignmentNo, images));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // 기존 사진 전체 수정
    @PutMapping(
        value = "/assignments/{no}/images",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> updateImages(
            @PathVariable("no") Long assignmentNo,
            @RequestParam(value = "beforeImages", required = false)
                    List<MultipartFile> beforeImages,
            @RequestParam(value = "afterImages", required = false)
                    List<MultipartFile> afterImages,
            @AuthenticationPrincipal CustomUser customUser) {

        Users pilot = getLoginPilot(customUser);

        if (pilot == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "조종사 로그인이 필요합니다."));
        }

        try {

            WorkAssignments assignment =
                    getMyCompletedAssignment(assignmentNo, pilot);

            if (assignment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "작업을 찾을 수 없습니다."));
            }

            imageService.replaceImages(
                    assignment, beforeImages, afterImages);

            List<WorkAssignmentImages> images =
                    imageService.getImages(assignmentNo);

            return ResponseEntity.ok(
                    WorkAssignmentImageGroupResponse.from(
                            assignmentNo, images));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}

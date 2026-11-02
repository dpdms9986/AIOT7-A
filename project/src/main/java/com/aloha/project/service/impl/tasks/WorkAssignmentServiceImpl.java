
package com.aloha.project.service.impl.tasks;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.project.domain.tasks.Chemicals;
import com.aloha.project.domain.tasks.Drones;
import com.aloha.project.domain.tasks.WorkAssignmentResources;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.repository.tasks.ChemicalRepository;
import com.aloha.project.repository.tasks.DroneRepository;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.tasks.WorkAssignmentResourceRepository;
import com.aloha.project.service.inter.tasks.WorkAssignmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WorkAssignmentServiceImpl implements WorkAssignmentService {

    private final WorkAssignmentRepository workAssignmentRepository;
    private final DroneRepository droneRepository;
    private final ChemicalRepository chemicalRepository;
    private final WorkAssignmentResourceRepository workAssignmentResourceRepository;

    // 작업 배정 상태 변경
    @Override
    public void updateAssignmentStatus(
            Long assignmentNo, String status, Long pilotNo) {

        log.info("작업 상태 변경 요청: 작업 번호={}, 조종사 번호={}, 변경 상태={}",
                assignmentNo, pilotNo, status);

        // 1. 작업 배정 조회
        WorkAssignments assignment = workAssignmentRepository.findById(assignmentNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 작업 배정입니다."));

        // 2. 본인에게 배정된 작업인지 확인
        if (!assignment.getPilot().getNo().equals(pilotNo)) {
            throw new IllegalArgumentException(
                    "본인에게 배정된 작업이 아닙니다.");
        }

        String currentStatus = assignment.getStatus();

        // 3. 작업 시작
        if ("IN_PROGRESS".equals(status)) {

            if (!"ASSIGNED".equals(currentStatus)) {
                throw new IllegalStateException(
                        "배정된 작업만 시작할 수 있습니다.");
            }

            if (!"ASSIGNED".equals(assignment.getLand().getStatus())) {
                throw new IllegalStateException(
                        "현재 작업을 시작할 수 없는 농지입니다.");
            }

            assignment.setStartedAt(LocalDateTime.now());
        }

        // 4. 작업 완료
        else if ("COMPLETED".equals(status)) {

            if (!"IN_PROGRESS".equals(currentStatus)) {
                throw new IllegalStateException(
                        "진행 중인 작업만 완료할 수 있습니다.");
            }

            if (!"IN_PROGRESS".equals(assignment.getLand().getStatus())) {
                throw new IllegalStateException(
                        "현재 완료할 수 없는 농지입니다.");
            }

            assignment.setCompletedAt(LocalDateTime.now());
        }

        // 5. 허용되지 않은 상태 변경
        else {
            throw new IllegalArgumentException(
                    "허용되지 않은 작업 상태입니다.");
        }

        // 6. 작업 배정 상태 변경
        assignment.updateStatus(status);

        // 7. 농지 상태 변경
        assignment.getLand().updateStatus(status);

        log.info("작업 상태 변경 완료: 작업 번호={}, 상태={}",
                assignmentNo, status);
    }

    // 드론과 약제를 선택하여 작업 시작
    @Override
    public void startAssignment(
            Long assignmentNo,
            Long pilotNo,
            Long droneNo,
            Long chemicalNo) {

        // 1. 작업 배정 조회
        WorkAssignments assignment = workAssignmentRepository.findById(assignmentNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 작업 배정입니다."));

        // 2. 본인에게 배정된 작업인지 확인
        if (!assignment.getPilot().getNo().equals(pilotNo)) {
            throw new IllegalArgumentException(
                    "본인에게 배정된 작업이 아닙니다.");
        }

        // 3. 선택한 드론 조회
        Drones drone = droneRepository.findById(droneNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 드론입니다."));

        // 4. 선택한 약제 조회
        Chemicals chemical = chemicalRepository.findById(chemicalNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 약제입니다."));

        // 5. 같은 방제단 소속 자원인지 확인
        Long teamNo = assignment.getPilot().getTeam().getNo();

        if (!Objects.equals(drone.getUser().getTeam().getNo(), teamNo)) {
            throw new IllegalArgumentException(
                    "해당 방제단에서 사용할 수 없는 드론입니다.");
        }

        if (!Objects.equals(chemical.getTeam().getNo(), teamNo)) {
            throw new IllegalArgumentException(
                    "해당 방제단에서 사용할 수 없는 약제입니다.");
        }

        // 6. 드론 사용 가능 상태 확인
        if (!"AVAILABLE".equals(drone.getStatus())) {
            throw new IllegalStateException(
                    "현재 사용할 수 없는 드론입니다.");
        }

        // 7. 기존 작업 시작 로직 실행
        updateAssignmentStatus(assignmentNo, "IN_PROGRESS", pilotNo);

        // 8. 선택한 드론과 약제 저장
        WorkAssignmentResources resources = WorkAssignmentResources.builder()
                .workAssignment(assignment)
                .drone(drone)
                .chemical(chemical)
                .build();

        workAssignmentResourceRepository.save(resources);

        log.info("작업 자원 저장 완료: 작업 번호={}, 드론 번호={}, 약제 번호={}",
                assignmentNo, droneNo, chemicalNo);
    }
}

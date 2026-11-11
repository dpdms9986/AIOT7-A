
package com.aloha.project.service.impl.tasks;

import java.time.LocalDateTime;
import java.util.List;
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
        if (assignment.getPilot() == null
                || !Objects.equals(assignment.getPilot().getNo(), pilotNo)) {
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

            assignment.setStartedAt(LocalDateTime.now());
        }

        // 4. 작업 일시정지
        else if ("PAUSED".equals(status)) {

            if (!"IN_PROGRESS".equals(currentStatus)) {
                throw new IllegalStateException(
                        "진행 중인 작업만 일시정지할 수 있습니다.");
            }
        }

        // 5. 작업 완료
        else if ("COMPLETED".equals(status)) {

            if (!"IN_PROGRESS".equals(currentStatus)) {
                throw new IllegalStateException(
                        "진행 중인 작업만 완료할 수 있습니다.");
            }

            assignment.setCompletedAt(LocalDateTime.now());
        }

        // 6. 허용되지 않은 상태 변경
        else {
            throw new IllegalArgumentException(
                    "허용되지 않은 작업 상태입니다.");
        }

        // 7. 작업 배정 상태 변경
        assignment.updateStatus(status);

        // 8. 전체 작업 상태를 기준으로 농지 상태 변경
        updateLandStatus(assignment);

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
        if (assignment.getPilot() == null
                || !Objects.equals(assignment.getPilot().getNo(), pilotNo)) {
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

        // 5. 로그인 조종사와 자산 담당자가 일치하는지 확인
        if (drone.getUser() == null
                || !Objects.equals(drone.getUser().getNo(), pilotNo)) {
            throw new IllegalArgumentException(
                    "본인에게 등록된 드론만 작업에 사용할 수 있습니다.");
        }

        if (chemical.getUser() == null
                || !Objects.equals(chemical.getUser().getNo(), pilotNo)) {
            throw new IllegalArgumentException(
                    "본인이 담당하는 약제만 작업에 사용할 수 있습니다.");
        }

        // 6. 같은 방제단 소속 자원인지 확인
        if (assignment.getPilot().getTeam() == null) {
            throw new IllegalArgumentException(
                    "조종사의 방제단 정보가 없습니다.");
        }

        Long teamNo = assignment.getPilot().getTeam().getNo();

        if (drone.getUser().getTeam() == null
                || !Objects.equals(drone.getUser().getTeam().getNo(), teamNo)) {
            throw new IllegalArgumentException(
                    "해당 방제단에서 사용할 수 없는 드론입니다.");
        }

        if (chemical.getTeam() == null
                || !Objects.equals(chemical.getTeam().getNo(), teamNo)) {
            throw new IllegalArgumentException(
                    "해당 방제단에서 사용할 수 없는 약제입니다.");
        }

        // 7. 드론 사용 가능 상태 확인
        if (!"AVAILABLE".equals(drone.getStatus())) {
            throw new IllegalStateException(
                    "현재 사용할 수 없는 드론입니다.");
        }

        // 8. 기존 작업 시작 로직 실행
        updateAssignmentStatus(assignmentNo, "IN_PROGRESS", pilotNo);

        // 9. 선택한 드론과 약제 저장
        WorkAssignmentResources resources = WorkAssignmentResources.builder()
                .workAssignment(assignment)
                .drone(drone)
                .chemical(chemical)
                .build();

        workAssignmentResourceRepository.save(resources);

        log.info("작업 자원 저장 완료: 작업 번호={}, 드론 번호={}, 약제 번호={}",
                assignmentNo, droneNo, chemicalNo);
    }

    // 일시정지된 작업 재개
    @Override
    public void resumeAssignment(Long assignmentNo, Long pilotNo) {

        resumeWorkAssignment(assignmentNo, pilotNo, "PAUSED");
    }

    // 강제중단된 작업 재개
    @Override
    public void resumeInterruptedAssignment(Long assignmentNo, Long pilotNo) {

        resumeWorkAssignment(assignmentNo, pilotNo, "INTERRUPTED");
    }

    // 기존 작업 재개 공통 처리
    private void resumeWorkAssignment(
            Long assignmentNo, Long pilotNo, String expectedStatus) {

        log.info("작업 재개 요청: 작업 번호={}, 조종사 번호={}, 현재 상태={}",
                assignmentNo, pilotNo, expectedStatus);

        // 1. 작업 배정 조회
        WorkAssignments assignment = workAssignmentRepository.findById(assignmentNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 작업 배정입니다."));

        // 2. 본인에게 배정된 작업인지 확인
        if (assignment.getPilot() == null
                || !Objects.equals(assignment.getPilot().getNo(), pilotNo)) {
            throw new IllegalArgumentException(
                    "본인에게 배정된 작업이 아닙니다.");
        }

        // 3. 지정된 상태의 작업인지 확인
        if (!expectedStatus.equals(assignment.getStatus())) {
            throw new IllegalStateException(
                    "현재 상태에서는 작업을 재개할 수 없습니다.");
        }

        // 4. 기존 작업 상태를 진행 중으로 변경
        // startedAt, flightPath, 마지막 GPS 좌표는 변경하지 않음
        assignment.updateStatus("IN_PROGRESS");

        // 5. 전체 작업 상태를 기준으로 농지 상태 변경
        updateLandStatus(assignment);

        log.info("기존 작업 재개 완료: 작업 번호={}, 조종사 번호={}",
                assignmentNo, pilotNo);
    }

        // 농지에 배정된 전체 작업 상태를 기준으로 농지 상태 변경
        private void updateLandStatus(WorkAssignments assignment) {

                List<WorkAssignments> landAssignments =
                        workAssignmentRepository.findByLand(assignment.getLand());

                // 재배정으로 종료된 이전 작업은 농지 상태 계산에서 제외
                List<WorkAssignments> activeAssignments = landAssignments.stream()
                        .filter(work -> !"STOPPED".equals(work.getStatus()))
                        .toList();

                // 상태를 판단할 작업이 없으면 기존 농지 상태 유지
                if (activeAssignments.isEmpty()) {
                        return;
                }

                // 하나라도 진행 중인 작업이 있다면 농지는 작업 중
                boolean hasInProgress = activeAssignments.stream()
                        .anyMatch(work -> "IN_PROGRESS".equals(work.getStatus()));

                // 모든 유효 작업이 완료되었는지 확인
                boolean allCompleted = activeAssignments.stream()
                        .allMatch(work -> "COMPLETED".equals(work.getStatus()));

                if (hasInProgress) {
                        assignment.getLand().updateStatus("IN_PROGRESS");
                } else if (allCompleted) {
                        assignment.getLand().updateStatus("COMPLETED");
                } else {
                        // ASSIGNED, PAUSED, INTERRUPTED 작업이 남아 있는 경우
                        assignment.getLand().updateStatus("ASSIGNED");
                }
        }
}

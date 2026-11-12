
package com.aloha.project.service.impl.tasks;

import java.time.Duration;
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

        // 작업 배정 조회
        WorkAssignments assignment = workAssignmentRepository.findById(assignmentNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 작업 배정입니다."));

        // 본인에게 배정된 작업인지 확인
        if (assignment.getPilot() == null
                || !Objects.equals(assignment.getPilot().getNo(), pilotNo)) {
            throw new IllegalArgumentException(
                    "본인에게 배정된 작업이 아닙니다.");
        }

        String currentStatus = assignment.getStatus();

        // 작업 시작
        if ("IN_PROGRESS".equals(status)) {

            // 강제 중단 작업은 resumeInterruptedAssignment()으로만 재개
            // 기존 경로와 마지막 GPS 좌표를 보존하기 위한 처리
            if (!"ASSIGNED".equals(currentStatus)) {
                throw new IllegalStateException(
                        "새로 배정된 작업만 시작할 수 있습니다. 중단된 작업은 이어서 재개해 주세요.");
            }

            // 최초 시작 일시 기록
            if (assignment.getStartedAt() == null) {
                assignment.setStartedAt(LocalDateTime.now());
            }
        }

        // 작업 일시정지
        else if ("PAUSED".equals(status)) {

            if (!"IN_PROGRESS".equals(currentStatus)) {
                throw new IllegalStateException(
                        "진행 중인 작업만 일시정지할 수 있습니다.");
            }
        }

        // 작업 중단 (INTERRUPTED)
        else if ("INTERRUPTED".equals(status)) {
            if (!"IN_PROGRESS".equals(currentStatus)) {
                throw new IllegalStateException(
                        "진행 중인 작업만 중단(일시정지)할 수 있습니다.");
            }

            // 중단 일시도 함께 기록
            assignment.setInterruptedAt(LocalDateTime.now());
        }

        // 작업 완료
        else if ("COMPLETED".equals(status)) {

            if (!"IN_PROGRESS".equals(currentStatus)) {
                throw new IllegalStateException(
                        "진행 중인 작업만 완료할 수 있습니다.");
            }

            assignment.setCompletedAt(LocalDateTime.now());
        }

        // 허용되지 않은 상태 변경
        else {
            throw new IllegalArgumentException(
                    "허용되지 않은 작업 상태입니다.");
        }

        // 작업 배정 상태 변경
        assignment.updateStatus(status);

        // 인계받은 작업이 완료되면 이전 조종사의 작업도 완료 처리
        if ("COMPLETED".equals(status)) {
            completePreviousHandover(assignment);
        }

        // 전체 작업 상태를 기준으로 농지 상태 변경
        updateLandStatus(assignment);

        log.info("작업 상태 변경 완료: 작업 번호={}, 상태={}",
                assignmentNo, status);
    }

    // 인계받은 작업 완료 시 이전 조종사의 작업도 완료 처리
    private void completePreviousHandover(
            WorkAssignments completedAssignment) {

        if (completedAssignment.getLand() == null
                || completedAssignment.getDispatchBatch() == null
                || completedAssignment.getCreatedAt() == null
                || completedAssignment.getPilot() == null) {
            return;
        }

        // 같은 농지에 배정된 작업 조회
        List<WorkAssignments> sameLand =
                workAssignmentRepository.findByLand(
                        completedAssignment.getLand());

        // 실제 인계 관계가 확인되는 이전 작업 한 건 조회
        sameLand.stream()

                // 인계 처리된 이전 작업만 대상
                .filter(previous ->
                        "STOPPED".equals(previous.getStatus()))

                // 인계 시각이 있어야 함
                .filter(previous ->
                        previous.getCompletedAt() != null)

                // 같은 배정 묶음인지 확인
                .filter(previous ->
                        previous.getDispatchBatch() != null
                        && Objects.equals(
                                previous.getDispatchBatch().getNo(),
                                completedAssignment.getDispatchBatch().getNo()))

                // 같은 농지의 동일 작업인지 확인
                .filter(previous ->
                        previous.getLand() != null
                        && Objects.equals(
                                previous.getLand().getNo(),
                                completedAssignment.getLand().getNo()))

                // 동일한 작업 분배 비율인지 확인
                .filter(previous ->
                        Objects.equals(
                                previous.getWeightRatio(),
                                completedAssignment.getWeightRatio()))

                // 서로 다른 조종사인지 확인
                .filter(previous ->
                        previous.getPilot() != null
                        && !Objects.equals(
                                previous.getPilot().getNo(),
                                completedAssignment.getPilot().getNo()))

                // 인계 시각과 새 작업 생성 시각이 5초 이내인지 확인
                .filter(previous ->
                        Math.abs(
                                Duration.between(
                                        previous.getCompletedAt(),
                                        completedAssignment.getCreatedAt()
                                ).getSeconds()
                        ) <= 5)

                // 가장 최근에 인계된 작업 선택
                .max(java.util.Comparator.comparing(
                        WorkAssignments::getNo))

                // 이전 작업 완료 처리
                .ifPresent(previous -> {

                    // 상태만 변경하고 인계 시각 및 GPS 기록은 보존
                    previous.updateStatus("COMPLETED");

                    log.info(
                            "인계된 이전 작업 완료 처리: 이전 작업 번호={}, 이전 조종사={}, 완료 작업 번호={}, 완료 조종사={}",
                            previous.getNo(),
                            previous.getPilot().getNo(),
                            completedAssignment.getNo(),
                            completedAssignment.getPilot().getNo()
                    );
                });
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

        // 강제 중단 작업을 일반 시작으로 덮어쓰지 못하도록 사전 차단
        if (!"ASSIGNED".equals(assignment.getStatus())) {
            throw new IllegalStateException(
                    "새로 배정된 작업만 시작할 수 있습니다. 중단 또는 일시정지된 작업은 재개 기능을 이용해 주세요.");
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
        // 기존 작업을 다시 시작하는 경우 자원 연결 행이 이미 있을 수 있으므로 조회 후 갱신
        WorkAssignmentResources resources = workAssignmentResourceRepository
                .findByWorkAssignment_No(assignmentNo)
                .orElseGet(() -> WorkAssignmentResources.builder()
                        .workAssignment(assignment)
                        .build());

        resources.setDrone(drone);
        resources.setChemical(chemical);

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

        log.info("작업 재개 요청: 작업 번호={}, 조종사 번호={}, 재개 대상 상태={}",
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

        // 3. 재배정되어 STOPPED 상태가 된 작업은 이전 조종사가 재개할 수 없음
        if ("STOPPED".equals(assignment.getStatus())) {
            throw new IllegalStateException(
                    "중단된 작업이 다른 조종사에게 재배정되었습니다. 새로운 작업을 선택해 주세요.");
        }

        // 4. 지정된 상태의 작업인지 확인
        if (!expectedStatus.equals(assignment.getStatus())) {
            throw new IllegalStateException(
                    "현재 상태에서는 작업을 재개할 수 없습니다.");
        }

        // 5. 기존 작업 상태를 진행 중으로 변경
        // startedAt, flightPath, 마지막 GPS 좌표 및 interruptedAt은 보존
        assignment.updateStatus("IN_PROGRESS");

        // 6. 전체 작업 상태를 기준으로 농지 상태 변경
        updateLandStatus(assignment);

        log.info("기존 작업 재개 완료: 작업 번호={}, 조종사 번호={}, 이전 상태={}",
                assignmentNo, pilotNo, expectedStatus);
    }

    // 농지에 배정된 전체 작업 상태를 기준으로 농지 상태 변경
    private void updateLandStatus(WorkAssignments assignment) {

        if (assignment.getLand() == null) {
            return;
        }

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

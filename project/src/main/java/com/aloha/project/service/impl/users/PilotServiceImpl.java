
package com.aloha.project.service.impl.users;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.dto.PilotAssignmentResponse;
import com.aloha.project.dto.WorkActionRequest;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.service.inter.tasks.WorkAssignmentService;
import com.aloha.project.service.inter.users.PilotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PilotServiceImpl implements PilotService {

    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkAssignmentService workAssignmentService;

    // 1. 내게 할당된 작업 목록 조회
    @Override
    public List<PilotAssignmentResponse> getMyAssignments(Long pilotNo) {

        List<WorkAssignments> assignments =
                workAssignmentRepository.findByPilot_No(pilotNo);

        return assignments.stream()
                .map(assignment -> {
                    var land = assignment.getLand();

                    return PilotAssignmentResponse.builder()
                            .assignmentNo(assignment.getNo())
                            .landNo(land.getNo())
                            .address(land.getAddress())
                            .latitude(land.getLatitude())
                            .longitude(land.getLongitude())
                            .status(assignment.getStatus())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 2. 작업 시작
    @Override
    @Transactional
    public void startWork(Long pilotNo, WorkActionRequest req) {

        validateRequest(req);

        WorkAssignments assignment = getMyAssignment(
                req.getAssignmentNo(), pilotNo
        );

        if (!req.getLandNo().equals(assignment.getLand().getNo())) {
            throw new IllegalArgumentException(
                    "작업 배정과 농지 정보가 일치하지 않습니다."
            );
        }

        // 작업 배정 및 농지 상태 변경
        workAssignmentService.updateAssignmentStatus(
                assignment.getNo(),
                "IN_PROGRESS",
                pilotNo
        );

        log.info("작업 시작 요청: 조종사 번호={}, 작업 번호={}",
                pilotNo, assignment.getNo());
    }

    // 3. 작업 완료
    @Override
    @Transactional
    public void completeWork(Long pilotNo, WorkActionRequest req) {

        validateRequest(req);

        WorkAssignments assignment = getMyAssignment(
                req.getAssignmentNo(), pilotNo
        );

        if (!req.getLandNo().equals(assignment.getLand().getNo())) {
            throw new IllegalArgumentException(
                    "작업 배정과 농지 정보가 일치하지 않습니다."
            );
        }

        // 작업 배정 및 농지 상태 변경
        workAssignmentService.updateAssignmentStatus(
                assignment.getNo(),
                "COMPLETED",
                pilotNo
        );

        log.info("작업 완료 요청: 조종사 번호={}, 작업 번호={}",
                pilotNo, assignment.getNo());
    }

    // 조종사 본인에게 배정된 작업인지 확인
    private WorkAssignments getMyAssignment(
            Long assignmentNo, Long pilotNo) {

        WorkAssignments assignment =
                workAssignmentRepository.findById(assignmentNo)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "작업 배정 정보를 찾을 수 없습니다."
                        ));

        if (!assignment.getPilot().getNo().equals(pilotNo)) {
            throw new IllegalArgumentException(
                    "본인에게 배정된 작업이 아닙니다."
            );
        }

        return assignment;
    }

    // 요청 데이터 유효성 확인
    private void validateRequest(WorkActionRequest req) {

        if (req == null
                || req.getAssignmentNo() == null
                || req.getLandNo() == null) {

            throw new IllegalArgumentException(
                    "작업 배정 번호와 농지 번호가 필요합니다."
            );
        }
    }

}

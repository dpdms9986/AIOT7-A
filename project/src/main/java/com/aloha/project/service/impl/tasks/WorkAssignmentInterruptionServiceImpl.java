
package com.aloha.project.service.impl.tasks;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkAssignmentInterruptionServiceImpl {

    private final WorkAssignmentRepository workAssignmentRepository;

    /**
     * 조종사의 진행 중인 작업을 강제 중단 처리
     */
    @Transactional
    public int interruptInProgressAssignments(Long pilotNo, String reason) {

        if (pilotNo == null) {
            return 0;
        }

        // 해당 조종사의 전체 작업 조회
        List<WorkAssignments> assignments =
                workAssignmentRepository.findByPilot_No(pilotNo);

        // 진행 중인 작업만 INTERRUPTED로 변경
        List<WorkAssignments> interruptedAssignments = assignments.stream()
                .filter(assignment ->
                        "IN_PROGRESS".equals(assignment.getStatus()))
                .toList();

        // 중단 처리 시각 기록
        if (!interruptedAssignments.isEmpty()) {
            LocalDateTime interruptedAt = LocalDateTime.now();

            interruptedAssignments.forEach(assignment -> {
                assignment.updateStatus("INTERRUPTED");
                assignment.setInterruptedAt(interruptedAt);
            });

            workAssignmentRepository.saveAll(interruptedAssignments);
        }

        log.info(
                "[작업 강제 중단] 조종사 번호={}, 중단 작업 수={}, 사유={}",
                pilotNo,
                interruptedAssignments.size(),
                reason
        );

        return interruptedAssignments.size();
    }
}

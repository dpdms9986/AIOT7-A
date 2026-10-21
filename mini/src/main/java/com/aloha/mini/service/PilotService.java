package com.aloha.mini.service;

import com.aloha.mini.domain.Lands;
import com.aloha.mini.domain.WorkAssignments;
import com.aloha.mini.dto.PilotAssignmentResponse;
import com.aloha.mini.dto.WorkActionRequest;
import com.aloha.mini.repository.LandRepository;
import com.aloha.mini.repository.WorkAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PilotService {

    private final WorkAssignmentRepository workAssignmentRepository;
    private final LandRepository landRepository;

    // 1. 내게 할당된 작업 목록 조회 (PLT_004)
    public List<PilotAssignmentResponse> getMyAssignments(Long pilotNo) {
        return workAssignmentRepository.findByPilot_No(pilotNo)
                .stream()
                .map(PilotAssignmentResponse::from)
                .collect(Collectors.toList());
    }

    // 2. 작업 시작 요청 및 비관적 락 동시성 제어 (PLT_006)
    @Transactional
    public void startWork(Long pilotNo, WorkActionRequest req) {
        // 1) 비관적 락을 걸어 농지 조회 (다른 트랜잭션의 동시 접근 대기 처리)
        Lands land = landRepository.findByNoWithPessimisticLock(req.getLandNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 농지입니다."));

        // 2) 농지 상태 검증: 이미 작업 중이거나 완료된 경우 차단
        if (!"ASSIGNED".equals(land.getStatus())) {
            throw new IllegalStateException("현재 작업을 시작할 수 없는 상태입니다. (현재 상태: " + land.getStatus() + ")");
        }

        // 3) 작업 할당 내역 조회 및 본인 확인
        WorkAssignments assignment = workAssignmentRepository.findById(req.getAssignmentNo())
                .orElseThrow(() -> new IllegalArgumentException("할당된 작업 내역을 찾을 수 없습니다."));

        // 참고: WorkAssignments의 조종사 필드명이 user라면 getUser().getNo()로 변경해 주세요.
        if (!assignment.getPilot().getNo().equals(pilotNo)) {
            throw new IllegalStateException("본인에게 배정된 작업만 시작할 수 있습니다.");
        }

        // 4) 농지 및 작업 할당 상태를 IN_PROGRESS(작업 중)로 변경
        land.updateStatus("IN_PROGRESS");
        assignment.updateStatus("IN_PROGRESS");
    }

    // 3. 작업 완료 보고 (PLT_009)
    @Transactional
    public void completeWork(Long pilotNo, WorkActionRequest req) {
        Lands land = landRepository.findByNoWithPessimisticLock(req.getLandNo())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 농지입니다."));

        if (!"IN_PROGRESS".equals(land.getStatus())) {
            throw new IllegalStateException("작업 중인 농지만 완료 처리할 수 있습니다.");
        }

        WorkAssignments assignment = workAssignmentRepository.findById(req.getAssignmentNo())
                .orElseThrow(() -> new IllegalArgumentException("할당된 작업 내역을 찾을 수 없습니다."));

        if (!assignment.getPilot().getNo().equals(pilotNo)) {
            throw new IllegalStateException("본인의 작업만 완료 보고할 수 있습니다.");
        }

        // 농지 및 작업 할당 상태를 COMPLETED(완료)로 변경
        land.updateStatus("COMPLETED");
        assignment.updateStatus("COMPLETED");
    }
}

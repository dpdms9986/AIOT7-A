package com.aloha.mini.service.impl.users;

import com.aloha.mini.domain.DispatchBatches;
import com.aloha.mini.domain.Lands;
import com.aloha.mini.domain.Users;
import com.aloha.mini.domain.WorkAssignments;
import com.aloha.mini.dto.AssignRequest;
import com.aloha.mini.repository.DispatchBatchRepository;
import com.aloha.mini.repository.LandRepository; // 단수형 확인
import com.aloha.mini.repository.UserRepository;
import com.aloha.mini.repository.WorkAssignmentRepository; // 단수형 확인
import com.aloha.mini.service.inter.users.LeaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본은 읽기 전용, 쓰기 메서드에만 @Transactional 따로 붙임
public class LeaderServiceImpl implements LeaderService {

    private final LandRepository landRepository;
    private final UserRepository userRepository;
    private final DispatchBatchRepository dispatchBatchRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

    // 1. 배정 대기(READY) 상태인 농지 목록 조회
    @Override
    public List<Lands> getReadyLands(Long regionNo) {
        // 해당 지역(regionNo)에서 상태가 READY인 농지만 조회
        return landRepository.findByRegion_NoAndStatus(regionNo, "READY");
    }

    // 2. 선택한 농지들을 조종사에게 배정
    @Override
    @Transactional
    public void assignLandsToPilot(Long adminNo, AssignRequest req) {
        // 단장, 조종사 정보 조회 (없으면 예외)
        Users admin = userRepository.findById(adminNo)
                .orElseThrow(() -> new IllegalArgumentException("단장 정보를 찾을 수 없습니다."));
        Users pilot = userRepository.findById(req.getPilotNo())
                .orElseThrow(() -> new IllegalArgumentException("조종사 정보를 찾을 수 없습니다."));

        // 배정 묶음(배치) 생성 및 저장 - 조종사 1명 기준
        DispatchBatches batch = DispatchBatches.builder()
                .team(admin.getTeam())
                .leader(admin)
                .batchTitle(req.getBatchTitle())
                .assignedPilotCount(1)
                .build();
        batch = dispatchBatchRepository.save(batch);

        // 요청에 담긴 농지 번호들로 농지 목록 조회
        List<Lands> selectedLands = landRepository.findAllById(req.getLandNos());

        // 농지마다 상태 확인 후 배정 처리
        for (Lands land : selectedLands) {
            // READY가 아니면 이미 배정됐거나 작업 중이라 배정 불가
            if (!"READY".equals(land.getStatus())) {
                throw new IllegalStateException("이미 배정되었거나 작업 중인 농지가 포함되어 있습니다: " + land.getAddress());
            }
            // 농지 상태를 ASSIGNED(배정됨)로 변경
            land.updateStatus("ASSIGNED");

            // 작업 배정 정보 생성 및 저장 (배치 + 농지 + 조종사 연결)
            WorkAssignments assignment = WorkAssignments.builder()
                    .dispatchBatch(batch)
                    .land(land)
                    .pilot(pilot) // Users 변수명이 user라면 user(pilot)으로 변경
                    .status("ASSIGNED")
                    .build();
            workAssignmentRepository.save(assignment);
        }
    }
}
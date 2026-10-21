package com.aloha.mini.service;

import com.aloha.mini.domain.DispatchBatches;
import com.aloha.mini.domain.Lands;
import com.aloha.mini.domain.Users;
import com.aloha.mini.domain.WorkAssignments;
import com.aloha.mini.dto.AssignRequest;
import com.aloha.mini.repository.DispatchBatchRepository;
import com.aloha.mini.repository.LandRepository; // 단수형 확인
import com.aloha.mini.repository.UserRepository;
import com.aloha.mini.repository.WorkAssignmentRepository; // 단수형 확인
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final LandRepository landRepository;
    private final UserRepository userRepository;
    private final DispatchBatchRepository dispatchBatchRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

    public List<Lands> getReadyLands(Long regionNo) {
        return landRepository.findByRegion_NoAndStatus(regionNo, "READY");
    }

    @Transactional
    public void assignLandsToPilot(Long adminNo, AssignRequest req) {
        Users admin = userRepository.findById(adminNo)
                .orElseThrow(() -> new IllegalArgumentException("단장 정보를 찾을 수 없습니다."));
        Users pilot = userRepository.findById(req.getPilotNo())
                .orElseThrow(() -> new IllegalArgumentException("조종사 정보를 찾을 수 없습니다."));

        DispatchBatches batch = DispatchBatches.builder()
                .team(admin.getTeam())
                .leader(admin)
                .batchTitle(req.getBatchTitle())
                .assignedPilotCount(1)
                .build();
        batch = dispatchBatchRepository.save(batch);

        List<Lands> selectedLands = landRepository.findAllById(req.getLandNos());
        
        for (Lands land : selectedLands) {
            if (!"READY".equals(land.getStatus())) {
                throw new IllegalStateException("이미 배정되었거나 작업 중인 농지가 포함되어 있습니다: " + land.getAddress());
            }
            land.updateStatus("ASSIGNED");

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

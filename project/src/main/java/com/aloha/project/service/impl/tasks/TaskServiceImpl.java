package com.aloha.project.service.impl.tasks;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.project.domain.lands.Lands;
import com.aloha.project.domain.tasks.DispatchBatches;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.TaskAssignRequest;
import com.aloha.project.repository.lands.LandRepository;
import com.aloha.project.repository.tasks.DispatchBatchRepository;
import com.aloha.project.repository.tasks.WorkAssignmentRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.tasks.TaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@Transactional 
@RequiredArgsConstructor 
public class TaskServiceImpl implements TaskService {

    private final DispatchBatchRepository dispatchBatchRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final LandRepository landRepository;
    private final UserRepository userRepository;

    @Override
    public boolean assignTask(Users leader, TaskAssignRequest request) {
        log.info("## 작업 배정 시작 ## : {}", request);

        try {
            Teams myTeam = leader.getTeam();
            if (myTeam == null) {
                throw new IllegalStateException("소속 방제단이 없습니다.");
            }

            // 1. 대상 농지 및 조종사 조회
            Lands land = landRepository.findById(request.getLandNo())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 농지입니다: " + request.getLandNo()));

            Users pilot = userRepository.findById(request.getPilotNo())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조종사입니다: " + request.getPilotNo()));

            // 2. 상위 배정 묶음(DispatchBatches) 생성
            String title = (request.getBatchTitle() != null && !request.getBatchTitle().trim().isEmpty())
                    ? request.getBatchTitle().trim()
                    : land.getAddress() + " 방제 배정";

            DispatchBatches batch = DispatchBatches.builder()
                    .team(myTeam)
                    .leader(leader)
                    .batchTitle(title)
                    .totalArea(land.getAreaSize()) // 대상 농지 면적
                    .assignedPilotCount(1)         // 투입 조종사 수
                    .build();

            dispatchBatchRepository.save(batch);

            // 3. 개별 작업 배정(WorkAssignments) 생성
            WorkAssignments assignment = WorkAssignments.builder()
                    .dispatchBatch(batch) // 👈 필수 참조 완벽 해결!
                    .land(land)
                    .pilot(pilot)
                    .status("ASSIGNED")   // 초기 상태: 배정됨
                    .build();

            workAssignmentRepository.save(assignment);

            // 4. 농지 상태 업데이트 (READY -> ASSIGNED)
            land.updateStatus("ASSIGNED"); // 👈 JPA 더티체킹으로 자동 반영

            log.info("작업 배정 성공: 농지({}) -> 조종사({})", land.getAddress(), pilot.getName());
            return true;

        } catch (Exception e) {
            log.error("작업 배정 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
}

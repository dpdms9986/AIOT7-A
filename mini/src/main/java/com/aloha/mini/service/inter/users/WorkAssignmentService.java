package com.aloha.mini.service.inter.users;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.mini.domain.Lands;
import com.aloha.mini.domain.Users;
import com.aloha.mini.domain.WorkAssignments;
import com.aloha.mini.dto.AssignmentFormResponse;
import com.aloha.mini.dto.DashboardResponse;
import com.aloha.mini.repository.LandRepository;
import com.aloha.mini.repository.UserRepository;
import com.aloha.mini.repository.WorkAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkAssignmentService {
    
    private final WorkAssignmentRepository workAssignmentRepository;
    private final UserRepository userRepository;
    private final LandRepository landRepository;

    @Transactional
    public void updateAssignmentStatus(Long assignmentNo, String newStatus) {
        

        WorkAssignments assignment = workAssignmentRepository.findById(assignmentNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 배정 내역을 찾을 수 없습니다: " + assignmentNo));
        
        assignment.setStatus(newStatus); 
    }
    
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(Long teamNo) {
    // 1. 해당 팀의 모든 배정 내역 조회
    List<WorkAssignments> assignments = workAssignmentRepository.findByPilot_Team_No(teamNo);
    
    // 2. 건수 계산
    long total = assignments.size();
    long assigned = assignments.stream().filter(a -> "ASSIGNED".equals(a.getStatus())).count();
    long inProgress = assignments.stream().filter(a -> "IN_PROGRESS".equals(a.getStatus())).count();
    long completed = assignments.stream().filter(a -> "COMPLETED".equals(a.getStatus())).count();
    
    // 3. 진행률(progressRate) 없이 상태 건수만 빌더에 담아 반환
    return DashboardResponse.builder()
            .totalCount(total)
            .assignedCount(assigned)
            .inProgressCount(inProgress)
            .completedCount(completed)
            .build();
}

@Transactional(readOnly = true)
public AssignmentFormResponse getAssignmentFormData(Long teamNo) {
    
    // 1. 단장과 같은 팀에 소속된 조종사(ROLE_PILOT) 목록 조회
    List<Users> pilots = userRepository.findByTeam_NoAndRole(teamNo, "ROLE_PILOT");
    
    // 2. 전체 농지 목록 조회 (만약 농지도 팀별 관할 구역이 있다면 조건이 추가되어야 합니다)
    List<Lands> lands = landRepository.findAll();
    
    // 3. Entity -> DTO 변환
    List<AssignmentFormResponse.PilotInfo> pilotInfos = pilots.stream()
            .map(p -> AssignmentFormResponse.PilotInfo.builder()
                    .userNo(p.getNo())
                    .username(p.getUsername())
                    .build())
            .collect(Collectors.toList());
            
    List<AssignmentFormResponse.LandInfo> landInfos = lands.stream()
            .map(l -> AssignmentFormResponse.LandInfo.builder()
                    .landNo(l.getNo())
                    .address(l.getAddress()) // Lands 엔티티에 getAddress()가 있다고 가정
                    .build())
            .collect(Collectors.toList());
            
    // 4. 합쳐서 반환
    return AssignmentFormResponse.builder()
            .pilots(pilotInfos)
            .lands(landInfos)
            .build();
}

// 작업 중단 시 농지 상태 초기화
@Transactional
public void resetLandStatus(Long landNo) {
    Lands land = landRepository.findByNoWithPessimisticLock(landNo)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "해당 농지를 찾을 수 없습니다: " + landNo
                    )
            );

    String status = land.getStatus();

    if (!"ASSIGNED".equals(status) && !"IN_PROGRESS".equals(status)) {
        throw new IllegalStateException(
                "배정되었거나 작업 중인 농지만 초기화할 수 있습니다."
        );
    }

    // lands 테이블의 농지 상태만 READY로 변경
    // 작업 배정 상태는 변경하지 않음
    land.updateStatus("READY");
}
}

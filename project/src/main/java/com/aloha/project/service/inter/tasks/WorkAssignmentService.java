package com.aloha.project.service.inter.tasks;

public interface WorkAssignmentService {

    // 작업 배정 상태 변경
    void updateAssignmentStatus(Long assignmentNo, String status, Long pilotNo);

    // 드론과 약제를 선택하여 작업 시작
    void startAssignment(
        Long assignmentNo,
        Long pilotNo,
        Long droneNo,
        Long chemicalNo
    );
    
    // 일시정지된 작업 재개
    void resumeAssignment(Long assignmentNo, Long pilotNo);
    
    // 강제중단된 작업 재개
    void resumeInterruptedAssignment(Long assignmentNo, Long pilotNo);

}
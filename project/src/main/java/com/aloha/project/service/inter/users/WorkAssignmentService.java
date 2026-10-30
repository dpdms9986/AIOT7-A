package com.aloha.project.service.inter.users;

public interface WorkAssignmentService {

    // 작업 배정 상태 변경
    
    void updateAssignmentStatus(Long assignmentNo, String status, Long pilotNo);

}
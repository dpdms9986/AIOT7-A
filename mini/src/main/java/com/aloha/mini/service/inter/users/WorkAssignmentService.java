package com.aloha.mini.service.inter.users;

import com.aloha.mini.dto.AssignmentFormResponse;
import com.aloha.mini.dto.DashboardResponse;

public interface WorkAssignmentService {

    // 작업 배정 상태 변경
    //  - assignmentNo: 변경할 작업 배정 번호
    //  - newStatus: 새로 적용할 상태값
    void updateAssignmentStatus(Long assignmentNo, String newStatus);

    // 팀별 대시보드 데이터 조회 (전체/배정/작업중/완료 건수)
    //  - teamNo: 조회할 팀 번호
    DashboardResponse getDashboardData(Long teamNo);

    // 배정 폼에 필요한 데이터 조회 (팀 소속 조종사 목록 + 전체 농지 목록)
    //  - teamNo: 단장이 속한 팀 번호
    AssignmentFormResponse getAssignmentFormData(Long teamNo);

    // 작업 중단 시 농지 상태를 READY로 초기화
    //  - landNo: 초기화할 농지 번호
    void resetLandStatus(Long landNo);
}
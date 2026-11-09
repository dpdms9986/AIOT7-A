package com.aloha.project.service.inter.users;

import java.util.List;

import com.aloha.project.dto.PilotAssignmentResponse;
import com.aloha.project.dto.WorkActionRequest;

public interface PilotService {

    // 1. 내게 할당된 작업 목록 조회 (PLT_004)
    // pilotNo: 조회할 조종사의 사용자 번호
    List<PilotAssignmentResponse> getMyAssignments(Long pilotNo);

    // 2. 작업 시작 요청 및 동시성 제어 (PLT_006)
    // pilotNo: 작업을 시작하는 조종사의 사용자 번호
    // req: 작업 배정 번호, 농지 번호
    void startWork(Long pilotNo, WorkActionRequest req);

    // 3. 작업 완료 보고 (PLT_009)
    // pilotNo: 완료 보고를 하는 조종사의 사용자 번호
    // req: 작업 배정 번호, 농지 번호
    void completeWork(Long pilotNo, WorkActionRequest req);

}

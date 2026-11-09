package com.aloha.mini.service.inter.users;

import com.aloha.mini.dto.PilotAssignmentResponse;
import com.aloha.mini.dto.WorkActionRequest;

import java.util.List;

public interface PilotService {

    // 1. 내게 할당된 작업 목록 조회 (PLT_004)
    //    - pilotNo: 조회할 조종사의 유저 번호
    List<PilotAssignmentResponse> getMyAssignments(Long pilotNo);

    // 2. 작업 시작 요청 및 비관적 락 동시성 제어 (PLT_006)
    //    - pilotNo: 작업을 시작하는 조종사의 유저 번호
    //    - req: 농지 번호, 작업 할당 번호
    void startWork(Long pilotNo, WorkActionRequest req);

    // 3. 작업 완료 보고 (PLT_009)
    //    - pilotNo: 완료 보고하는 조종사의 유저 번호
    //    - req: 농지 번호, 작업 할당 번호
    void completeWork(Long pilotNo, WorkActionRequest req);
}
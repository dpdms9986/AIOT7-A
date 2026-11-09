package com.aloha.mini.service.inter.users;

import com.aloha.mini.domain.Lands;
import com.aloha.mini.dto.AssignRequest;

import java.util.List;

public interface LeaderService {

    // 1. 배정 대기(READY) 상태인 농지 목록 조회
    //    - regionNo: 조회할 지역 번호
    List<Lands> getReadyLands(Long regionNo);

    // 2. 선택한 농지들을 조종사에게 배정
    //    - adminNo: 배정을 요청하는 단장의 유저 번호
    //    - req: 조종사 번호, 배정 묶음 제목, 농지 번호 목록
    void assignLandsToPilot(Long adminNo, AssignRequest req);
}
package com.aloha.mini.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.mini.domain.Lands;
import com.aloha.mini.dto.LandUpdateRequest;
import com.aloha.mini.repository.LandRepository;
import com.aloha.mini.repository.WorkAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final LandRepository landRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

    // 농지 정보 수정
    @Transactional
    public void updateLand(Long landNo, LandUpdateRequest req) {
        Lands land = landRepository.findById(landNo)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 농지를 찾을 수 없습니다: " + landNo
                        )
                );

        land.setAddress(req.getAddress());
        land.setAreaSize(req.getAreaSize());
        }

        // 농지 삭제
        @Transactional
        public void deleteLand(Long landNo) {
                Lands land = landRepository.findById(landNo)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 농지를 찾을 수 없습니다: " + landNo
                                )
                        );

                // 작업 중인 농지는 삭제할 수 없음
                if ("IN_PROGRESS".equals(land.getStatus())) {
                throw new IllegalStateException(
                        "작업 중인 농지는 삭제할 수 없습니다."
                );
                }

                // 작업 배정 이력이 있는 농지는 이력을 보존하기 위해 삭제 차단
                if (workAssignmentRepository.existsByLand_No(landNo)) {
                throw new IllegalStateException(
                        "작업 배정 이력이 있는 농지는 삭제할 수 없습니다."
                );
                }

                landRepository.delete(land);
        }
}


package com.aloha.project.repository.tasks;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.WorkAssignmentImages;

public interface WorkAssignmentImageRepository
        extends JpaRepository<WorkAssignmentImages, Long> {

    // 특정 작업의 전체 사진 조회
    List<WorkAssignmentImages> findByWorkAssignment_NoOrderByNoAsc(
            Long workAssignmentNo);

    // 특정 작업의 사진 유형별 조회
    List<WorkAssignmentImages> findByWorkAssignment_NoAndImageTypeOrderByNoAsc(
            Long workAssignmentNo, String imageType);

    // 특정 작업의 사진 전체 삭제
    void deleteByWorkAssignment_No(Long workAssignmentNo);
}

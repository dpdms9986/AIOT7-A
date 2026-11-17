
package com.aloha.project.service.inter.tasks;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.aloha.project.domain.tasks.WorkAssignmentImages;
import com.aloha.project.domain.tasks.WorkAssignments;

public interface WorkAssignmentImageService {

    // 작업 사진 조회
    List<WorkAssignmentImages> getImages(Long assignmentNo);

    // 최초 사진 등록
    void registerImages(
            WorkAssignments assignment,
            List<MultipartFile> beforeImages,
            List<MultipartFile> afterImages);

    // 기존 사진 전체 수정
    void replaceImages(
            WorkAssignments assignment,
            List<MultipartFile> beforeImages,
            List<MultipartFile> afterImages);
}

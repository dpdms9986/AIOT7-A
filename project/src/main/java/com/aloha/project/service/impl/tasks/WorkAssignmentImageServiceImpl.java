
package com.aloha.project.service.impl.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.aloha.project.domain.tasks.WorkAssignmentImages;
import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.repository.tasks.WorkAssignmentImageRepository;
import com.aloha.project.service.inter.tasks.WorkAssignmentImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WorkAssignmentImageServiceImpl
        implements WorkAssignmentImageService {

    private static final int MAX_IMAGES = 4;

    private final WorkAssignmentImageRepository imageRepository;

    @Value("${upload.path}")
    private String uploadPath;

    // 작업 사진 조회
    @Override
    @Transactional(readOnly = true)
    public List<WorkAssignmentImages> getImages(Long assignmentNo) {

        return imageRepository
                .findByWorkAssignment_NoOrderByNoAsc(assignmentNo);
    }

    // 최초 사진 등록
    @Override
    public void registerImages(
            WorkAssignments assignment,
            List<MultipartFile> beforeImages,
            List<MultipartFile> afterImages) {

        List<MultipartFile> before = normalize(beforeImages);
        List<MultipartFile> after = normalize(afterImages);

        validateImages(before, after);

        if (before.isEmpty() && after.isEmpty()) {
            throw new IllegalArgumentException(
                    "등록할 사진을 선택해 주세요.");
        }

        Long assignmentNo = assignment.getNo();

        if (!imageRepository
                .findByWorkAssignment_NoOrderByNoAsc(assignmentNo)
                .isEmpty()) {

            throw new IllegalStateException(
                    "이미 등록된 사진이 있습니다. 수정 기능을 이용해 주세요.");
        }

        saveImages(assignment, before, after);
    }

    // 기존 사진 전체 수정
    @Override
    public void replaceImages(
            WorkAssignments assignment,
            List<MultipartFile> beforeImages,
            List<MultipartFile> afterImages) {

        List<MultipartFile> before = normalize(beforeImages);
        List<MultipartFile> after = normalize(afterImages);

        validateImages(before, after);

        Long assignmentNo = assignment.getNo();

        List<WorkAssignmentImages> oldImages =
                imageRepository.findByWorkAssignment_NoOrderByNoAsc(
                        assignmentNo);

        // 새로운 사진을 먼저 저장
        List<WorkAssignmentImages> newImages =
                saveImages(assignment, before, after);

        // 기존 DB 정보 삭제
        imageRepository.deleteAll(oldImages);

        // 기존 실제 파일 삭제
        for (WorkAssignmentImages oldImage : oldImages) {
            deletePhysicalFile(
                    assignmentNo,
                    oldImage.getSaveName());
        }

        log.info(
                "작업 사진 수정 완료: 작업 번호={}, 기존 사진={}, 신규 사진={}",
                assignmentNo,
                oldImages.size(),
                newImages.size());
    }

    // 비어 있는 파일 제거
    private List<MultipartFile> normalize(
            List<MultipartFile> files) {

        if (files == null) {
            return List.of();
        }

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    // 사진 개수 검증
    private void validateImages(
            List<MultipartFile> before,
            List<MultipartFile> after) {

        if (before.size() > MAX_IMAGES) {
            throw new IllegalArgumentException(
                    "작업 전 사진은 최대 4장까지 등록할 수 있습니다.");
        }

        if (after.size() > MAX_IMAGES) {
            throw new IllegalArgumentException(
                    "작업 후 사진은 최대 4장까지 등록할 수 있습니다.");
        }
    }

    // 전·후 사진 저장
    private List<WorkAssignmentImages> saveImages(
            WorkAssignments assignment,
            List<MultipartFile> before,
            List<MultipartFile> after) {

        List<WorkAssignmentImages> savedImages = new ArrayList<>();

        try {

            // 작업 전 사진 저장
            for (MultipartFile file : before) {

                WorkAssignmentImages image =
                        saveOneImage(assignment, file, "BEFORE");

                savedImages.add(image);
            }

            // 작업 후 사진 저장
            for (MultipartFile file : after) {

                WorkAssignmentImages image =
                        saveOneImage(assignment, file, "AFTER");

                savedImages.add(image);
            }

            // DB 저장
            return imageRepository.saveAll(savedImages);

        } catch (Exception e) {

            // 저장 중 오류가 발생하면 새로 생성한 파일 정리
            for (WorkAssignmentImages image : savedImages) {
                deletePhysicalFile(
                        assignment.getNo(),
                        image.getSaveName());
            }

            throw new IllegalStateException(
                    "작업 사진 저장에 실패했습니다.", e);
        }
    }

    // 개별 이미지 저장
    private WorkAssignmentImages saveOneImage(
            WorkAssignments assignment,
            MultipartFile file,
            String imageType) throws IOException {

        // 원본 파일명
        String originalName = sanitizeFileName(
                file.getOriginalFilename());

        // 원본 파일의 확장자 추출
        // JFIF 등 원본 확장자 유지
        String extension = originalName != null
                && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".jpg";

        // UUID를 이용한 중복 방지 파일명 생성
        String saveName = UUID.randomUUID().toString() + extension;

        Long assignmentNo = assignment.getNo();

        // 작업별 이미지 저장 폴더 생성
        Path directory = Paths.get(
                uploadPath,
                "work-assignment-images",
                String.valueOf(assignmentNo));

        Files.createDirectories(directory);

        // 실제 파일 저장
        Path savePath = directory.resolve(saveName);

        file.transferTo(savePath.toFile());

        // 이미지 접근 경로
        String filePath =
                "/upload/work-assignment-images/"
                + assignmentNo + "/"
                + saveName;

        // DB 이미지 정보 생성
        return WorkAssignmentImages.builder()
                .workAssignment(assignment)
                .imageType(imageType)
                .originName(originalName)
                .saveName(saveName)
                .filePath(filePath)
                .fileSize(file.getSize())
                .build();
    }

    // 파일명 경로 문자 제거
    private String sanitizeFileName(String originalName) {

        if (originalName == null || originalName.isBlank()) {
            return null;
        }

        String normalized = originalName.replace("\\", "/");

        return normalized.substring(
                normalized.lastIndexOf("/") + 1);
    }

    // 실제 서버 파일 삭제
    private void deletePhysicalFile(
            Long assignmentNo,
            String saveName) {

        try {

            Path directory = Paths.get(
                    uploadPath,
                    "work-assignment-images",
                    String.valueOf(assignmentNo))
                    .toAbsolutePath()
                    .normalize();

            Path file = directory.resolve(saveName)
                    .toAbsolutePath()
                    .normalize();

            if (!file.startsWith(directory)) {
                log.warn("잘못된 이미지 파일 경로: {}", saveName);
                return;
            }

            Files.deleteIfExists(file);

        } catch (IOException e) {
            log.error(
                    "작업 사진 파일 삭제 실패: 작업 번호={}, 파일={}",
                    assignmentNo,
                    saveName,
                    e);
        }
    }
}

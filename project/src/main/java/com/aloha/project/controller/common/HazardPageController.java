package com.aloha.project.controller.common;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.domain.common.HazardImages;
import com.aloha.project.domain.common.HazardReports;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.HazardRequest;
import com.aloha.project.repository.common.HazardImageRepository;
import com.aloha.project.repository.common.HazardRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Controller
@RequestMapping("/hazards")
@RequiredArgsConstructor
public class HazardPageController {

    @Value("${upload.path}")
    private String uploadPath;
    private final HazardImageRepository hazardImageRepository;
    private final HazardRepository hazardRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 1. 위험 구역 목록 화면 (단장/조종사 공통)
     */
    @GetMapping
    public String getHazardList(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) return "redirect:/login";

        // 최신순 위험 목록 조회
        List<HazardReports> hazards = hazardRepository.findAllByOrderByCreatedAtDesc();
        // 위험 구역별 이미지 목록 조회
        Map<Long, List<HazardImages>> hazardImagesMap = new HashMap<>();
        Map<Long, String> hazardImagePathsMap = new HashMap<>();
        Map<Long, String> hazardImageDataMap = new HashMap<>();

        
        for (HazardReports hazard : hazards) {
            List<HazardImages> images =
                    hazardImageRepository.findByHazard(hazard);

            hazardImagesMap.put(hazard.getNo(), images);

            // 상세보기에서 사용할 이미지 경로 목록
            List<String> imagePaths = images.stream()
                    .map(HazardImages::getFilePath)
                    .toList();

            // 수정 팝업에서 사용할 이미지 번호 + 경로 목록
            List<Map<String, Object>> imageData = images.stream()
                    .map(image -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("no", image.getNo());
                        data.put("filePath", image.getFilePath());
                        return data;
                    })
                    .toList();

            try {
                hazardImagePathsMap.put(
                        hazard.getNo(),
                        objectMapper.writeValueAsString(imagePaths)
                );

                hazardImageDataMap.put(
                        hazard.getNo(),
                        objectMapper.writeValueAsString(imageData)
                );
            } catch (JacksonException e) {
                log.error("위험 구역 이미지 경로 변환 실패: 번호={}",
                        hazard.getNo(), e);

                hazardImagePathsMap.put(hazard.getNo(), "[]");
                hazardImageDataMap.put(hazard.getNo(), "[]");
            }
        }

        

        model.addAttribute("hazardImagesMap", hazardImagesMap);
        model.addAttribute("hazardImagePathsMap", hazardImagePathsMap);
        model.addAttribute("hazardImageDataMap", hazardImageDataMap);

        // 상단 요약 통계 집계
        long totalCount = hazards.size();
        long activeCount = hazards.stream().filter(h -> "ACTIVE".equals(h.getStatus())).count(); // 주의 요망
        long resolvedCount = hazards.stream().filter(h -> "RESOLVED".equals(h.getStatus())).count(); // 해소됨
        long highRiskCount = hazards.stream().filter(h -> "고위험".equals(h.getHazardType()) && "ACTIVE".equals(h.getStatus())).count(); // 고위험

        model.addAttribute("hazards", hazards);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("highRiskCount", highRiskCount);

        // 수정/삭제 버튼 노출 여부 판단을 위해 현재 로그인 사용자 번호 전달
        model.addAttribute("currentLoginUserNo", customUser.getUser().getNo());
        model.addAttribute("currentLoginUserRole", customUser.getUser().getRole());

        return "page/common/hazards"; // templates/page/hazards.html
    }

    /**
     * * 2. 위험 구역 신규 등록 (사진 업로드 포함)
     * - 프론트엔드 form 에 enctype="multipart/form-data" 필수!
     */
    @PostMapping
    public String createHazard(@ModelAttribute HazardRequest request,
                               @RequestParam(value = "images", required = false) List<MultipartFile> images,
                               @AuthenticationPrincipal CustomUser customUser,
                               RedirectAttributes redirectAttributes) {
        if (customUser == null) return "redirect:/login";

        try {
            Users author = userRepository.findById(customUser.getUser().getNo()).orElse(null);

            HazardReports hazard = HazardReports.builder()
                    .user(author)
                    .title(request.getTitle().trim())
                    .content(request.getContent().trim())
                    .address(request.getAddress() != null ? request.getAddress().trim() : "-")
                    .hazardType(request.getHazardType() != null ? request.getHazardType() : "중위험")
                    .latitude(request.getLatitude() != null ? request.getLatitude() : 36.3504) // 기본값(대전)
                    .longitude(request.getLongitude() != null ? request.getLongitude() : 127.3845)
                    .status("ACTIVE")
                    .build();

            hazardRepository.save(hazard);

            // 2. 첨부 사진 파일이 있으면 로컬 폴더 및 DB 저장
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                saveHazardImage(hazard, image);
            }
        }
    }

            log.info("위험 요소 등록 성공: {} (첨부 사진: {}장)", hazard.getTitle(), (images == null ? 0 : images.size()));
            redirectAttributes.addFlashAttribute("successMessage", "위험 구역이 성공적으로 등록되었습니다.");

        } catch (Exception e) {
            log.error("위험 등록 실패: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "위험 구역 등록 중 오류가 발생했습니다.");
        }

        return "redirect:/hazards";
    }

    
    /**
     * 3. 위험 구역 내용 수정 (사진 교체 지원)
     * - 작성자 본인 또는 관리자(ADMIN)만 가능
     */
    @PostMapping("/{no}/update")
    public String updateHazard(@PathVariable("no") Long no,
                               @ModelAttribute HazardRequest request,
                               @RequestParam(value = "images", required = false) List<MultipartFile> images,
                               @RequestParam(value = "deleteImageNos", required = false) List<Long> deleteImageNos,
                               @AuthenticationPrincipal CustomUser customUser,
                               RedirectAttributes redirectAttributes) {

        if (customUser == null) return "redirect:/login";

        HazardReports hazard = hazardRepository.findById(no).orElse(null);
        if (hazard == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 게시글입니다.");
            return "redirect:/hazards";
        }

        Users loginUser = customUser.getUser();
        boolean isAuthor = hazard.getUser() != null && hazard.getUser().getNo().equals(loginUser.getNo());
        boolean isAdmin = "ADMIN".equals(loginUser.getRole());

        // 권한 체크: 본인도 아니고 관리자도 아니면 차단
        if (!isAuthor && !isAdmin) {
            redirectAttributes.addFlashAttribute("errorMessage", "수정 권한이 없습니다.");
            return "redirect:/hazards";
        }

        // 필수 입력값 확인
        if (request.getTitle() == null || request.getTitle().isBlank()
                || request.getContent() == null || request.getContent().isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "제목과 상세 내용을 입력해주세요.");
            return "redirect:/hazards";
        }

        // 내용 업데이트 (JPA 더티 체킹으로 자동 저장)
        hazard.update(
            request.getTitle().trim(),
            request.getContent().trim(),
            request.getHazardType() != null ? request.getHazardType() : hazard.getHazardType(),
            request.getAddress() != null ? request.getAddress().trim() : hazard.getAddress(),
            request.getLatitude(),
            request.getLongitude()
        );
        hazardRepository.save(hazard);

        
        // 사용자가 삭제 선택한 기존 이미지 처리
        if (deleteImageNos != null && !deleteImageNos.isEmpty()) {

            // 현재 위험 구역에 속한 이미지인지 확인
            List<HazardImages> deleteTargets =
                    hazardImageRepository.findByNoInAndHazard(deleteImageNos, hazard);

            File imageDir = new File(uploadPath, "hazards");

            for (HazardImages existingImage : deleteTargets) {

                // 실제 서버 이미지 파일 삭제
                File existingFile = new File(
                        imageDir,
                        existingImage.getSaveName()
                );

                if (existingFile.exists() && !existingFile.delete()) {
                    log.warn("이미지 파일 삭제 실패: {}",
                            existingFile.getAbsolutePath());
                }
            }

            // 선택된 이미지의 DB 정보만 삭제
            hazardImageRepository.deleteAll(deleteTargets);
        }

        // 새 사진은 기존 사진을 유지하면서 추가 저장
        if (images != null) {
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    saveHazardImage(hazard, image);
                }
            }
        }



        log.info("위험 구역 수정 완료: 번호={}", no);
        redirectAttributes.addFlashAttribute("successMessage", "위험 구역 정보가 수정되었습니다.");
        return "redirect:/hazards";
    }


    /**
     * 4. 위험 구역 삭제 (POST /hazards/{no}/delete)
     * - 작성자 본인 또는 관리자(ADMIN)만 가능
     */
    @PostMapping("/{no}/delete")
    public String deleteHazard(@PathVariable("no") Long no,
                               @AuthenticationPrincipal CustomUser customUser,
                               RedirectAttributes redirectAttributes) {
        if (customUser == null) return "redirect:/login";

        HazardReports hazard = hazardRepository.findById(no).orElse(null);
        if (hazard == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 게시글입니다.");
            return "redirect:/hazards";
        }

        Users loginUser = customUser.getUser();
        boolean isAuthor = hazard.getUser() != null && hazard.getUser().getNo().equals(loginUser.getNo());
        boolean isAdmin = "ADMIN".equals(loginUser.getRole());

        // 권한 체크
        if (!isAuthor && !isAdmin) {
            redirectAttributes.addFlashAttribute("errorMessage", "삭제 권한이 없습니다.");
            return "redirect:/hazards";
        }

        // 첨부 사진 먼저 삭제 후 게시글 삭제
        hazardImageRepository.deleteByHazard(hazard);
        hazardRepository.delete(hazard);
        
        log.info("위험 구역 삭제 완료: 번호={}", no);
        redirectAttributes.addFlashAttribute("successMessage", "위험 구역이 삭제되었습니다.");
        return "redirect:/hazards";
    }

    /**
     * 5. 위험 해소 완료 처리 (버튼 클릭 시)
     */
    @PostMapping("/{no}/resolve")
    public String resolveHazard(@PathVariable("no") Long no, RedirectAttributes redirectAttributes) {
        HazardReports hazard = hazardRepository.findById(no).orElse(null);
        if (hazard != null) {
            hazard.setStatus("RESOLVED");
            hazardRepository.save(hazard);
            redirectAttributes.addFlashAttribute("successMessage", "위험 요소가 해소(완료) 처리되었습니다.");
        }
        return "redirect:/hazards";
    }

    /* ===== 사진 파일 저장 헬퍼 메서드 ===== */
    private void saveHazardImage(HazardReports hazard, MultipartFile file) {
        try {
            String originName = file.getOriginalFilename();
            String ext = originName != null && originName.contains(".") 
                         ? originName.substring(originName.lastIndexOf(".")) : ".jpg";
            String saveName = UUID.randomUUID().toString() + ext;

            // 1. properties에 지정된 uploadPath 아래에 hazards 폴더 생성
            File dir = new File(uploadPath, "hazards");
            if (!dir.exists()) dir.mkdirs();

            // 실제 파일 저장
            File saveFile = new File(dir, saveName);
            file.transferTo(saveFile);

            // DB에 이미지 정보 저장 (/uploads/hazards/uuid.jpg)
            HazardImages hazardImage = HazardImages.builder()
                    .hazard(hazard)
                    .originName(originName)
                    .saveName(saveName)
                    .filePath("/upload/hazards/" + saveName)
                    .fileSize(file.getSize())
                    .build();

            hazardImageRepository.save(hazardImage);

        } catch (IOException e) {
            log.error("사진 파일 저장 실패: {}", e.getMessage(), e);
        }
    }

}
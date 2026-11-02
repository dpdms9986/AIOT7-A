package com.aloha.project.controller.common;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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

import com.aloha.project.domain.common.HazardComments;
import com.aloha.project.domain.common.HazardImages;
import com.aloha.project.domain.common.HazardReports;
import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.HazardRequest;
import com.aloha.project.repository.common.HazardCommentRepository;
import com.aloha.project.repository.common.HazardImageRepository;
import com.aloha.project.repository.common.HazardRepository;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/hazards")
@RequiredArgsConstructor
public class HazardPageController {

    @Value("${upload.path}")
    private String uploadPath;
    private final HazardImageRepository hazardImageRepository;
    private final HazardCommentRepository hazardCommentRepository;
    private final HazardRepository hazardRepository;
    private final UserRepository userRepository;

    /**
     * 1. 위험 구역 목록 화면 (단장/조종사 공통)
     */
    @GetMapping
    public String getHazardList(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) return "redirect:/login";

        // 최신순 위험 목록 조회
        List<HazardReports> hazards = hazardRepository.findAllByOrderByCreatedAtDesc();

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

        return "page/hazards"; // templates/page/hazards.html
    }

    /**
     * * 2. 위험 구역 신규 등록 (사진 업로드 포함)
     * - 프론트엔드 form 에 enctype="multipart/form-data" 필수!
     */
    @PostMapping
    public String createHazard(@ModelAttribute HazardRequest request,
                               @RequestParam(value = "image", required = false) MultipartFile image,
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
            if (image != null && !image.isEmpty()) {
                saveHazardImage(hazard, image);
            }

            log.info("위험 요소 등록 성공: {} (사진 첨부: {})", hazard.getTitle(), (image != null && !image.isEmpty()));
            redirectAttributes.addFlashAttribute("successMessage", "위험 구역이 성공적으로 등록되었습니다.");

        } catch (Exception e) {
            log.error("위험 등록 실패: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "위험 구역 등록 중 오류가 발생했습니다.");
        }

        return "redirect:/hazards";
    }

    /**
     * 3. 위험 구역 내용 수정 (사진 교체 지원))
     * - 작성자 본인 또는 관리자(ADMIN)만 가능
     */
    @PostMapping("/{no}/update")
    public String updateHazard(@PathVariable("no") Long no,
                               @ModelAttribute HazardRequest request,
                               @RequestParam(value = "image", required = false) MultipartFile image,
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

        // 내용 업데이트 (JPA 더티 체킹으로 자동 저장)
        hazard.update(
            request.getTitle().trim(),
            request.getContent().trim(),
            request.getHazardType(),
            request.getAddress() != null ? request.getAddress().trim() : hazard.getAddress(),
            request.getLatitude(),
            request.getLongitude()
        );
        hazardRepository.save(hazard);

        // 새 사진이 올라왔다면 기존 사진 교체
        if (image != null && !image.isEmpty()) {
            hazardImageRepository.deleteByHazard(hazard); // 기존 사진 삭제
            saveHazardImage(hazard, image); // 새 사진 저장
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

    /**
     * 6. 댓글 작성 (POST /hazards/{no}/comments)
     */
    @PostMapping("/{no}/comments")
    public String addComment(@PathVariable("no") Long hazardNo,
                             @RequestParam("content") String content,
                             @AuthenticationPrincipal CustomUser customUser,
                             RedirectAttributes redirectAttributes) {

        if (customUser == null) return "redirect:/login";

        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 내용을 입력해 주세요.");
            return "redirect:/hazards";
        }

        HazardReports hazard = hazardRepository.findById(hazardNo).orElse(null);
        Users author = userRepository.findById(customUser.getUser().getNo()).orElse(null);

        if (hazard != null && author != null) {
            HazardComments comment = HazardComments.builder()
                    .hazard(hazard)
                    .user(author)
                    .content(content.trim())
                    .build();

            hazardCommentRepository.save(comment);
            log.info("댓글 등록 완료: 위험글({}) -> 작성자({})", hazardNo, author.getName());
            redirectAttributes.addFlashAttribute("successMessage", "댓글이 등록되었습니다.");
        }

        return "redirect:/hazards";
    }

    /**
     * 7. 댓글 삭제 (POST /hazards/comments/{commentNo}/delete)
     * - 본인 또는 관리자(ADMIN)만 삭제 가능
     */
    @PostMapping("/comments/{commentNo}/delete")
    public String deleteComment(@PathVariable("commentNo") Long commentNo,
                                @AuthenticationPrincipal CustomUser customUser,
                                RedirectAttributes redirectAttributes) {

        if (customUser == null) return "redirect:/login";

        HazardComments comment = hazardCommentRepository.findById(commentNo).orElse(null);
        if (comment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 댓글입니다.");
            return "redirect:/hazards";
        }

        Users loginUser = customUser.getUser();
        boolean isAuthor = comment.getUser() != null && comment.getUser().getNo().equals(loginUser.getNo());
        boolean isAdmin = "ADMIN".equals(loginUser.getRole());

        if (!isAuthor && !isAdmin) {
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 삭제 권한이 없습니다.");
            return "redirect:/hazards";
        }

        hazardCommentRepository.delete(comment);
        log.info("댓글 삭제 완료: 댓글번호={}", commentNo);
        redirectAttributes.addFlashAttribute("successMessage", "댓글이 삭제되었습니다.");

        return "redirect:/hazards";
    }
}
package com.aloha.project.api.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aloha.project.domain.common.Media;
import com.aloha.project.service.inter.common.MediaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Jodit 에디터 이미지 업로드 API
 * ✅ 업로드 파일은 media 테이블에 parentId(게시글 ID)와 함께 저장되어 게시글 삭제 시 함께 삭제된다.
 * ✅ 응답은 Jodit 기본 업로더 형식(success, data.files, data.isImages)을 따른다.
 */
@Slf4j
@RestController
@RequestMapping("/api/common/editor")
@RequiredArgsConstructor
public class EditorApi {

  private static final String DOMAIN = "editor";
  private static final int MAX_FILES = 10;
  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
  private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

  private final MediaService mediaService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/upload")
  public ResponseEntity<?> upload(
      @RequestParam("files") MultipartFile[] files,
      @RequestParam(value = "parentId", required = false) String parentId) {

    if (files == null || files.length == 0) {
      return fail("업로드할 이미지가 없습니다.");
    }
    if (files.length > MAX_FILES) {
      return fail("한 번에 최대 " + MAX_FILES + "개까지 업로드할 수 있습니다.");
    }

    for (MultipartFile file : files) {
      String error = validate(file);
      if (error != null) {
        return fail(error);
      }
    }

    List<String> urls = new ArrayList<>();
    List<Boolean> isImages = new ArrayList<>();

    for (MultipartFile file : files) {
      Media media = Media.builder()
          .parentId(parentId != null && !parentId.isBlank() ? parentId : "temp")
          .isMain(false)
          .isThumb(false)
          .domain(DOMAIN)
          .build();
      media.setFile(file);

      Media uploaded = mediaService.uploadFile(media);
      if (uploaded == null) {
        log.error("에디터 이미지 업로드 실패: {}", file.getOriginalFilename());
        return ResponseEntity.internalServerError()
            .body(Map.of("success", false, "data", Map.of("messages", List.of("이미지 저장 중 오류가 발생했습니다."))));
      }
      urls.add(uploaded.getContent());
      isImages.add(true);
    }

    return ResponseEntity.ok(Map.of(
        "success", true,
        "data", Map.of(
            "files", urls,
            "isImages", isImages,
            "path", "",
            "baseurl", "",
            "messages", List.of())));
  }

  private String validate(MultipartFile file) {
    if (file.isEmpty()) {
      return "비어있는 파일은 업로드할 수 없습니다.";
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      return "이미지는 파일당 10MB 이하만 업로드할 수 있습니다.";
    }
    String name = file.getOriginalFilename();
    if (name == null || name.contains("/") || name.contains("\\") || name.contains("..")) {
      return "허용되지 않는 파일명입니다.";
    }
    int dot = name.lastIndexOf('.');
    String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    if (!ALLOWED_EXTENSIONS.contains(ext)) {
      return "jpg, jpeg, png, gif, webp 형식의 이미지만 업로드할 수 있습니다.";
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      return "이미지 파일만 업로드할 수 있습니다.";
    }
    return null;
  }

  private ResponseEntity<?> fail(String message) {
    return ResponseEntity.badRequest()
        .body(Map.of("success", false, "data", Map.of("messages", List.of(message))));
  }

}

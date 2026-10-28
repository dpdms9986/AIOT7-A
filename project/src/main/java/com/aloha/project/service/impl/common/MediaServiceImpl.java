package com.aloha.project.service.impl.common;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aloha.project.domain.common.Media;
import com.aloha.project.repository.common.MediaRepository;
import com.aloha.project.service.impl.BaseServiceImpl;
import com.aloha.project.service.inter.common.MediaService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MediaServiceImpl extends BaseServiceImpl<Media, MediaRepository> implements MediaService {

  @Value("${upload.path}")
  private String uploadPath;
  
  @Override
  public Media uploadFile(Media media) {
    if (media == null || media.getFile() == null) {
      log.warn("No file to upload");
      return null;
    }
    
    MultipartFile file = media.getFile();
    log.info("Uploading file: {}", file.getOriginalFilename());

    try {
      // 도메인 추출 (parentId에서 products, reviews 등 판별)
      String domain = extractDomainFromParentId(media.getDomain());
      
      // 업로드 디렉토리 생성
      File uploadDir = new File(uploadPath, domain);
      if (!uploadDir.exists()) {
        uploadDir.mkdirs();
        log.info("Created upload directory: {}", uploadDir.getAbsolutePath());
      }
      
      // 고유한 파일명 생성
      String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
      
      // 파일 저장
      File destFile = new File(uploadDir, uniqueFileName);
      FileCopyUtils.copy(file.getBytes(), destFile);
      log.info("File saved to: {}", destFile.getAbsolutePath());
      
      // 파일 타입 감지
      String fileType = determineFileType(file.getContentType(), file.getOriginalFilename());
      
      // Set media properties
      media.setContent("/upload/" + domain + "/" + uniqueFileName);  // 웹 접근 URL
      media.setName(uniqueFileName);
      media.setOriginName(file.getOriginalFilename());
      media.setPath(destFile.getAbsolutePath());  // 실제 파일 시스템 경로
      media.setType(fileType);

      // Save to database
      this.insert(media);
      log.info("File uploaded successfully: {}", media.getContent());
      
      return media;
    } catch (Exception e) {
      log.error("Error uploading file: {}", file.getOriginalFilename(), e);
      markRollbackOnly();
      return null;
    }
  }

  @Override
  public Media uploadFile(MultipartFile file, String parentId) {
    if (file == null || file.isEmpty()) {
      log.warn("No file to upload");
      return null;
    }
    Media media = new Media();
    media.setFile(file);
    media.setParentId(parentId);
    return uploadFile(media);
  }

  
  
  private String generateUniqueFileName(String originalFilename) {
    String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    return uid + "_" + originalFilename;
  }

  @Override
  public Media uploadFile(MultipartFile file, String parentId, Boolean isMain, Boolean isThumb, Integer thumbSeq) {
    if (file == null || file.isEmpty()) {
      log.warn("No file to upload");
      return null;
    }
    Media media = new Media();
    media.setFile(file);
    media.setParentId(parentId);
    media.setIsMain(isMain);
    media.setIsThumb(isThumb);
    media.setThumbSeq(thumbSeq);
    return uploadFile(media);
  }

  @Override
  public Media selectByParentId(String parentId) {
    if (parentId == null || parentId.trim().isEmpty()) {
      log.warn("Parent ID is null or empty");
      return null;
    }
    try {
      return repository.findFirstByParentIdOrderByNoAsc(parentId).orElse(null);
    } catch (Exception e) {
      log.error("Error selecting media by parent ID: {}", parentId, e);
      return null;
    }
  }
  
  @Override
  public List<Media> selectAllByParentId(String parentId) {
    if (parentId == null || parentId.trim().isEmpty()) {
      log.warn("Parent ID is null or empty");
      return null;
    }
    try {
      return repository.findByParentId(parentId);
    } catch (Exception e) {
      log.error("Error selecting all media by parent ID: {}", parentId, e);
      return null;
    }
  }

  @Override
  public boolean deleteMediaById(String id) {
    log.info("===========================================================");
    log.info("id = {}", id);
    if (id == null || id.trim().isEmpty()) {
      log.warn("Media ID is null or empty");
      return false;
    }
    try {
      Media media = repository.findOneById(id).orElse(null);
      if (media == null) {
        log.warn("No media found with ID: {}", id);
        return false;
      }
      
      // 로컬 파일 삭제
      if (media.getPath() != null) {
        File file = new File(media.getPath());
        if (file.exists()) {
          if (file.delete()) {
            log.info("File deleted from filesystem: {}", media.getPath());
          } else {
            log.warn("Failed to delete file from filesystem: {}", media.getPath());
          }
        }
      }
      
      // Delete record from database
      repository.delete(media);
      repository.flush();
      log.info("Media record deleted from database: ID={}", id);
      return true;
    } catch (Exception e) {
      log.error("Error deleting media by ID: {}", id, e);
      markRollbackOnly();
      return false;
    }

  }

  /**
   * domain 추출 (parentId에서 products, reviews 등 판별)
   */
  private String extractDomainFromParentId(String domain) {
    if (domain == null) {
      return "others";
    }
    return domain;
  }

  /**
   * 파일 타입 감지
   */
  private String determineFileType(String contentType, String filename) {
    // Content Type이 있는 경우
    if (contentType != null) {
      if (contentType.startsWith("image/")) {
        return "이미지";
      } else if (contentType.startsWith("video/")) {
        return "동영상";
      }
    }
    
    // 파일 확장자로 판별
    if (filename != null) {
      String lowerFilename = filename.toLowerCase();
      if (lowerFilename.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp|svg)$")) {
        return "이미지";
      } else if (lowerFilename.matches(".*\\.(mp4|avi|mov|wmv|flv|webm|mkv)$")) {
        return "동영상";
      }
    }
    
    // 기본값
    return "이미지";
  }

  @Override
  public boolean deleteMediaByUrl(String url) {
    log.info("Deleting media by URL: {}", url);
    if (url == null || url.trim().isEmpty()) {
      log.warn("URL is null or empty");
      return false;
    }
    try {
      return deleteAll(repository.findByContent(url), "URL: " + url);
    } catch (Exception e) {
      log.error("Error deleting media by URL: {}", url, e);
      return false;
    }
  }

  @Override
  public boolean deleteByParentId(String id) {
    log.info("Deleting media by Parent ID: {}", id);
    if (id == null || id.trim().isEmpty()) {
      log.warn("Parent ID is null or empty");
      return false;
    }
    try {
      return deleteAll(repository.findByParentId(id), "Parent ID: " + id);
    } catch (Exception e) {
      log.error("Error deleting media by Parent ID: {}", id, e);
      return false;
    }
  }

  private boolean deleteAll(List<Media> mediaList, String condition) {
    if (mediaList == null || mediaList.isEmpty()) {
      log.warn("No media found with {}", condition);
      return false;
    }

    boolean allDeleted = true;
    for (Media media : mediaList) {
      boolean deleted = deleteMediaById(media.getId());
      if (!deleted) {
        allDeleted = false;
        log.warn("Failed to delete media with ID: {}", media.getId());
      }
    }
    return allDeleted;
  }

}

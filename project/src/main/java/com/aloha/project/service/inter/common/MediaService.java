package com.aloha.project.service.inter.common;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.aloha.project.domain.common.Media;
import com.aloha.project.service.inter.BaseService;

public interface MediaService extends BaseService<Media>  {

  // 파일 업로드
  public Media uploadFile(Media media);
  public Media uploadFile(MultipartFile file, String parentId);
  public Media uploadFile(MultipartFile file, String parentId, Boolean isMain, Boolean isThumb, Integer thumbSeq);
  public Media selectByParentId(String id);
  public List<Media> selectAllByParentId(String parentId);

  // 파일 삭제
  public boolean deleteMediaById(String id);
  public boolean deleteMediaByUrl(String url);
  public boolean deleteByParentId(String id);

}

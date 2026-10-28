package com.aloha.project.domain.common;

import org.hibernate.annotations.DynamicInsert;
import org.springframework.web.multipart.MultipartFile;

import com.aloha.project.domain.Base;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@Table(name = "media")
public class Media extends Base {
  private String parentId;       // FK (부모 UK)
  private Boolean isMain;      // 메인미디어
  private Boolean isThumb;     // 썸네일
  private Integer thumbSeq;    // 썸네일순서
  private String type;         // 타입 ('이미지','동영상','임베드')
  private String content;      // 컨텐츠( URL, 임베드 코드 등 )
  private String path;         // 파일경로
  private String name;         // 파일명
  private String originName;   // 원본파일명


  @Transient
  private String domain;      // 도메인 (products, reviews 등)

  @JsonIgnore
  @Transient
  private MultipartFile file;  // 업로드 파일
}

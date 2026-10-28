package com.aloha.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${upload.path}")
  private String uploadPath;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    log.info("Configuring static resource handler for upload path: {}", uploadPath);
    
    // /upload/** 경로로 접근하면 실제 파일 시스템의 upload.path 디렉토리에서 파일을 제공
    registry.addResourceHandler("/upload/**")
        .addResourceLocations("file:" + uploadPath + "/");
  }
}

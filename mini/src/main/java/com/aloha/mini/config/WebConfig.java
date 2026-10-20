package com.aloha.mini.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // /api/ 로 시작하는 모든 요청에 대해 적용
                .allowedOrigins(
                        "http://localhost:3000", // CRA(Create React App) 기본 포트
                        "http://localhost:5173"  // Vite 기본 포트
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // HttpSession 쿠키(JSESSIONID) 교류를 위해 필수
                .maxAge(3600);
    }
}
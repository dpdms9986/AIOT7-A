package com.aloha.mini.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin") // ROLE_ADMIN만 접근 가능
@RequiredArgsConstructor
public class AdminController {
    
    // 요구사항 정의서 확정 후 이곳에 관리자 전용 API(농지 관리, 방제단 관리 등)를 추가합니다.

}

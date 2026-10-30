package com.aloha.project.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/pilot")
public class PilotController {

    // 1. 조종사 대시보드
    @GetMapping("/dashboard")
    public String getDashboard() {
        return "page/pilot/dashboard";
    }

    // 2. 작업 이력
    @GetMapping("/history")
    public String getHistory() {
        return "page/pilot/history";
    }

    // 3. 드론 자산
    @GetMapping("/assetlist")
    public String getAssetList() {
        return "page/pilot/assetlist";
    }

}

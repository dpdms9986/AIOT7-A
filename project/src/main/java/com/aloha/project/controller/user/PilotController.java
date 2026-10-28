package com.aloha.project.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;


@Slf4j 
@Controller 
@RequestMapping("/pilot")
public class PilotController {

    /**
     * 파일럿 대시보드
     * @return
     */
    @GetMapping("/dashboard")
    public String getDashboard() {
        return "page/pilot/dashboard";
    }

    /**
     * 작업 이력
     * @return
     */
    @GetMapping("/history")
    public String getHistory() {
        return "page/pilot/history";
    }

    /**
     * 드론자산
     * @return
     */
    @GetMapping("/assetlist")
    public String getAssetList() {
        return "page/pilot/assetlist";
    }
    
}

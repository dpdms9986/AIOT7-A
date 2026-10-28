package com.aloha.project.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Controller 
@RequestMapping("/leader")
public class LeaderController {

    /**
     * 관제 대시보드
     * @return
     */
    @GetMapping("/dashboard")
    public String getDashboard() {
        return "page/leader/dashboard";
    }

    /**
     * 작업 배정
     * @return
     */
    @GetMapping("/task-assignment")
    public String getTaskAssignment() {
        return "page/leader/task-assignment";
    }

    /**
     * 농지 관리
     * @return
     */
    @GetMapping("/farmlist")
    public String getFarmList() {
        return "page/leader/farmlist";
    }

    /**
     * 방제단 관리
     * @return
     */
    @GetMapping("/team-management")
    public String getTeamManagement() {
        return "page/leader/team-management";
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
        return "page/leader/assetlist";
    }
    
}

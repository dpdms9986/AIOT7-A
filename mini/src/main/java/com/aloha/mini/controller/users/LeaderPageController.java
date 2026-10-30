package com.aloha.mini.controller.users;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
/** 방제단장 화면 URL을 담당하는 컨트롤러입니다. */
public class LeaderPageController {

    @GetMapping({"/leader", "/leader/dashboard"})
    public String dashboard() {
        return "leader-dashboard";
    }

    @GetMapping("/leader/assets")
    public String assets() {
        return "assetlist-leader";
    }

    @GetMapping("/leader/tasks")
    public String taskAssignment() {
        return "task-assignment";
    }

    @GetMapping("/leader/team-management")
    public String teamManagement() {
        return "team-management";
    }

    @GetMapping("/leader/statistics")
    public String statistics() {
        return "statistics";
    }

    @GetMapping("/leader/hazards")
    public String hazards() {
        return "hazardlist";
    }
}

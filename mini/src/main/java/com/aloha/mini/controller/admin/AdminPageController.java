package com.aloha.mini.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 시스템 관리자 화면 URL을 담당하는 컨트롤러입니다. */
@Controller
public class AdminPageController {

    @GetMapping({"/admin", "/admin/dashboard"})
    public String dashboard() {
        return "control-dashboard";
    }

    @GetMapping("/admin/lands")
    public String lands() {
        return "farmlist";
    }

    @GetMapping("/admin/statistics")
    public String statistics() {
        return "statistics";
    }

    @GetMapping("/admin/team-management")
    public String teamManagement() {
        return "team-management";
    }
}

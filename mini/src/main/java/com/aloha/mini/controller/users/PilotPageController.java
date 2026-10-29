package com.aloha.mini.controller.users;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 드론 조종사 화면 URL을 담당하는 컨트롤러입니다. */
@Controller
public class PilotPageController {

    @GetMapping({"/pilot", "/pilot/dashboard"})
    public String dashboard() {
        return "pilot-dashboard";
    }

    @GetMapping("/pilot/assets")
    public String assets() {
        return "assetlist-pilot";
    }

    @GetMapping("/pilot/history")
    public String history() {
        return "pilot-history";
    }

    @GetMapping("/pilot/mypage")
    public String myPage() {
        return "mypage";
    }
}

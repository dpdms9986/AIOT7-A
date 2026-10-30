package com.aloha.project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.aloha.project.dto.SignupRequest;
import com.aloha.project.repository.users.TeamRepository;
import com.aloha.project.service.inter.users.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Controller
@RequiredArgsConstructor 
public class HomeController {

  private final UserService userService; // 실제 회원가입 DB 처리를 하는 서비스 주입
    private final TeamRepository teamRepository;
  /**
   * 💻 회원가입
   * @return
   */
  @GetMapping({"/join", "/signup"})
  public String getMethodName() {
      return "page/signup";
  }

  // 임시: 등록된 방제단과 초대코드 목록 전체 반환 (Thunder Client 테스트용)
    @GetMapping("/api/test/invite-codes")
    @ResponseBody
    public ResponseEntity<?> getInviteCodes() {
        // DB에 저장된 모든 팀의 이름과 초대코드 조회
        return ResponseEntity.ok(teamRepository.findAll());
    }


   // [깔끔하게 정리된 버전]
    @PostMapping("/signup")
    public String signupSubmit(@ModelAttribute SignupRequest signupDto, 
                               RedirectAttributes redirectAttributes) {
        log.info("회원가입 요청 데이터: {}", signupDto);

        try {
            // 서비스가 방제단 생성, 암호화, 권한 부여, DB 저장까지 한 번에 처리!
            boolean isSuccess = userService.join(signupDto);

            if (!isSuccess) {
                log.warn("회원가입 실패 (userService.join 반환값 false)");
                redirectAttributes.addFlashAttribute("errorMessage", "회원가입에 실패했습니다.");
                return "redirect:/signup";
            }

            log.info("회원가입 성공: {}", signupDto.getUsername());
            redirectAttributes.addFlashAttribute("signupSuccess", true);
            return "redirect:/login";

        } catch (Exception e) {
            log.error("회원가입 중 예외 발생: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "오류가 발생했습니다: " + e.getMessage());
            return "redirect:/signup";
        }
    }

    /**
   * 💻 로그인
   * @return
   */
  @GetMapping({"/login", "/signin"})
  public String login(
    @CookieValue(value = "remember-id", required = false) Cookie cookie,
    Model model
  ) {
    log.info(":::::::::: 로그인 페이지 ::::::::::");
    String username = "";
    boolean rememberId = false;
    if( cookie != null ) {
        log.info("CookieName : " + cookie.getName());
        log.info("CookieValue : " + cookie.getValue());
        username = cookie.getValue();
        rememberId = true;
    }
    model.addAttribute("username", username);
    model.addAttribute("rememberId", rememberId);
    return "page/login";
  }

  /**
   * 💻 아이디 찾기
   * @return
   */
  @GetMapping("/find-id")
  public String findId() {
      log.info(":::::::::: 아이디 찾기 페이지 ::::::::::");
      return "page/find-id";
  }

  /**
   * 💻 비밀번호 찾기
   * @return
   */
  @GetMapping("/find-pw")
  public String findPassword() {
      log.info(":::::::::: 비밀번호 찾기 페이지 ::::::::::");
      return "page/find-pw";
  }

  /**
   * 이용약관
   */
  @GetMapping("/terms/{page}")
  public String termsPage(@PathVariable("page") String page) {
      return "page/terms/" + page;
  }
  

  /**
   * 고객안내
   * @return
   */
  @GetMapping("/info")
  public String info() {
      return "page/info";
  }
  
  
  /**
  * 로그아웃
  * @param param
  * @return
  */
  @GetMapping("/logout")
  public String logout(
    HttpSession session,
    HttpServletResponse response,
    @RequestParam("redirect") String redirect,
    @CookieValue(value = "remember-me", required = false) String rememberMe,
    @CookieValue(value = "remember-id", required = false) String rememberId
  ) {
    log.info("로그아웃 요청");
    // 세션 무효화
    if (session != null) {
        session.invalidate();
        Cookie cookie = new Cookie("remember-me", null);
        cookie.setMaxAge(0); // 쿠키 삭제
        Cookie cookie2 = new Cookie("remember-id", null);
        cookie2.setMaxAge(0); // 쿠키 삭제
        response.addCookie(cookie);
        response.addCookie(cookie2);
        log.info("세션 무효화 완료");
    }
    // redirect 파라미터 있으면, 해당 페이지로 이동
    if( redirect != null && !redirect.isEmpty() ) {
      return "redirect:" + redirect;
    }
    return "redirect:/";
  }

}



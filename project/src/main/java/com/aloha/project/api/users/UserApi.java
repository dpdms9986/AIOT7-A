package com.aloha.project.api.users;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.domain.users.Users;
import com.aloha.project.service.inter.users.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor 
public class UserApi {
  
  private final UserService userService;
  
  @GetMapping("/user")
  public ResponseEntity<?> getAll(
    @RequestParam(value = "page", required = false, defaultValue = "1") int page,
    @RequestParam(value = "size", required = false, defaultValue = "10") int size
  ) {
      try {
          return new ResponseEntity<>(userService.page(page, size), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @GetMapping("/user/{id}")
  public ResponseEntity<?> getOne(@PathVariable("id") String id) {
      try {
          return new ResponseEntity<>(userService.selectById(id), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PostMapping(path = "/user", consumes = "application/x-www-form-urlencoded")
  public ResponseEntity<?> createForm(Users user) {
      log.info("## FORM ##");
      log.info("user={}", user);
      try {
          return new ResponseEntity<>(userService.join(user), HttpStatus.OK);
      } catch (Exception e) {
          log.error("Error in form processing: ", e);
          return new ResponseEntity<>("Server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  @PostMapping(path = "/user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> createMultiPartForm(Users user) {
      log.info("## MULTIPART ##");
      log.info("user={}", user);
      try {
          // multipart 요청에서 빈 파일 필드들을 제거하거나 처리
          if (user != null) {
              return new ResponseEntity<>(userService.join(user), HttpStatus.OK);
          } else {
              return new ResponseEntity<>("Invalid user data", HttpStatus.BAD_REQUEST);
          }
      } catch (Exception e) {
          log.error("Error in multipart form processing: ", e);
          return new ResponseEntity<>("Server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  @PostMapping(path = "/user", consumes = "application/json")
  public ResponseEntity<?> create(@RequestBody Users user) {
      log.info("## JSON ##");
      log.info("user={}", user);
      try {
          return new ResponseEntity<>(userService.join(user), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PutMapping(path = "/user", consumes = "application/x-www-form-urlencoded")
  public ResponseEntity<?> updateForm(Users user) {
      try {
          return new ResponseEntity<>(userService.updateById(user), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PutMapping(path = "/user", consumes = "multipart/form-data")
  public ResponseEntity<?> updateMultiPartForm(Users user) {
      try {
          return new ResponseEntity<>(userService.updateById(user), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PutMapping(path = "/user", consumes = "application/json")
  public ResponseEntity<?> update(@RequestBody Users user) {
      try {
          return new ResponseEntity<>(userService.updateById(user), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @DeleteMapping("/user/{id}")
  public ResponseEntity<?> destroy(@PathVariable("id") String id) {
      try {
          return new ResponseEntity<>(userService.deleteById(id), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  @GetMapping("/user/check-username")
  public ResponseEntity<?> checkUsername(@RequestParam("username") String username) {
    try {
      Users user = userService.selectByUsername(username);
      boolean available = (user == null || user.getUsername() == null || user.getUsername().isEmpty());
      return ResponseEntity.ok().body(java.util.Collections.singletonMap("available", available));
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  // 회원 탈퇴
  @PostMapping("/user/check-password")
  public ResponseEntity<?> checkPassword(@RequestBody Users user) {
    try {
      Users existingUser = userService.selectById(user.getId());
      if (existingUser == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("FAIL");
      }
      boolean isValid = userService.checkPassword(existingUser, user.getPassword());
      if (isValid) {
        return ResponseEntity.ok().body("SUCCESS");
      } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("FAIL");
      }
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
  /**
   * 아이디 찾기 API
   * @param request 이름과 이메일 정보
   * @return 아이디 찾기 결과
   */
  @PostMapping("/find-id")
  public ResponseEntity<?> findId(@RequestBody java.util.Map<String, String> request) {
      log.info("## 아이디 찾기 API ##");
      log.info("request={}", request);
      
      try {
          String name = request.get("name");
          String email = request.get("email");
          
          // 입력값 검증
          if (name == null || name.trim().isEmpty()) {
              return ResponseEntity.badRequest()
                  .body(createErrorResponse("이름을 입력해주세요."));
          }
          
          if (email == null || email.trim().isEmpty()) {
              return ResponseEntity.badRequest()
                  .body(createErrorResponse("이메일을 입력해주세요."));
          }
          
          // 사용자 조회
          Users user = userService.findByNameAndEmail(name.trim(), email.trim());
          
          if (user != null) {
              // 성공 응답
              java.util.Map<String, Object> response = new java.util.HashMap<>();
              response.put("success", true);
              response.put("username", user.getUsername());
              response.put("message", "아이디를 찾았습니다.");
              
              return ResponseEntity.ok(response);
          } else {
              // 실패 응답
              return ResponseEntity.ok(createErrorResponse("입력하신 정보와 일치하는 아이디가 없습니다."));
          }
          
      } catch (Exception e) {
          log.error("아이디 찾기 API 오류: {}", e.getMessage(), e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(createErrorResponse("아이디 찾기 중 오류가 발생했습니다."));
      }
  }
  
  /**
   * 비밀번호 찾기 (임시 비밀번호 발송) API
   * @param request 아이디와 이메일 정보
   * @return 임시 비밀번호 발송 결과
   */
  @PostMapping("/find-password")
  public ResponseEntity<?> findPassword(@RequestBody java.util.Map<String, String> request) {
      log.info("## 비밀번호 찾기 API ##");
      log.info("request={}", request);
      
      try {
          String username = request.get("username");
          String email = request.get("email");
          
          // 입력값 검증
          if (username == null || username.trim().isEmpty()) {
              return ResponseEntity.badRequest()
                  .body(createErrorResponse("아이디를 입력해주세요."));
          }
          
          if (email == null || email.trim().isEmpty()) {
              return ResponseEntity.badRequest()
                  .body(createErrorResponse("이메일을 입력해주세요."));
          }
          
          // 비밀번호 재설정
          boolean result = userService.resetPassword(username.trim(), email.trim());
          
          if (result) {
              // 성공 응답
              java.util.Map<String, Object> response = new java.util.HashMap<>();
              response.put("success", true);
              response.put("email", email);
              response.put("message", "임시 비밀번호가 이메일로 발송되었습니다.");
              
              return ResponseEntity.ok(response);
          } else {
              // 실패 응답
              return ResponseEntity.ok(createErrorResponse("입력하신 정보와 일치하는 계정이 없습니다."));
          }
          
      } catch (Exception e) {
          log.error("비밀번호 찾기 API 오류: {}", e.getMessage(), e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(createErrorResponse("임시 비밀번호 발송 중 오류가 발생했습니다."));
      }
  }
  
  /**
   * 에러 응답 생성 헬퍼 메서드
   * @param message 에러 메시지
   * @return 에러 응답 Map
   */
  private java.util.Map<String, Object> createErrorResponse(String message) {
      java.util.Map<String, Object> response = new java.util.HashMap<>();
      response.put("success", false);
      response.put("message", message);
      return response;
  }

}
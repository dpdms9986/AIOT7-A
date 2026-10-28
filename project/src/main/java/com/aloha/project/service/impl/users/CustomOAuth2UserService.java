package com.aloha.project.service.impl.users;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.users.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 🌐 소셜 로그인 사용자 서비스 (Google, Naver, Kakao)
 * ✅ 제공자에서 사용자 정보를 조회하고, 서비스 회원(users)과 연결한다.
 * ✅ 처음 로그인하는 사용자는 자동으로 회원가입(ROLE_USER) 처리한다.
 * ✅ username 규칙 : {provider}_{providerId}  (예: google_1234567890)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;
  private final UserService userService;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);

    String provider = userRequest.getClientRegistration().getRegistrationId();
    Map<String, Object> attributes = oAuth2User.getAttributes();
    log.info("소셜 로그인 - provider: {}", provider);

    String providerId;
    String name;
    String email;

    switch (provider) {
      case "google" -> {
        providerId = asString(attributes.get("sub"));
        name = asString(attributes.get("name"));
        email = asString(attributes.get("email"));
      }
      case "naver" -> {
        Map<String, Object> response = asMap(attributes.get("response"));
        providerId = asString(response.get("id"));
        name = asString(response.get("name"));
        email = asString(response.get("email"));
      }
      case "kakao" -> {
        Map<String, Object> account = asMap(attributes.get("kakao_account"));
        Map<String, Object> profile = asMap(account.get("profile"));
        Map<String, Object> properties = asMap(attributes.get("properties"));
        providerId = asString(attributes.get("id"));
        name = asString(profile.get("nickname"));
        if (name == null) {
          name = asString(properties.get("nickname"));
        }
        email = asString(account.get("email"));
      }
      default -> throw oauthError("지원하지 않는 소셜 로그인입니다: " + provider);
    }

    if (providerId == null || providerId.isBlank()) {
      throw oauthError("소셜 사용자 식별자를 확인할 수 없습니다.");
    }

    String username = provider + "_" + providerId;
    Users user = userRepository.findByUsername(username).orElse(null);

    if (user == null) {
      user = new Users();
      user.setId(UUID.randomUUID().toString());
      user.setUsername(username);
      user.setPassword(UUID.randomUUID().toString());      // 소셜 계정은 비밀번호 로그인 불가 (임의값)
      user.setName(name != null && !name.isBlank() ? name : provider + " 사용자");
      user.setTel("");
      user.setEmail(email != null ? email : "");
      user.setEnabled(true);

      if (!userService.join(user)) {
        throw oauthError("소셜 회원가입에 실패했습니다.");
      }
      user = userRepository.findByUsername(username).orElse(null);
      log.info("소셜 회원가입 완료: {}", username);
    }

    if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
      throw oauthError("사용할 수 없는 계정입니다.");
    }

    return new CustomUser(user, attributes);
  }

  private static OAuth2AuthenticationException oauthError(String message) {
    return new OAuth2AuthenticationException(new OAuth2Error("social_login_failed", message, null));
  }

  private static String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object value) {
    return value instanceof Map ? (Map<String, Object>) value : Map.of();
  }

}

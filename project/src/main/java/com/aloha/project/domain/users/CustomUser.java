package com.aloha.project.domain.users;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
public class CustomUser implements UserDetails, OAuth2User  {

  // 사용자 DTO
  private Users user;

  // 소셜 로그인 시 제공자가 내려준 사용자 속성
  private Map<String, Object> attributes = Collections.emptyMap();

  public CustomUser(Users user) {
      this.user = user;
  }

  public CustomUser(Users user, Map<String, Object> attributes) {
      this.user = user;
      this.attributes = attributes;
  }

  @Override
  public Map<String, Object> getAttributes() {
      return attributes;
  }

  @Override
  public String getName() {
      return user.getUsername();
  }


  /**
   * 🔐 권한 정보 메소드
   * ✅ UserDetails 를 CustomUser 로 구현하여,
   *    Spring Security 의 User 대신 사용자 정의 인증 객체(CustomUser)로 적용
   * ⚠ CustomUser 적용 시, 권한을 사용할 때는 'ROLE_' 붙여서 사용해야한다.
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {

    return user.getAuthList().stream()
                              .map( (auth) -> new SimpleGrantedAuthority(auth.getAuth()) )
                              .collect(Collectors.toList());

  }

  @Override
  public String getPassword() {
      return user.getPassword();
  }

  @Override
  public String getUsername() {
      return user.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
      return true;
  }

  @Override
  public boolean isAccountNonLocked() {
      return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
      return true;
  }

  @Override
  public boolean isEnabled() {
      return user.getEnabled();
  }
  
}

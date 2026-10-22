package com.aloha.mini.domain;

import java.util.Collection;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.ToString;

@Getter 
@ToString 
public class CustomUser implements UserDetails {

    // 회원 정보
    private Users user;

    public CustomUser(Users user) {
        this.user = user;
    }

    /** 
     * 🔐 권한 정보 메소드
     *     UserDetails 를 Customer 로 구현하여, 
     *     Spring Security 의 User 대신 사용자 정의 인증 객체(Customer로 활용
     *     CustomUser 적용 시, 권한 사용할 때 'ROLE_' 붙여 사용해야 함.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthList().stream()          
                                 .map( (auth) -> new SimpleGrantedAuthority(auth.getAuth()))           
                                 .collect(Collectors.toList());
        // user의 authList 를 스트림으로 반복하여
        // SimpleGrantedAuthority 객체로 매핑하고
        // List 형식으로 변환하여 반환 ! 
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // Spring Security의 식별자 메서드(getUsername)가 우리 엔티티의 userId(이메일)를 반환하도록 연결
        return user.getUserid();
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled() == 0 ? false : true;
    }
}

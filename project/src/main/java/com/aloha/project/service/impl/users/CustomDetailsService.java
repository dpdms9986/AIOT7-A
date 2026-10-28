package com.aloha.project.service.impl.users;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.aloha.project.domain.users.CustomUser;
import com.aloha.project.domain.users.Users;
import com.aloha.project.repository.users.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class CustomDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

    /**
     * 사용자 정보 조회
     * username: 사용자 이메일
     * return: UserDetails
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("username: " + username);
        // JPA 사용해서 사용자 정보 조회
        Users user = userRepository.findByUsername(username).orElse(null);
        log.info("user: " + user);
        if( user == null ) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다." + username);
        }

        // 🔐 CustomUser ➡ UserDetails
        CustomUser customUser = new CustomUser(user);
        return customUser;
    }
  
}

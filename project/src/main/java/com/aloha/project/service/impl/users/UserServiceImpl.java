package com.aloha.project.service.impl.users;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.domain.users.UserAuth;
import com.aloha.project.domain.users.Users;
import com.aloha.project.repository.users.UserAuthRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.impl.BaseServiceImpl;
import com.aloha.project.service.inter.common.EmailService;
import com.aloha.project.service.inter.users.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class UserServiceImpl extends BaseServiceImpl<Users, UserRepository> implements UserService {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    @Override
    public boolean join(Users entity) {

        log.info("## 회원가입 ##");
        log.info("entity={}", entity);
        
        // 회원가입 처리
        try {
            // ID(UUID) 체크
            if (entity.getId() == null || entity.getId().isEmpty()) {
                entity.setId(java.util.UUID.randomUUID().toString());
            }
            // 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(entity.getPassword());
            entity.setPassword(encodedPassword);
            entity.setNo(null);
            repository.saveAndFlush(entity);

            // 기본 권한등록 : ROLE_USER
            UserAuth userAuth = new UserAuth();
            userAuth.setUserNo(entity.getNo());
            userAuth.setUsername(entity.getUsername());
            userAuth.setName("사용자");
            userAuth.setAuth("ROLE_USER");
            userAuthRepository.saveAndFlush(userAuth);
            log.info("회원가입 성공: {}", entity.getUsername());
            return true;
        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage());
            markRollbackOnly();
            return false;
        }

    }

    @Override
    @Transactional(readOnly = true)
    public Users selectByUsername(String username) {
        try {
            Users user = repository.findByUsername(username).orElse(null);
            if (user != null) {
                log.info("사용자 {} 존재", username);
            } else {
                log.info("사용자 {} 존재하지 않음", username);
            }
            return user;
        } catch (Exception e) {
            log.error("사용자 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageInfo<Users> page(QueryParams queryParams) {
        // 검색 및 정렬 조건을 포함하여 데이터 조회
        return repository.search(queryParams);
    }

    @Override
    protected List<String> excludeOnUpdate() {
        return List.of("authList");
    }

    @Override
    public boolean update(Users entity) {
        log.info("## 회원정보 수정 ##");
        log.info("entity={}", entity);
        
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(entity.getPassword());
        entity.setPassword(encodedPassword);
        
        // 회원정보 수정
        try {
            boolean result = super.update(entity);
            if (result) {
                log.info("회원정보 수정 성공: {}", entity.getUsername());
            } else {
                log.warn("회원정보 수정 실패: {}", entity.getUsername());
            }
            return result;
        } catch (Exception e) {
            log.error("회원정보 수정 중 오류 발생: {}", e.getMessage());
            markRollbackOnly();
            return false;
        }
    }

    @Override
    public boolean updateById(Users entity) {
        log.info("## 회원정보 수정 ##");
        log.info("entity={}", entity);
        
        boolean newPasswordProvided = entity.getNewPassword() != null && !entity.getNewPassword().isEmpty();
        boolean confirmPasswordProvided = entity.getConfirmPassword() != null && !entity.getConfirmPassword().isEmpty();
        // 비밀번호 암호화
        if( !newPasswordProvided && !confirmPasswordProvided ) {
            entity.setPassword(null); // 비밀번호 변경 없음
            log.info("비밀번호 변경 없음: {}", entity.getUsername());
        } else {
            log.info("비밀번호 변경: {}", entity.getUsername());

            Users existingUser = repository.findOneById(entity.getId()).orElse(null);
            if (existingUser == null) {
                log.warn("사용자 정보가 존재하지 않습니다: {}", entity.getId());
                return false;
            }
            if (!passwordEncoder.matches(entity.getPassword(), existingUser.getPassword())) {
                log.warn("현재 비밀번호가 일치하지 않습니다: {}", entity.getUsername());
                return false;
            }
            String encodedPassword = passwordEncoder.encode(entity.getNewPassword());
            entity.setPassword(encodedPassword);
        }
        // 회원정보 수정
        try {
            boolean result = super.updateById(entity);
            if (result) {
                log.info("회원정보 수정 성공: {}", entity.getUsername());
            } else {
                log.warn("회원정보 수정 실패: {}", entity.getUsername());
            }
            return result;
        } catch (Exception e) {
            log.error("회원정보 수정 중 오류 발생: {}", e.getMessage());
            markRollbackOnly();
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkPassword(Users existingUser, String password) {
        if (existingUser == null || password == null) {
            return false;
        }
        return passwordEncoder.matches(password, existingUser.getPassword());
    }

    @Override
    @Transactional(readOnly = true)
    public Users findByNameAndEmail(String name, String email) {
        log.info("## 아이디 찾기 ##");
        log.info("name={}, email={}", name, email);
        
        try {
            // 활성화된 계정만 조회
            Users user = repository.findEnabledByNameAndEmail(name, email);
            if (user != null) {
                log.info("아이디 찾기 성공: {}", user.getUsername());
            } else {
                log.info("아이디 찾기 실패: 일치하는 사용자 없음");
            }
            return user;
        } catch (Exception e) {
            log.error("아이디 찾기 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean resetPassword(String username, String email) {
        log.info("## 비밀번호 재설정 ##");
        log.info("username={}, email={}", username, email);
        
        try {
            // 사용자 조회 (활성화된 계정만 대상)
            Users user = repository.findEnabledByUsernameAndEmail(username, email);
            if (user == null) {
                log.info("비밀번호 재설정 실패: 일치하는 사용자 없음");
                return false;
            }
            
            // 8자리 임시 비밀번호 생성
            String tempPassword = generateTempPassword();
            log.info("임시 비밀번호 생성: {}", tempPassword);
            
            // 비밀번호 암호화 후 업데이트
            String encodedPassword = passwordEncoder.encode(tempPassword);
            user.setPassword(encodedPassword);
            repository.saveAndFlush(user);

            log.info("임시 비밀번호 설정 성공: {}", username);
            
            // 임시 비밀번호 이메일 발송 (실패해도 비밀번호 재설정은 성공 처리)
            try {
                emailService.sendTempPasswordMail(user.getEmail(), username, tempPassword);
            } catch (Exception mailEx) {
                log.error("임시 비밀번호 이메일 발송 실패 (비밀번호 재설정은 완료): username={}, error={}", username, mailEx.getMessage());
            }
            
            return true;
        } catch (Exception e) {
            log.error("비밀번호 재설정 중 오류 발생: {}", e.getMessage());
            markRollbackOnly();
            return false;
        }
    }
    
    /**
     * 8자리 임시 비밀번호 생성
     * @return 임시 비밀번호
     */
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder tempPassword = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 8; i++) {
            tempPassword.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return tempPassword.toString();
    }

}

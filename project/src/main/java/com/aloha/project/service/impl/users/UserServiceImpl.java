package com.aloha.project.service.impl.users;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.UserAuth;
import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.SignupRequest;
import com.aloha.project.repository.users.TeamRepository;
import com.aloha.project.repository.users.UserRepository;
import com.aloha.project.service.inter.users.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository repository;
    private final TeamRepository teamRepository;

    // 1. 회원가입
    @Transactional
    @Override
    public boolean join(SignupRequest request) {
        log.info("## 회원가입 진행: {} ##", request.getUsername());

        try {
            Teams team = null;

            // 방제단 처리 (단장은 생성, 조종사는 초대코드로 조회)
            if ("LEADER".equals(request.getRole())) {
                String inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                team = Teams.builder()
                        .teamName(request.getTeamName().trim())
                        .inviteCode(inviteCode)
                        .createdAt(LocalDateTime.now())
                        .build();
                teamRepository.save(team);
            } else if ("PILOT".equals(request.getRole())) {
                String code = request.getInviteCode().trim().toUpperCase();
                team = teamRepository.findByInviteCode(code)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 초대코드입니다: " + code));
            }

            // Users 엔티티 생성 (엔티티 변수명 userName 반영)
            Users user = Users.builder()
                    .userName(request.getUsername().trim()) // 아이디(이메일)
                    .password(passwordEncoder.encode(request.getPassword())) // 암호화
                    .name(request.getName().trim()) // 실명
                    .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                    .role(request.getRole())
                    .team(team)
                    .enabled(true)
                    .build();

            // 권한 매핑
            UserAuth userAuth = UserAuth.builder()
                    .auth("ROLE_" + request.getRole())
                    .build();
            user.addAuth(userAuth);

            // 저장 (Cascade에 의해 UserAuth도 함께 저장됨)
            repository.save(user);

            log.info("회원가입 완료: {}", user.getUserName());
            return true;

        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // 2. 아이디(이메일)로 회원 조회
    @Override
    @Transactional(readOnly = true)
    public Users selectByUsername(String username) {
        try {
            Users user = repository.findByUserName(username).orElse(null);
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

    // 3. 페이징 조회
    @Override
    @Transactional(readOnly = true)
    public PageInfo<Users> page(QueryParams queryParams) {
        return repository.search(queryParams);
    }

    // 4. 회원정보 수정 (JPA 더티 체킹 활용)
    @Transactional
    public boolean update(Users entity) {
        log.info("## 회원정보 수정: {} ##", entity.getUserName());

        try {
            // DB에서 기존 회원 조회 (영속성 컨텍스트 관리 대상)
            Users existing = repository.findByUserName(entity.getUserName()).orElse(null);
            if (existing == null) {
                log.warn("수정할 회원을 찾을 수 없음: {}", entity.getUserName());
                return false;
            }

            // 비밀번호 변경 요청이 있을 경우 암호화하여 반영
            if (entity.getPassword() != null && !entity.getPassword().trim().isEmpty()) {
                existing.setPassword(passwordEncoder.encode(entity.getPassword()));
            }

            // 실명 및 전화번호 등 변경된 정보 반영
            if (entity.getName() != null) existing.setName(entity.getName());
            if (entity.getPhone() != null) existing.setPhone(entity.getPhone());
            if (entity.getTeam() != null) existing.setTeam(entity.getTeam());

            // JPA의 변경 감지(Dirty Checking)로 트랜잭션 종료 시 자동 UPDATE 쿼리 발생!
            log.info("회원정보 수정 성공: {}", existing.getUserName());
            return true;

        } catch (Exception e) {
            log.error("회원정보 수정 중 오류 발생: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}
   
    // @Override
    // public boolean updateById(Users entity) {
    //     log.info("## 회원정보 수정 ##");
    //     log.info("entity={}", entity);
        
    //     boolean newPasswordProvided = entity.getNewPassw ord() != null && !entity.getNewPassword().isEmpty();
    //     boolean confirmPasswordProvided = entity.getConfirmPassword() != null && !entity.getConfirmPassword().isEmpty();
    //     // 비밀번호 암호화
    //     if( !newPasswordProvided && !confirmPasswordProvided ) {
    //         entity.setPassword(null); // 비밀번호 변경 없음
    //         log.info("비밀번호 변경 없음: {}", entity.getUserName());
    //     } else {
    //         log.info("비밀번호 변경: {}", entity.getUserName());

    //         Users existingUser = repository.findOneById(entity.getId()).orElse(null);
    //         if (existingUser == null) {
    //             log.warn("사용자 정보가 존재하지 않습니다: {}", entity.getId());
    //             return false;
    //         }
    //         if (!passwordEncoder.matches(entity.getPassword(), existingUser.getPassword())) {
    //             log.warn("현재 비밀번호가 일치하지 않습니다: {}", entity.getUserName());
    //             return false;
    //         }
    //         String encodedPassword = passwordEncoder.encode(entity.getNewPassword());
    //         entity.setPassword(encodedPassword);
    //     }
    //     // 회원정보 수정
    //     try {
    //         boolean result = super.updateById(entity);
    //         if (result) {
    //             log.info("회원정보 수정 성공: {}", entity.getUserName());
    //         } else {
    //             log.warn("회원정보 수정 실패: {}", entity.getUserName());
    //         }
    //         return result;
    //     } catch (Exception e) {
    //         log.error("회원정보 수정 중 오류 발생: {}", e.getMessage());
    //         markRollbackOnly();
    //         return false;
    //     }
    // }

    // @Override
    // @Transactional(readOnly = true)
    // public boolean checkPassword(Users existingUser, String password) {
    //     if (existingUser == null || password == null) {
    //         return false;
    //     }
    //     return passwordEncoder.matches(password, existingUser.getPassword());
    // }

    // @Override
    // @Transactional(readOnly = true)
    // public Users findByNameAndEmail(String name, String email) {
    //     log.info("## 아이디 찾기 ##");
    //     log.info("name={}, email={}", name, email);
        
    //     try {
    //         // 활성화된 계정만 조회
    //         Users user = repository.findEnabledByNameAndEmail(name, email);
    //         if (user != null) {
    //             log.info("아이디 찾기 성공: {}", user.getUserName());
    //         } else {
    //             log.info("아이디 찾기 실패: 일치하는 사용자 없음");
    //         }
    //         return user;
    //     } catch (Exception e) {
    //         log.error("아이디 찾기 중 오류 발생: {}", e.getMessage());
    //         return null;
    //     }
    // }

    // @Override
    // public boolean resetPassword(String username, String email) {
    //     log.info("## 비밀번호 재설정 ##");
    //     log.info("username={}, email={}", username, email);
        
    //     try {
    //         // 사용자 조회 (활성화된 계정만 대상)
    //         Users user = repository.findEnabledByUsernameAndEmail(username, email);
    //         if (user == null) {
    //             log.info("비밀번호 재설정 실패: 일치하는 사용자 없음");
    //             return false;
    //         }
            
    //         // 8자리 임시 비밀번호 생성
    //         String tempPassword = generateTempPassword();
    //         log.info("임시 비밀번호 생성: {}", tempPassword);
            
    //         // 비밀번호 암호화 후 업데이트
    //         String encodedPassword = passwordEncoder.encode(tempPassword);
    //         user.setPassword(encodedPassword);
    //         repository.saveAndFlush(user);

    //         log.info("임시 비밀번호 설정 성공: {}", username);
            
            // // 이메일 전송
            // String subject = "비밀번호 재설정 안내";
            // String body = "안녕하세요, " + user.getName() + "님.\n\n" +
            //               "요청하신 비밀번호 재설정이 완료되었습니다.\n" +
            //               "임시 비밀번호는 다음과 같습니다:\n\n" +
            //               tempPassword + "\n\n" +
            //               "로그인 후 반드시 비밀번호를 변경해 주세요.\n\n" +
            //               "감사합니다.";
            // emailService.sendEmail(user.getEmail(), subject, body);
            // log.info("임시 비밀번호 이메일 전송 성공: {}", user.getEmail());
            // return true;
            
    //     } catch (Exception e) {
    //         log.error("비밀번호 재설정 중 오류 발생: {}", e.getMessage());
    //         markRollbackOnly();
    //         return false;
    //     }
    // }
    
    // /**
    //  * 8자리 임시 비밀번호 생성
    //  * @return 임시 비밀번호
    //  */
    // private String generateTempPassword() {
    //     String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    //     StringBuilder tempPassword = new StringBuilder();
    //     java.util.Random random = new java.util.Random();
        
    //     for (int i = 0; i < 8; i++) {
    //         tempPassword.append(chars.charAt(random.nextInt(chars.length())));
    //     }
        
    //     return tempPassword.toString();
    // }

// }

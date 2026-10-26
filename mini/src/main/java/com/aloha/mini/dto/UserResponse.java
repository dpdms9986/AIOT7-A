package com.aloha.mini.dto;

import com.aloha.mini.domain.Users;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long userNo;       // users.no
    private String userId;     // 이메일
    private String userName;   // 실명
    private String role;       // ADMIN / PILOT
    private Long teamNo;       // 소속 방제단 번호
    private String teamName;   // 소속 방제단 명칭
    private String inviteCode; // 방제단 초대코드 (단장인 경우 표시용)

    public static UserResponse from(Users user) {
        Long teamNo = (user.getTeam() != null) ? user.getTeam().getNo() : null;
        String teamName = (user.getTeam() != null) ? user.getTeam().getTeamName() : null;
        String inviteCode = (user.getTeam() != null) ? user.getTeam().getInviteCode() : null;

        return UserResponse.builder()
                .userNo(user.getNo())
                .userId(user.getUserId())
                .userName(user.getUsername())
                .role(user.getRole())
                .teamNo(teamNo)
                .teamName(teamName)
                .inviteCode(inviteCode)
                .build();
    }
}
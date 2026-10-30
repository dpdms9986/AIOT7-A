package com.aloha.project.dto;

import com.aloha.project.domain.users.Users; // 👈 Users 엔티티 import 추가!
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 방제단 관리 화면 - 조종사 한 명
 * ※ 백엔드팀과의 약속: 서비스는 이 모양(List<TeamMemberDto>)으로 데이터를 넘겨준다
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDto {

    private Long no;            // 조종사 번호 (users.no)
    private String name;        // 이름
    private String username;    // 이메일(아이디)
    private String phone;       // 연락처
    private String status;      // online / working / offline
    private String drone;       // 담당 기체 (없으면 null)
    private String currentTask; // 현재 작업 (없으면 "-")
    private int completed;      // 금일 완료 건수
    private String skill;       // 초급 / 중급 / 베테랑

    // 👇 [백엔드 필수 메서드 추가] Users 엔티티 -> TeamMemberDto 변환
    public static TeamMemberDto from(Users user) {
        if (user == null) return null;

        return TeamMemberDto.builder()
                .no(user.getNo())
                .name(user.getName())
                .username(user.getUserName())
                .phone(user.getPhone() != null ? user.getPhone() : "-")
                .status("online")  // 기본 접속 상태
                .drone("-")        // 배정 기체
                .currentTask("-")  // 배정 작업
                .completed(0)
                .skill("중급")     // 기본 숙련도
                .build();
    }
}
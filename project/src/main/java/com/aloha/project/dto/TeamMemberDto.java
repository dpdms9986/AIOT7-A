package com.aloha.project.dto;

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
    private Integer tank;       // 탱크 용량 L (없으면 null)
    private String currentTask; // 현재 작업 (없으면 "-")
    private int completed;      // 금일 완료 건수
    private String skill;       // 초급 / 중급 / 베테랑
}
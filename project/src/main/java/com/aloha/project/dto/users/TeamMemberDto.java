package com.aloha.project.dto.users;

import java.time.LocalDate;
import java.util.List;

import com.aloha.project.domain.tasks.WorkAssignments;
import com.aloha.project.domain.tasks.WorkAssignmentResources;
import com.aloha.project.domain.users.Users;

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
    private String status;      // online / working / paused / offline
    private String drone;       // 담당 기체 (없으면 null)
    private String currentTask; // 현재 작업 (없으면 "-")
    private int completed;      // 금일 완료 건수
    private String skill;       // 초급 / 중급 / 베테랑

    // Users 엔티티 -> TeamMemberDto 변환
    public static TeamMemberDto from(Users user) {
        if (user == null) return null;

        return TeamMemberDto.builder()
                .no(user.getNo())
                .name(user.getName())
                .username(user.getUserName())
                .phone(user.getPhone() != null ? user.getPhone() : "-")
                .status("online")
                .drone("-")
                .currentTask("-")
                .completed(0)
                .skill("중급")
                .build();
    }

    // 작업 목록을 받아 상태/현재 작업/금일 완료를 채우는 버전
    public static TeamMemberDto from(
            Users user,
            List<WorkAssignments> works) {

        return from(user, works, null);
    }

    // 현재 작업에 연결된 자원 정보까지 받아 사용 드론을 채우는 버전
    public static TeamMemberDto from(
            Users user,
            List<WorkAssignments> works,
            WorkAssignmentResources resources) {

        TeamMemberDto dto = from(user);

        if (dto == null) return null;

        if (works == null) {
            return dto;
        }

        // 현재 작업 중인 배정 건 조회
        WorkAssignments current = works.stream()
                .filter(w -> "IN_PROGRESS".equals(w.getStatus()))
                .findFirst()
                .orElse(null);

        // 작업 중인 배정이 없다면 일시정지된 배정 건 조회
        WorkAssignments paused = works.stream()
                .filter(w -> "PAUSED".equals(w.getStatus()))
                .findFirst()
                .orElse(null);

        // IN_PROGRESS 상태를 PAUSED보다 우선 적용
        if (current != null) {

            dto.setStatus("working");

            dto.setCurrentTask(
                    current.getLand() != null
                            ? current.getLand().getAddress()
                            : "-"
            );

            if (resources != null && resources.getDrone() != null) {
                dto.setDrone(resources.getDrone().getModelName());
            }

        } else if (paused != null) {

            // 일시정지 상태
            dto.setStatus("paused");

            dto.setCurrentTask(
                    paused.getLand() != null
                            ? paused.getLand().getAddress()
                            : "-"
            );
        }

        // 금일 완료 작업 건수
        dto.setCompleted((int) works.stream()
                .filter(w -> "COMPLETED".equals(w.getStatus())
                          && w.getCompletedAt() != null
                          && w.getCompletedAt().toLocalDate().equals(LocalDate.now()))
                .count());

        return dto;
    }
}
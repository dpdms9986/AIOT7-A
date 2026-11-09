package com.aloha.project.dto;

import com.aloha.project.domain.users.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {

    private Long no;
    private String name;
    private String username;
    private String phone;
    private String status;
    private String drone;
    private String currentTask;
    private int completed;

    public static TeamMember from(Users user) {
        if (user == null) return null;

        return TeamMember.builder()
                .no(user.getNo())
                .name(user.getName())
                .username(user.getUserName())
                .phone(user.getPhone() != null ? user.getPhone() : "-")
                .status("online")
                .drone("-")
                .currentTask("-")
                .completed(0)
                .build();
    }
}
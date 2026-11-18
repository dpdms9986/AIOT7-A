
package com.aloha.project.dto.tasks;

import java.util.List;

import com.aloha.project.domain.tasks.WorkAssignmentImages;

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
public class WorkAssignmentImageGroupResponse {

    private Long assignmentNo;

    private List<WorkAssignmentImageResponse> beforeImages;

    private List<WorkAssignmentImageResponse> afterImages;

    public static WorkAssignmentImageGroupResponse from(
            Long assignmentNo,
            List<WorkAssignmentImages> images) {

        List<WorkAssignmentImageResponse> before = images.stream()
                .filter(image -> "BEFORE".equals(image.getImageType()))
                .map(WorkAssignmentImageResponse::from)
                .toList();

        List<WorkAssignmentImageResponse> after = images.stream()
                .filter(image -> "AFTER".equals(image.getImageType()))
                .map(WorkAssignmentImageResponse::from)
                .toList();

        return WorkAssignmentImageGroupResponse.builder()
                .assignmentNo(assignmentNo)
                .beforeImages(before)
                .afterImages(after)
                .build();
    }
}

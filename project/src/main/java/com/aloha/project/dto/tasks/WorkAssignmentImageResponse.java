
package com.aloha.project.dto.tasks;

import java.time.LocalDateTime;

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
public class WorkAssignmentImageResponse {

    private Long no;

    private String imageType;

    private String originName;

    private String filePath;

    private Long fileSize;

    private LocalDateTime createdAt;

    public static WorkAssignmentImageResponse from(
            WorkAssignmentImages image) {

        return WorkAssignmentImageResponse.builder()
                .no(image.getNo())
                .imageType(image.getImageType())
                .originName(image.getOriginName())
                .filePath(image.getFilePath())
                .fileSize(image.getFileSize())
                .createdAt(image.getCreatedAt())
                .build();
    }
}

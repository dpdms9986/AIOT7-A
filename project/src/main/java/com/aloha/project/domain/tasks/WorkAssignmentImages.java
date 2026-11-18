
package com.aloha.project.domain.tasks;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "work_assignment_images")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkAssignmentImages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_assignments_no", nullable = false)
    private WorkAssignments workAssignment;

    @Column(name = "image_type", nullable = false, length = 10)
    private String imageType; // BEFORE / AFTER

    @Column(name = "original_name", nullable = false, length = 255)
    private String originName;

    @Column(name = "save_name", nullable = false, length = 255)
    private String saveName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

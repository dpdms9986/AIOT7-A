
package com.aloha.project.repository.tasks;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.Drones;
import com.aloha.project.domain.users.Teams;

public interface DroneRepository extends JpaRepository<Drones, Long> {

    // 같은 팀에 소속된 특정 상태의 드론 목록 조회
    List<Drones> findByUser_TeamAndStatus(Teams team, String status);

    // 특정 조종사 본인 소유의 전체 드론 조회
    List<Drones> findByUser_No(Long userNo);

    // 같은 방제단에 소속된 전체 드론 조회
    List<Drones> findByUser_Team_No(Long teamNo);
}
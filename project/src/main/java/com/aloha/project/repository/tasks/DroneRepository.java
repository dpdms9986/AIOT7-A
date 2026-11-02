package com.aloha.project.repository.tasks;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.tasks.Drones;
import com.aloha.project.domain.users.Teams;

public interface DroneRepository extends JpaRepository<Drones, Long> {

    // 같은 팀에 소속된 특정 상태의 드론 목록 조회
    List<Drones> findByUser_TeamAndStatus(Teams team, String status);

}
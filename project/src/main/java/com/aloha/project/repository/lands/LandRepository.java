package com.aloha.project.repository.lands;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aloha.project.domain.lands.Lands;
import com.aloha.project.domain.users.Teams;

import jakarta.persistence.LockModeType;

public interface LandRepository extends JpaRepository<Lands, Long> {
    // 상태별 농지 조회 (READY, ASSIGNED 등)
    List<Lands> findByStatus(String status);

    //⭐ [필수] 우리 방제단(팀) 소속 농지 목록 조회 (팀 격리용)
    List<Lands> findByTeam(Teams team);

    // 삭제되지 않은 우리 방제단 소속 농지 조회
    List<Lands> findByTeamAndDeletedFalse(Teams team);

    // LandRepository.java
    // 우리 팀에 해당 주소로 아직 완료(COMPLETED)되지 않은 농지가 있는지 검사
    boolean existsByTeamAndAddressAndStatusNot(Teams team, String address, String status);

    // ⭐ [비관적 락] 작업 배정 시 동시성 충돌 방지 잠금 조회 (SELECT ... FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lands l WHERE l.no = :no")
    Optional<Lands> findByIdWithLock(@Param("no") Long no);
}

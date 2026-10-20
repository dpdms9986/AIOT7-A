package com.aloha.mini.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aloha.mini.domain.Lands;

import jakarta.persistence.LockModeType;

public interface LandRepository extends JpaRepository<Lands, Long>{
    // 특정 행정구역의 전체 농지 또는 특정 상태(READY) 농지 조회 (ADM_005)
    List<Lands> findByRegion_No(Long regionsNo);
    List<Lands> findByRegion_NoAndStatus(Long regionsNo, String status);

    // 조종사 [작업 시작] 클릭 시 0.0001초 동시 진입 차단을 위한 비관적 락 조회 (PLT_006)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lands l WHERE l.no = :no")
    Optional<Lands> findByNoWithPessimisticLock(@Param("no") Long no);
}

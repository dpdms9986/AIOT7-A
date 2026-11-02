
package com.aloha.project.repository.tasks;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aloha.project.domain.tasks.Chemicals;

@Repository
public interface ChemicalRepository extends JpaRepository<Chemicals, Long> {

    // 방제단별 약제 목록 조회
    List<Chemicals> findByTeam_No(Long teamNo);

}

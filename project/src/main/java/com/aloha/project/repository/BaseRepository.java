package com.aloha.project.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.aloha.project.domain.Base;

@NoRepositoryBean
public interface BaseRepository<E extends Base> extends JpaRepository<E, Long> {

  // id(UK) 조회
  Optional<E> findOneById(String id);

  // id(UK) 삭제 (삭제된 행 수 반환)
  long removeById(String id);

}

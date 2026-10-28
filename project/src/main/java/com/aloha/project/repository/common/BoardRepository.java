package com.aloha.project.repository.common;

import org.springframework.stereotype.Repository;

import com.aloha.project.domain.common.Board;
import com.aloha.project.repository.BaseRepository;

@Repository
public interface BoardRepository extends BaseRepository<Board>, BoardRepositoryCustom {

}

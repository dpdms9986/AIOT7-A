package com.aloha.project.repository.common;

import com.aloha.project.domain.common.Board;
import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;

public interface BoardRepositoryCustom {

  // 검색 조건 페이징 조회
  PageInfo<Board> search(QueryParams queryParams);

}

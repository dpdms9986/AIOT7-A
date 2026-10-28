package com.aloha.project.service.inter.common;

import com.aloha.project.domain.common.Board;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.service.inter.BaseService;
import com.aloha.project.domain.common.PageInfo;

public interface BoardService extends BaseService<Board> {

  PageInfo<Board> page(QueryParams queryParams);

  boolean delete(Long no);                                      // no(PK) 삭제
  boolean deleteById(String id);                                // id(PK) 삭제
  
}

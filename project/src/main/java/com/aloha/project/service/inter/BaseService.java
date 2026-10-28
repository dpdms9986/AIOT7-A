package com.aloha.project.service.inter;

import java.util.List;

import com.aloha.project.domain.common.PageInfo;

public interface BaseService<E> {

  List<E> list();                                               // 전체 리스트
  PageInfo<E> page(int page, int size);                         // 페이징 리스트 (page: 페이지 번호, pageSize: 페이지당 데이터 수)
  PageInfo<E> page(int page, int size, String search);          // 페이징 리스트 (page: 페이지 번호, pageSize: 페이지당 데이터 수, search: 검색어)
  E select(Long no);                                            // no(PK) 조회
  E selectById(String id);                                      // id(PK) 조회
  boolean insert(E entity);                                     // 생성
  boolean update(E entity);                                     // 수정
  boolean updateById(E entity);                                 // id(PK) 수정
  boolean delete(Long no);                                      // no(PK) 삭제
  boolean deleteById(String id);                                // id(PK) 삭제
  
}

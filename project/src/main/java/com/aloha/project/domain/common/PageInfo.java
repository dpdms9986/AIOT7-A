package com.aloha.project.domain.common;

import java.util.List;

import lombok.Getter;
import lombok.ToString;

/**
 * [페이징 조회 결과]
 * - list     : 현재 페이지 데이터
 * - total    : 전체 데이터 수
 * - pageNum  : 현재 페이지 번호
 * - pageSize : 페이지당 데이터 수
 * - pages    : 전체 페이지 수
 */
@Getter
@ToString
public class PageInfo<T> {

  private final List<T> list;
  private final long total;
  private final int pageNum;
  private final int pageSize;
  private final int pages;
  private final int size;

  public PageInfo(List<T> list, long total, int pageNum, int pageSize) {
    this.list = list;
    this.total = total;
    this.pageNum = pageNum;
    this.pageSize = pageSize;
    this.pages = pageSize > 0 ? (int) ((total + pageSize - 1) / pageSize) : 0;
    this.size = list.size();
  }

}

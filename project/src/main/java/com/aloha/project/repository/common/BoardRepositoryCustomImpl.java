package com.aloha.project.repository.common;

import static com.aloha.project.domain.common.QBoard.board;

import java.util.List;

import org.springframework.util.StringUtils;

import com.aloha.project.domain.common.Board;
import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.utils.QuerydslUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BoardRepositoryCustomImpl implements BoardRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public PageInfo<Board> search(QueryParams queryParams) {
    BooleanBuilder where = new BooleanBuilder();

    if (queryParams.getParams() != null) {
      Object type = queryParams.getParams().get("type");
      if (type != null && StringUtils.hasText(type.toString())) {
        where.and(board.type.eq(type.toString()));
      }
    }

    String search = queryParams.getSearch();
    if (StringUtils.hasText(search)) {
      where.and(board.title.contains(search).or(board.content.contains(search)));
    }

    long page = queryParams.getPage();
    long size = queryParams.getSize();

    List<Board> list = queryFactory.selectFrom(board)
        .where(where)
        .orderBy(QuerydslUtils.orders(board, queryParams.getSortBy(), queryParams.getSortOrder(),
            board.seq.asc(), board.no.desc()))
        .offset(QuerydslUtils.offset(page, size))
        .limit(size)
        .fetch();

    Long total = queryFactory.select(board.count())
        .from(board)
        .where(where)
        .fetchOne();

    return QuerydslUtils.pageInfo(list, total == null ? 0 : total, page, size);
  }

}

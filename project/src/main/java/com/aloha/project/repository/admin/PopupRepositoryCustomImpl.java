package com.aloha.project.repository.admin;

import static com.aloha.project.domain.admin.QPopups.popups;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.util.StringUtils;

import com.aloha.project.domain.admin.Popups;
import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.utils.QuerydslUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PopupRepositoryCustomImpl implements PopupRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public PageInfo<Popups> search(QueryParams queryParams) {
    BooleanBuilder where = new BooleanBuilder();
    String search = queryParams.getSearch();
    if (StringUtils.hasText(search)) {
      where.and(popups.name.contains(search)
          .or(popups.type.contains(search))
          .or(popups.content.contains(search)));
    }

    long page = queryParams.getPage();
    long size = queryParams.getSize();

    List<Popups> list = queryFactory.selectFrom(popups)
        .where(where)
        .orderBy(QuerydslUtils.orders(popups, queryParams.getSortBy(), queryParams.getSortOrder(),
            popups.no.desc(), popups.createdAt.desc()))
        .offset(QuerydslUtils.offset(page, size))
        .limit(size)
        .fetch();

    Long total = queryFactory.select(popups.count())
        .from(popups)
        .where(where)
        .fetchOne();

    return QuerydslUtils.pageInfo(list, total == null ? 0 : total, page, size);
  }

  /**  
    SELECT *
    FROM popups
    WHERE type = ?
      AND is_show = 1
      AND started_at <= 🗓
      AND ended_at >= 🗓
   */
  @Override
  public List<Popups> findOpenByType(String type, LocalDateTime now) {
    return queryFactory.selectFrom(popups)
        .where(popups.type.eq(type),
               popups.isShow.isTrue(),
               popups.startedAt.loe(now),   // loe : less than or equal to (작거나 같다)
               popups.endedAt.goe(now))     // goe : greater than or equal to (크거나 같다)
        .fetch();
  }

}

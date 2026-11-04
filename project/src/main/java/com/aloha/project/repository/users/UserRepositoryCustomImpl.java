package com.aloha.project.repository.users;

import static com.aloha.project.domain.users.QUsers.users;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.domain.users.Users;
import com.aloha.project.utils.QuerydslUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public PageInfo<Users> search(QueryParams queryParams) {
    BooleanBuilder where = new BooleanBuilder();
    String search = queryParams.getSearch();
    if (StringUtils.hasText(search)) {
      where.and(users.userName.contains(search)
          .or(users.name.contains(search))
          .or(users.phone.contains(search)));
    }

    // 추가 검색 조건
    Map<String, Object> params = queryParams.getParams();

    if (params != null) {

        // 가입일 시작일
        Object start = params.get("start");
        if (start != null && StringUtils.hasText(start.toString())) {
            LocalDate startDate = LocalDate.parse(start.toString());
            where.and(users.createdAt.goe(startDate.atStartOfDay()));
        }

        // 가입일 종료일
        Object end = params.get("end");
        if (end != null && StringUtils.hasText(end.toString())) {
            LocalDate endDate = LocalDate.parse(end.toString());
            where.and(users.createdAt.lt(endDate.plusDays(1).atStartOfDay()));
        }

        // 활성 상태 (1: 활성, 0: 비활성)
        Object enabled = params.get("enabled");

        if ("1".equals(String.valueOf(enabled))) {
            where.and(users.enabled.isTrue());
        } else if ("0".equals(String.valueOf(enabled))) {
            where.and(users.enabled.isFalse());
        }
    }
    
    long page = queryParams.getPage();
    long size = queryParams.getSize();

    List<Users> list = queryFactory.selectFrom(users)
        .where(where)
        .orderBy(QuerydslUtils.orders(users, queryParams.getSortBy(), queryParams.getSortOrder(), users.no.desc()))
        .offset(QuerydslUtils.offset(page, size))
        .limit(size)
        .fetch();

    Long total = queryFactory.select(users.count())
        .from(users)
        .where(where)
        .fetchOne();

    return QuerydslUtils.pageInfo(list, total == null ? 0 : total, page, size);
  }

  @Override
  public Users findEnabledByNameAndEmail(String name, String email) {
    return queryFactory.selectFrom(users)
        .where(users.name.eq(name), users.userName.eq(email), users.enabled.isTrue())
        .fetchFirst();
  }

  @Override
  public Users findEnabledByUsernameAndEmail(String username, String email) {
    return queryFactory.selectFrom(users)
        .where(users.userName.eq(username), users.userName.eq(email), users.enabled.isTrue())
        .fetchFirst();
  }

}

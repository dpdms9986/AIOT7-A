package com.aloha.project.repository.users;

import static com.aloha.project.domain.users.QUsers.users;

import java.util.List;

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
      where.and(users.username.contains(search)
          .or(users.name.contains(search))
          .or(users.email.contains(search))
          .or(users.tel.contains(search)));
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
        .where(users.name.eq(name), users.email.eq(email), users.enabled.isTrue())
        .fetchFirst();
  }

  @Override
  public Users findEnabledByUsernameAndEmail(String username, String email) {
    return queryFactory.selectFrom(users)
        .where(users.username.eq(username), users.email.eq(email), users.enabled.isTrue())
        .fetchFirst();
  }

}

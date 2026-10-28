package com.aloha.project.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.aloha.project.domain.Base;
import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.repository.BaseRepository;
import com.aloha.project.service.inter.BaseService;
import com.aloha.project.utils.QuerydslUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
public class BaseServiceImpl<E extends Base, R extends BaseRepository<E>> implements BaseService<E> {

  // 수정 시 항상 제외되는 속성
  private static final List<String> NOT_UPDATABLE = List.of("no", "id", "createdAt", "updatedAt");

  @Autowired protected R repository;
  @Autowired protected JPAQueryFactory queryFactory;

  private final Class<E> entityClass;

  @SuppressWarnings("unchecked")
  protected BaseServiceImpl() {
    this.entityClass = (Class<E>) GenericTypeResolver
        .resolveTypeArguments(ClassUtils.getUserClass(getClass()), BaseServiceImpl.class)[0];
  }

  @Override
  @Transactional(readOnly = true)
  public List<E> list() {
    return repository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public PageInfo<E> page(int page, int size) {
    return page(page, size, null);
  }

  @Override
  @Transactional(readOnly = true)
  public PageInfo<E> page(int page, int size, String search) {
    int pageNum = Math.max(page, 1);
    int pageSize = size > 0 ? size : 10;

    PathBuilder<E> path = new PathBuilder<>(entityClass, "entity");
    BooleanBuilder where = new BooleanBuilder();
    // "name" 필드가 존재할 때만 검색 조건 추가
    if (StringUtils.hasText(search) && ReflectionUtils.findField(entityClass, "name") != null) {
      where.and(path.getString("name").contains(search));
    }

    List<E> list = queryFactory.selectFrom(path)
        .where(where)
        .orderBy(path.getNumber("no", Long.class).desc(), path.getDateTime("createdAt", java.util.Date.class).desc())
        .offset(QuerydslUtils.offset(pageNum, pageSize))
        .limit(pageSize)
        .fetch();

    Long total = queryFactory.select(path.count())
        .from(path)
        .where(where)
        .fetchOne();

    return QuerydslUtils.pageInfo(list, total == null ? 0 : total, pageNum, pageSize);
  }

  @Override
  @Transactional(readOnly = true)
  public E select(Long no) {
    return no == null ? null : repository.findById(no).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public E selectById(String id) {
    return id == null ? null : repository.findOneById(id).orElse(null);
  }

  @Override
  public boolean insert(E entity) {
    entity.setNo(null);
    repository.saveAndFlush(entity);
    return entity.getNo() != null;
  }

  @Override
  public boolean update(E entity) {
    if (entity.getNo() == null) {
      return false;
    }
    return repository.findById(entity.getNo())
        .map(existing -> merge(existing, entity))
        .orElse(false);
  }

  @Override
  public boolean updateById(E entity) {
    if (entity.getId() == null) {
      return false;
    }
    return repository.findOneById(entity.getId())
        .map(existing -> merge(existing, entity))
        .orElse(false);
  }

  @Override
  public boolean delete(Long no) {
    if (no == null || !repository.existsById(no)) {
      return false;
    }
    repository.deleteById(no);
    return true;
  }

  @Override
  public boolean deleteById(String id) {
    return id != null && repository.removeById(id) > 0;
  }

  /**
   * 값이 있는(null 이 아닌) 속성만 기존 엔티티에 반영하여 수정한다.
   */
  protected boolean merge(E existing, E entity) {
    BeanUtils.copyProperties(entity, existing, ignoreProperties(entity));
    repository.saveAndFlush(existing);
    return true;
  }

  // 수정 시 추가로 제외할 속성
  protected List<String> excludeOnUpdate() {
    return List.of();
  }

  private String[] ignoreProperties(E entity) {
    List<String> ignores = new ArrayList<>(NOT_UPDATABLE);
    ignores.addAll(excludeOnUpdate());
    BeanWrapper wrapper = new BeanWrapperImpl(entity);
    for (var descriptor : wrapper.getPropertyDescriptors()) {
      String name = descriptor.getName();
      if (descriptor.getReadMethod() != null && wrapper.getPropertyValue(name) == null) {
        ignores.add(name);
      }
    }
    return ignores.toArray(new String[0]);
  }

  // 예외를 처리(catch)한 뒤에도 트랜잭션이 정상 종료되도록, 현재 트랜잭션을 롤백으로 표시
  protected void markRollbackOnly() {
    try {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    } catch (NoTransactionException e) {
      log.debug("No active transaction to mark as rollback-only");
    }
  }

}

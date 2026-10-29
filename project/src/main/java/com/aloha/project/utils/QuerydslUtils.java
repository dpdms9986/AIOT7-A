package com.aloha.project.utils;

import java.util.ArrayList;
import java.util.List;

import com.aloha.project.domain.common.PageInfo;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;

public final class QuerydslUtils {

  private QuerydslUtils() {}

  /**
   * 정렬 조건 생성
   * - 엔티티에 존재하고 비교 가능한 필드만 허용 (snake_case 도 camelCase 로 변환)
   * - 유효한 정렬 조건이 없으면 defaults 를 사용
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static OrderSpecifier<?>[] orders(
      EntityPathBase<?> entity, List<String> sortBy, List<String> sortOrder, OrderSpecifier<?>... defaults) {

    List<OrderSpecifier<?>> orders = new ArrayList<>();
    if (sortBy != null) {
      PathBuilder path = new PathBuilder(entity.getType(), entity.getMetadata());
      for (int i = 0; i < sortBy.size(); i++) {
        String property = toCamelCase(sortBy.get(i));
        Class<?> type = fieldType(entity.getType(), property);
        if (type == null || !Comparable.class.isAssignableFrom(wrap(type))) {
          continue;
        }
        boolean asc = sortOrder != null && sortOrder.size() > i && "asc".equalsIgnoreCase(sortOrder.get(i));
        orders.add(new OrderSpecifier(asc ? Order.ASC : Order.DESC, path.getComparable(property, Comparable.class)));
      }
    }
    if (orders.isEmpty()) {
      return defaults;
    }
    return orders.toArray(new OrderSpecifier<?>[0]);
  }

  public static <T> PageInfo<T> pageInfo(List<T> list, long total, long page, long size) {
    return new PageInfo<>(list, total, (int) page, (int) size);
  }

  public static long offset(long page, long size) {
    return (Math.max(page, 1) - 1) * size;
  }

  private static String toCamelCase(String name) {
    if (name == null || name.indexOf('_') < 0) {
      return name;
    }
    StringBuilder sb = new StringBuilder();
    boolean upper = false;
    for (char c : name.trim().toCharArray()) {
      if (c == '_') {
        upper = true;
      } else {
        sb.append(upper ? Character.toUpperCase(c) : c);
        upper = false;
      }
    }
    return sb.toString();
  }

  private static Class<?> fieldType(Class<?> type, String name) {
    if (name == null) {
      return null;
    }
    for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
      try {
        return c.getDeclaredField(name).getType();
      } catch (NoSuchFieldException e) {
        // 상위 클래스에서 계속 탐색
      }
    }
    return null;
  }

  private static Class<?> wrap(Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    return switch (type.getName()) {
      case "int" -> Integer.class;
      case "long" -> Long.class;
      case "boolean" -> Boolean.class;
      case "double" -> Double.class;
      case "float" -> Float.class;
      case "short" -> Short.class;
      case "byte" -> Byte.class;
      default -> Character.class;
    };
  }

}

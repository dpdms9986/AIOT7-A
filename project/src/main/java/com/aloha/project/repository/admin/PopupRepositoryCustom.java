package com.aloha.project.repository.admin;

import java.time.LocalDateTime;
import java.util.List;

import com.aloha.project.domain.admin.Popups;
import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;

public interface PopupRepositoryCustom {

  // 검색 조건 페이징 조회
  PageInfo<Popups> search(QueryParams queryParams);

  // 노출 가능한 팝업 조회 (공개 + 노출 기간 내)
  List<Popups> findOpenByType(String type, LocalDateTime now);

}

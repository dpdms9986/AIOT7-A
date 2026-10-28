package com.aloha.project.service.inter.admin;

import java.util.List;

import com.aloha.project.domain.admin.Popups;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.service.inter.BaseService;
import com.aloha.project.domain.common.PageInfo;

public interface PopupService extends BaseService<Popups> {
  
    PageInfo<Popups> page(QueryParams queryParams);
  
    List<Popups> listByType(String type);
    List<Popups> listByTypeOpen(String type);

    boolean delete(Long no);                                      // no(PK) 삭제
    boolean deleteById(String id);                                // id(PK) 삭제
}

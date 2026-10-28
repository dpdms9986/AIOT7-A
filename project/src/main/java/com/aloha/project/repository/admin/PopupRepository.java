package com.aloha.project.repository.admin;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.aloha.project.domain.admin.Popups;
import com.aloha.project.repository.BaseRepository;

@Repository
public interface PopupRepository extends BaseRepository<Popups>, PopupRepositoryCustom {

  List<Popups> findByType(String type);

}

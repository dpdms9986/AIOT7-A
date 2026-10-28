package com.aloha.project.repository.common;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.aloha.project.domain.common.Media;
import com.aloha.project.repository.BaseRepository;

@Repository
public interface MediaRepository extends BaseRepository<Media> {

  Optional<Media> findFirstByParentIdOrderByNoAsc(String parentId);

  List<Media> findByParentId(String parentId);

  List<Media> findByContent(String content);

}

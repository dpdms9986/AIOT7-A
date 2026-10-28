package com.aloha.project.service.impl.common;

import org.springframework.stereotype.Service;

import com.aloha.project.domain.common.Board;
import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.repository.common.BoardRepository;
import com.aloha.project.service.impl.BaseServiceImpl;
import com.aloha.project.service.inter.common.BoardService;
import com.aloha.project.service.inter.common.MediaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor 
public class BoardServiceImpl extends BaseServiceImpl<Board, BoardRepository> implements BoardService {
  
  private final MediaService mediaService;

  @Override
  public PageInfo<Board> page(QueryParams queryParams) {
      log.info("queryParams : {}", queryParams);
      return repository.search(queryParams);
  }

  @Override
  public boolean delete(Long no) {
    try {
        // 게시판 정보 조회
        Board board = repository.findById(no).orElse(null);
        if (board == null) {
            log.warn("Board not found with no: {}", no);
            return false;
        }

        deleteBoard(board);
        log.info("Board deleted successfully - no: {}", no);
        return true;
    } catch (Exception e) {
        log.error("Error deleting board with no: {}", no, e);
        markRollbackOnly();
        return false;
    }
  }

  @Override
  public boolean deleteById(String id) {
    try {
        // id로 게시판 정보 조회
        Board board = repository.findOneById(id).orElse(null);
        if (board == null) {
            log.warn("Board not found with id: {}", id);
            return false;
        }

        deleteBoard(board);
        log.info("Board deleted successfully - id: {}", id);
        return true;
    } catch (Exception e) {
        log.error("Error deleting board with id: {}", id, e);
        markRollbackOnly();
        return false;
    }
  }

  // 첨부 파일 삭제 후 게시글 삭제
  private void deleteBoard(Board board) {
    boolean mediaDeleted = mediaService.deleteByParentId(board.getId());
    if (mediaDeleted) {
        log.info("Associated media deleted for board id: {}", board.getId());
    } else {
        log.warn("Failed to delete associated media for board id: {}", board.getId());
    }
    repository.delete(board);
    repository.flush();
  }

}

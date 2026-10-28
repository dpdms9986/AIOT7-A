package com.aloha.project.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.project.domain.common.Board;
import com.aloha.project.service.inter.common.BoardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/common/board")
@RequiredArgsConstructor 
public class BoardApi {
  
  private final BoardService boardService;
  
  @GetMapping()
  public ResponseEntity<?> getAll(
    @RequestParam(value = "page", required = false, defaultValue = "1") int page,
    @RequestParam(value = "size", required = false, defaultValue = "10") int size
  ) {
      try {
          return new ResponseEntity<>(boardService.page(page, size), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @GetMapping("/{id}")
  public ResponseEntity<?> getOne(@PathVariable("id") String id) {
      try {
          return new ResponseEntity<>(boardService.selectById(id), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PostMapping(path = "", consumes = "application/x-www-form-urlencoded")
  public ResponseEntity<?> createForm(Board board) {
      log.info("## FORM ##");
      log.info("board={}", board);
      try {
          return new ResponseEntity<>(boardService.insert(board), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  @PostMapping(path = "", consumes = "multipart/form-data")
  public ResponseEntity<?> createMultiPartForm(Board board) {
      log.info("## MULTIPART ##");
      log.info("board={}", board);
      try {
          return new ResponseEntity<>(boardService.insert(board), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  @PostMapping(path = "", consumes = "application/json")
  public ResponseEntity<?> create(@RequestBody Board board) {
      log.info("## JSON ##");
      log.info("board={}", board);
      try {
          return new ResponseEntity<>(boardService.insert(board), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PutMapping(path = "", consumes = "application/x-www-form-urlencoded")
  public ResponseEntity<?> updateForm(Board board) {
      log.info("## UPDATE FORM ##");
      try {
          return new ResponseEntity<>(boardService.updateById(board), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PutMapping(path = "", consumes = "multipart/form-data")
  public ResponseEntity<?> updateMultiPartForm(Board board) {
      log.info("## UPDATE MULTIPART ##");
      try {
          return new ResponseEntity<>(boardService.updateById(board), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PutMapping(path = "", consumes = "application/json")
  public ResponseEntity<?> update(@RequestBody Board board) {
      try {
          return new ResponseEntity<>(boardService.updateById(board), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @DeleteMapping("/{id}")
  public ResponseEntity<?> destroy(@PathVariable("id") String id) {
      try {
          return new ResponseEntity<>(boardService.deleteById(id), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

}

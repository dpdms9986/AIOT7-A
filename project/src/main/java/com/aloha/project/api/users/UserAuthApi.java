package com.aloha.project.api.users;

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

import com.aloha.project.domain.users.UserAuth;
import com.aloha.project.service.inter.users.UserAuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users/auth")
@RequiredArgsConstructor 
public class UserAuthApi {
  
  private final UserAuthService userAuthService;
  
  @GetMapping()
  public ResponseEntity<?> getAll(
    @RequestParam(value = "page", required = false, defaultValue = "1") int page,
    @RequestParam(value = "size", required = false, defaultValue = "10") int size
  ) {
      try {
          return new ResponseEntity<>(userAuthService.page(page, size), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @GetMapping("/{id}")
  public ResponseEntity<?> getOne(@PathVariable("id") String id) {
      try {
          return new ResponseEntity<>(userAuthService.selectById(id), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PostMapping(path = "", consumes = "application/x-www-form-urlencoded")
  public ResponseEntity<?> createForm(UserAuth userAuth) {
      log.info("## FORM ##");
      log.info("userAuth={}", userAuth);
      try {
          return new ResponseEntity<>(userAuthService.insert(userAuth), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  @PostMapping(path = "", consumes = "multipart/form-data")
  public ResponseEntity<?> createMultiPartForm(UserAuth userAuth) {
      log.info("## MULTIPART ##");
      log.info("userAuth={}", userAuth);
      try {
          return new ResponseEntity<>(userAuthService.insert(userAuth), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

  @PostMapping(path = "", consumes = "application/json")
  public ResponseEntity<?> create(@RequestBody UserAuth userAuth) {
      log.info("## JSON ##");
      log.info("userAuth={}", userAuth);
      try {
          return new ResponseEntity<>(userAuthService.insert(userAuth), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PutMapping(path = "", consumes = "application/x-www-form-urlencoded")
  public ResponseEntity<?> updateForm(UserAuth userAuth) {
      try {
          return new ResponseEntity<>(userAuthService.updateById(userAuth), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PutMapping(path = "", consumes = "multipart/form-data")
  public ResponseEntity<?> updateMultiPartForm(UserAuth userAuth) {
      try {
          return new ResponseEntity<>(userAuthService.updateById(userAuth), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @PutMapping(path = "", consumes = "application/json")
  public ResponseEntity<?> update(@RequestBody UserAuth userAuth) {
      try {
          return new ResponseEntity<>(userAuthService.updateById(userAuth), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
  
  @DeleteMapping("/{id}")
  public ResponseEntity<?> destroy(@PathVariable("id") String id) {
      try {
          return new ResponseEntity<>(userAuthService.deleteById(id), HttpStatus.OK);
      } catch (Exception e) {
          return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }

}

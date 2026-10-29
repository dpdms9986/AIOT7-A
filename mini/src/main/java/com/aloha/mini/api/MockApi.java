package com.aloha.mini.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aloha.mini.service.inter.sample.MockDataService;

@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
public class MockApi {

private final MockDataService mockDataService;

// POST /api/mock/lands
@PostMapping("/lands")
public ResponseEntity<String> createMockLands() {
    String result = mockDataService.createMockLands();
    return ResponseEntity.ok(result);
}

// POST /api/mock/users (유저 세팅)
@PostMapping("/users")
public ResponseEntity<String> createMockUsers() {
    String result = mockDataService.createMockUsers();
    return ResponseEntity.ok(result);
}
}
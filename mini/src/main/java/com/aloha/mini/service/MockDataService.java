package com.aloha.mini.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aloha.mini.domain.Lands;
import com.aloha.mini.domain.Regions;
import com.aloha.mini.domain.Teams;
import com.aloha.mini.domain.UserAuth;
import com.aloha.mini.domain.Users;
import com.aloha.mini.repository.LandRepository;
import com.aloha.mini.repository.RegionRepository;
import com.aloha.mini.repository.TeamRepository;
import com.aloha.mini.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MockDataService {

    private final RegionRepository regionRepository;
    private final LandRepository landRepository;
    // (유저, 팀, 비밀번호 암호화)
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String createMockLands() {

        // 1. 행정구역 생성 또는 조회(임의의 지역코드 '3011059000' 사용)
    Regions region = regionRepository.findByRegionCode("3011059000")
                                     .orElseGet(() -> {
                Regions newRegion = Regions.builder()
                        .regionCode("3011059000")
                        .sido("대전광역시")
                        .sigungu("중구")
                        .eupmyeondong("어남동")
                        .centerLat(36.2530)
                        .centerLng(127.4580)
                        .isActive(true)
                        .build();
                return regionRepository.save(newRegion);
            });
            

        // 2. 어남동 주변 가상 농지 20개 생성
        List<Lands> mockLands = new ArrayList<>();
        Random random = new Random();

        // 중심 좌표에서 위경도 ±0.005 정도의 난수 발생 (약 500m 이내 반경)
        double baseLat = region.getCenterLat();
        double baseLng = region.getCenterLng();

        for (int i = 1; i <= 20; i++) {
            double randomLat = baseLat + (random.nextDouble() - 0.5) * 0.01;
            double randomLng = baseLng + (random.nextDouble() - 0.5) * 0.01;
            // 면적은 1000 ~ 5000㎡ 사이 랜덤
            double randomArea = 1000 + (random.nextDouble() * 4000);

            // 주의: 아래 빌더의 메서드명은 Lands.java 엔티티의 변수명과 100% 일치해야 합니다.
            Lands land = Lands.builder()
                    .region(region)
                    .address("대전시 중구 어남동 산" + random.nextInt(100) + "-" + i)
                    .latitude(randomLat)  // 엔티티 변수명이 latitude 인지 확인 (lat 인 경우 .lat() 사용)
                    .longitude(randomLng) // 엔티티 변수명이 longitude 인지 확인 (lng 인 경우 .lng() 사용)
                    .areaSize(Math.round(randomArea * 10.0) / 10.0) // 엔티티 변수명이 areaSize 인지 확인
                    .status("READY")
                    .build();

            mockLands.add(land);
        }

    // 3. 20개의 농지 데이터를 DB에 한 번에 저장합니다.
    landRepository.saveAll(mockLands);

    return "어남동 주변 미배정 가상 농지 20개가 성공적으로 생성되었습니다.";
}

   @PostConstruct
 public void init() {
 createMockLands(); // 서버가 켜질 때 이 로직이 무조건 한 번 실행되게 만듦
 }

// 👇 [추가된 코드] 3가지 Role에 맞춘 초기 사용자 데이터 생성 로직
    @Transactional
    public String createMockUsers() {
        // 1. 방제단 생성
        Teams teamA = Teams.builder().teamName("알로하 방제단").build();
        teamRepository.save(teamA);

        String encodedPassword = passwordEncoder.encode("password123");

        // 2-1. 시스템 관리자 (ROLE_ADMIN) - 팀 없음
        Users sysAdmin = Users.builder()
                .userId("admin@test.com")
                .password(encodedPassword)
                .username("시스템관리자")
                .role("ROLE_ADMIN")
                .build();
        // 빌더 이용하기
        sysAdmin.addAuth(UserAuth.builder().auth("ROLE_ADMIN").build());
        userRepository.save(sysAdmin);


        // 2-2. 단장 (ROLE_LEADER) - 알로하 방제단 소속
        Users leader = Users.builder()
                .userId("leader@test.com")
                .password(encodedPassword)
                .username("단장님")
                .role("ROLE_LEADER")
                .team(teamA)
                .build();

        // 노가다
        UserAuth leaderAuth = new UserAuth();
        leaderAuth.setAuth("ROLE_LEADER");
        leader.addAuth(leaderAuth);
        userRepository.save(leader);;

        // 2-3. 조종사 (ROLE_PILOT) - 알로하 방제단 소속
        Users pilot = Users.builder()
                .userId("pilot@test.com")
                .password(encodedPassword)
                .username("조종사1")
                .role("ROLE_PILOT")
                .team(teamA)
                .build();
        UserAuth pilotAuth = new UserAuth();
        pilotAuth.setAuth("ROLE_PILOT");
        pilot.addAuth(pilotAuth);
        userRepository.save(pilot);

        return "가상 계정(ADMIN, LEADER, PILOT) 세팅 완료";



}

}
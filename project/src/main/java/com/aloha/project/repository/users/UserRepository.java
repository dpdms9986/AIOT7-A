package com.aloha.project.repository.users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.users.Teams;
import com.aloha.project.domain.users.Users;

public interface UserRepository extends JpaRepository<Users, Long>, UserRepositoryCustom {

  // 회원조회 (권한 목록 포함)
  Optional<Users> findByUserName(String userName);
  
  // UUID로 회원 조회
  Optional<Users> findOneById(String id);

  // 팀별 회원 조회
  List<Users> findByTeam(Teams team);
  
}

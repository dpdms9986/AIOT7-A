package com.aloha.project.repository.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.users.Users;

public interface UserRepository extends JpaRepository<Users, Long>, UserRepositoryCustom {

  // 회원조회 (권한 목록 포함)
  Optional<Users> findByUserName(String userName);

}

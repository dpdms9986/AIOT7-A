package com.aloha.project.repository.users;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.aloha.project.domain.users.Users;
import com.aloha.project.repository.BaseRepository;

@Repository
public interface UserRepository extends BaseRepository<Users>, UserRepositoryCustom {

  // 회원조회 (권한 목록 포함)
  Optional<Users> findByUsername(String username);

}

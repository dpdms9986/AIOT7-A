package com.aloha.project.repository.users;

import com.aloha.project.domain.common.PageInfo;
import com.aloha.project.domain.common.QueryParams;
import com.aloha.project.domain.users.Users;

public interface UserRepositoryCustom {

  // 검색 조건 페이징 조회
  PageInfo<Users> search(QueryParams queryParams);

  // 활성화된 사용자 조회 - 이름, 이메일 (아이디 찾기용)
  Users findEnabledByNameAndEmail(String name, String email);

  // 활성화된 사용자 조회 - 사용자명, 이메일 (비밀번호 재설정용)
  Users findEnabledByUsernameAndEmail(String username, String email);

}

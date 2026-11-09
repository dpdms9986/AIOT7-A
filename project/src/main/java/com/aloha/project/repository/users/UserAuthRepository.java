package com.aloha.project.repository.users;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aloha.project.domain.users.UserAuth;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

}

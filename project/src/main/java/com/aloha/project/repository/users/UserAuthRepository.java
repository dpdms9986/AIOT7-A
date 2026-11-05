package com.aloha.project.repository.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aloha.project.domain.users.UserAuth;

@Repository
public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

}

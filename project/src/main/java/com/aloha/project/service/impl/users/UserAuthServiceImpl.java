package com.aloha.project.service.impl.users;

import org.springframework.stereotype.Service;

import com.aloha.project.domain.users.UserAuth;
import com.aloha.project.repository.users.UserAuthRepository;
import com.aloha.project.service.impl.BaseServiceImpl;
import com.aloha.project.service.inter.users.UserAuthService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserAuthServiceImpl extends BaseServiceImpl<UserAuth, UserAuthRepository> implements UserAuthService {

}

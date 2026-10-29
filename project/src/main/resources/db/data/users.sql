-- Active: 1788828725682@@127.0.0.1@3306@project
-- 외래키 체크 비활성화
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `user_auth`;
TRUNCATE TABLE `users`;

-- users 샘플 데이터
INSERT INTO `users` (`id`, `username`, `password`, `name`, `tel`, `email`, `enabled`) VALUES
(UUID(), 'user@test.com', '$2a$10$fKQnGL./xuKkevPl99Y1yu2092.YF8drw.A9ZiMLe5tnJuXIs99BK', '일반사용자', '010-1111-1111', 'user@example.com', 1),
(UUID(), 'aloha@test.com', '$2a$10$fKQnGL./xuKkevPl99Y1yu2092.YF8drw.A9ZiMLe5tnJuXIs99BK', '알로하', '010-2222-2222', 'aloha@example.com', 1),
(UUID(), 'admin@test.com', '$2a$10$fKQnGL./xuKkevPl99Y1yu2092.YF8drw.A9ZiMLe5tnJuXIs99BK', '관리자', '010-9999-9999', 'admin@example.com', 1),
(UUID(), 'pilot1@test.com', '$2a$10$fKQnGL./xuKkevPl99Y1yu2092.YF8drw.A9ZiMLe5tnJuXIs99BK', '파일럿', '010-9999-9999', 'pilot1@example.com', 1),
(UUID(), 'leader1@test.com', '$2a$10$fKQnGL./xuKkevPl99Y1yu2092.YF8drw.A9ZiMLe5tnJuXIs99BK', '단장', '010-9999-9999', 'leader1@example.com', 1)
;

-- user_auth 샘플 데이터
INSERT INTO `user_auth` (`id`, `user_no`, `username`, `auth`, `name`) 
SELECT UUID(), u.no, u.username, 'ROLE_USER', u.name FROM `users` u WHERE u.username = 'user@test.com';

INSERT INTO `user_auth` (`id`, `user_no`, `username`, `auth`, `name`) 
SELECT UUID(), u.no, u.username, 'ROLE_USER', u.name FROM `users` u WHERE u.username = 'aloha@test.com';

INSERT INTO `user_auth` (`id`, `user_no`, `username`, `auth`, `name`) 
SELECT UUID(), u.no, u.username, 'ROLE_USER', u.name FROM `users` u WHERE u.username = 'admin@test.com';

INSERT INTO `user_auth` (`id`, `user_no`, `username`, `auth`, `name`) 
SELECT UUID(), u.no, u.username, 'ROLE_ADMIN', u.name FROM `users` u WHERE u.username = 'admin@test.com';

INSERT INTO `user_auth` (`id`, `user_no`, `username`, `auth`, `name`) 
SELECT UUID(), u.no, u.username, 'ROLE_USER', u.name FROM `users` u WHERE u.username = 'pilot1@test.com';

INSERT INTO `user_auth` (`id`, `user_no`, `username`, `auth`, `name`) 
SELECT UUID(), u.no, u.username, 'ROLE_PILOT', u.name FROM `users` u WHERE u.username = 'pilot1@test.com';

INSERT INTO `user_auth` (`id`, `user_no`, `username`, `auth`, `name`) 
SELECT UUID(), u.no, u.username, 'ROLE_USER', u.name FROM `users` u WHERE u.username = 'leader1@test.com';

INSERT INTO `user_auth` (`id`, `user_no`, `username`, `auth`, `name`) 
SELECT UUID(), u.no, u.username, 'ROLE_LEADER', u.name FROM `users` u WHERE u.username = 'leader1@test.com';

-- 외래키 체크 활성화
SET FOREIGN_KEY_CHECKS = 1;




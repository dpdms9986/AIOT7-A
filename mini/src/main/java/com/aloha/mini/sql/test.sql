
-- MySQL에서 직접 수정할 경우 예시
UPDATE users SET role = 'ROLE_LEADER' WHERE user_id = 'admin@test.com';

-- 또는 회원가입/Mock 데이터 생성 시 권한 3가지로 분리
-- 1. 관리자: ROLE_ADMIN
-- 2. 단장: ROLE_LEADER
-- 3. 조종사: ROLE_PILOT
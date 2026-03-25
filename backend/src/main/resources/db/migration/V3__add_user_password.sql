-- V3__add_user_password.sql
-- 이메일 회원가입을 위한 password 컬럼 추가 (nullable: OAuth 사용자는 비밀번호 없음)

ALTER TABLE users ADD COLUMN password TEXT;

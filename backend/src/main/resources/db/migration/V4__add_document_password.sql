-- 문서 비밀번호 보호 기능
ALTER TABLE documents ADD COLUMN password_hash TEXT;

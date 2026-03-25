-- V2__add_storage_usage_check_constraints.sql
-- storage_usage 테이블에 CHECK 제약 조건을 추가하여 음수 값을 DB 레벨에서 방지한다.

ALTER TABLE storage_usage
  ADD CONSTRAINT chk_total_bytes_non_negative CHECK (total_bytes >= 0);

ALTER TABLE storage_usage
  ADD CONSTRAINT chk_document_count_non_negative CHECK (document_count >= 0);

-- null 또는 빈 title을 가진 문서에 날짜 기반 기본 제목 설정
UPDATE documents
SET title = TO_CHAR(created_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD HH24:MI') || ' 문서'
WHERE title IS NULL OR TRIM(title) = '';

-- macOS NFD 파일명으로 저장된 제목을 NFC로 정규화
-- macOS HFS+는 Unicode NFD를 기본 사용하므로 드래그앤드롭 파일명이 NFD로 저장됨
-- 검색 시 NFC 입력과 매칭되도록 일괄 변환
UPDATE documents
SET title = normalize(title, NFC)
WHERE title IS NOT NULL
  AND title IS DISTINCT FROM normalize(title, NFC);

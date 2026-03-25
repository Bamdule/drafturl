package com.drafturl.api.domain.document.port;

/**
 * 파일 저장소 Port 인터페이스.
 * 도메인 계층에서 정의하며, 인프라 계층(R2FileStorage)에서 구현한다.
 */
public interface FileStorage {

    /**
     * 파일 업로드.
     *
     * @param key         R2 키 (예: documents/xK9mP2nQ/content.html)
     * @param content     파일 내용 (바이트)
     * @param contentType MIME 타입 (text/html, text/markdown)
     */
    void upload(String key, byte[] content, String contentType);

    /**
     * 파일 다운로드.
     *
     * @param key R2 키
     * @return 파일 내용 (바이트)
     */
    byte[] download(String key);

    /**
     * 파일 삭제.
     *
     * @param key R2 키
     */
    void delete(String key);
}

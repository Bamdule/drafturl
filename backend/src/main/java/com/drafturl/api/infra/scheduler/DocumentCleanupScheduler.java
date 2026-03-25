package com.drafturl.api.infra.scheduler;

import com.drafturl.api.domain.document.service.DocumentCleanupService;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료 문서 정리 스케줄러.
 * 매시 정각에 실행되며, 만료된 ACTIVE 문서를 EXPIRED로 전이하고
 * 30일 이상 경과한 EXPIRED/DELETED 문서를 물리 삭제한다.
 * 고아 문서(userId=null, expiresAt=null)에 24시간 만료를 부여한다.
 */
@Component
public class DocumentCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(DocumentCleanupScheduler.class);

    private final DocumentCleanupService documentCleanupService;

    public DocumentCleanupScheduler(DocumentCleanupService documentCleanupService) {
        this.documentCleanupService = documentCleanupService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredDocuments() {
        log.info("만료 문서 정리 스케줄러 시작");
        try {
            documentCleanupService.processExpiredDocuments();
        } catch (Exception e) {
            log.error("만료 문서 정리 스케줄러 실패", e);
            Sentry.captureException(e);
        }

        try {
            documentCleanupService.cleanupOrphanDocuments();
        } catch (Exception e) {
            log.error("고아 문서 정리 스케줄러 실패", e);
            Sentry.captureException(e);
        }
        log.info("만료 문서 정리 스케줄러 종료");
    }
}

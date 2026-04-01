package com.drafturl.api.infra.scheduler;

import com.drafturl.api.domain.document.service.DocumentCleanupService;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PENDING 문서 정리 스케줄러.
 * 매 5분마다 실행되며, 생성 후 5분이 초과된 PENDING 문서를 정리한다.
 * PENDING 상태는 R2 업로드 실패 또는 DB UPDATE 실패로 남은 문서이므로
 * 빈번하게 정리해야 한다.
 */
@Component
public class PendingCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(PendingCleanupScheduler.class);

    private final DocumentCleanupService documentCleanupService;

    public PendingCleanupScheduler(DocumentCleanupService documentCleanupService) {
        this.documentCleanupService = documentCleanupService;
    }

    @Scheduled(cron = "0 30 * * * *")
    public void cleanupPendingDocuments() {
        log.info("PENDING 문서 정리 스케줄러 시작");
        try {
            documentCleanupService.cleanupPendingDocuments();
        } catch (Exception e) {
            log.error("PENDING 문서 정리 스케줄러 실패", e);
            Sentry.captureException(e);
        }
        log.info("PENDING 문서 정리 스케줄러 종료");
    }
}

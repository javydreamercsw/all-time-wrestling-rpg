package com.github.javydreamercsw.base.ai.notion;

import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class NotionApiExecutor {

    public final NotionHandler notionHandler;
    private final NotionRateLimitService rateLimitService;
    private final NotionSyncProperties syncProperties;
    private final ExecutorService syncExecutorService;

    public NotionApiExecutor(NotionHandler notionHandler, NotionRateLimitService rateLimitService, NotionSyncProperties syncProperties) {
        this.notionHandler = notionHandler;
        this.rateLimitService = rateLimitService;
        this.syncProperties = syncProperties;
        this.syncExecutorService = Executors.newFixedThreadPool(syncProperties.getParallelThreads());
    }

    /** Execute a Notion API call with proper rate limiting. */
    public <T> T executeWithRateLimit(@NonNull Supplier<T> apiCall) throws InterruptedException {
        rateLimitService.acquirePermit();
        return apiCall.get();
    }

    @PreDestroy
    public void shutdownExecutor() {
        syncExecutorService.shutdown();
        try {
            if (!syncExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                syncExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

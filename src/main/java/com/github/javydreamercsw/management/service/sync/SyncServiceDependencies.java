package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SyncServiceDependencies {

    public final SyncProgressTracker progressTracker;

    @Autowired(required = false)
    public final SyncHealthMonitor healthMonitor;

    public final RetryService retryService;
    public final CircuitBreakerService circuitBreakerService;
    public final SyncValidationService validationService;
    public final SyncTransactionManager syncTransactionManager;
    public final DataIntegrityChecker integrityChecker;
    public final NotionRateLimitService rateLimitService;
    public final EntitySyncConfiguration entitySyncConfig;
}

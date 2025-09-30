package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestSyncConfiguration {

  @Bean
  @Primary
  public SyncHealthMonitor syncHealthMonitor(
      NotionSyncProperties syncProperties, SyncProgressTracker progressTracker) {
    return new SyncHealthMonitor(syncProperties, progressTracker);
  }
}

package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class SyncHealthMonitorConfiguration {

  @Bean
  public SyncProgressTracker syncProgressTracker() {
    return new SyncProgressTracker();
  }

  @Bean
  @Primary
  public SyncHealthMonitor syncHealthMonitor(
      NotionSyncProperties syncProperties, SyncProgressTracker progressTracker) {
    return new SyncHealthMonitor(syncProperties, progressTracker);
  }
}

/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management;

import com.github.javydreamercsw.base.Initializable;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.migration.DataMigrationRunner;
import com.github.javydreamercsw.management.sync.DataSyncContributor;
import com.github.javydreamercsw.management.sync.review.SeedReviewConfig;
import com.github.javydreamercsw.management.sync.review.SeedReviewDetectionOrchestrator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements Initializable {

  private final boolean enabled;
  private final List<DataSyncContributor> contributors;
  private final DataMigrationRunner dataMigrationRunner;
  private final SeedReviewConfig seedReviewConfig;
  private final SeedReviewDetectionOrchestrator seedReviewDetectionOrchestrator;

  @Autowired
  public DataInitializer(
      @Value("${data.initializer.enabled:true}") final boolean enabled,
      final List<DataSyncContributor> contributors,
      final DataMigrationRunner dataMigrationRunner,
      final SeedReviewConfig seedReviewConfig,
      final SeedReviewDetectionOrchestrator seedReviewDetectionOrchestrator) {
    this.enabled = enabled;
    this.contributors = contributors;
    this.dataMigrationRunner = dataMigrationRunner;
    this.seedReviewConfig = seedReviewConfig;
    this.seedReviewDetectionOrchestrator = seedReviewDetectionOrchestrator;
  }

  public void init() {
    log.debug("DataInitializer.init() called. enabled={}", enabled);
    if (enabled) {
      GeneralSecurityUtils.runAsAdmin(this::performInit);
    }
  }

  private void performInit() {
    contributors.forEach(DataSyncContributor::sync);
    dataMigrationRunner.runMigrations();
    // Seed-update review detection (PR 4): once per version change, after the syncs (all domain
    // passes complete, stamp top-ups committed) and before the escape hatch below (a keep-all
    // force-resolve must decide only after THIS boot's collisions are parked). The orchestrator
    // checks the gate once, runs the 24 content domains' version-changed branches with
    // per-domain failure isolation, closes the pending-notification phase, and stamps the
    // detected version. Parking raises the seed-review gate from inside park*; the orchestrator
    // never touches the gate itself.
    seedReviewDetectionOrchestrator.detectIfVersionChanged();
    // Operator escape hatch (seed-update review gate): runs last so detection/parking from THIS
    // boot's syncs and migrations is already parked before any force-resolve decides the rows.
    seedReviewConfig.forceResolveIfConfigured();
    log.debug("Data initialization complete.");
  }
}

/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.migration;

import com.github.javydreamercsw.management.sync.SegmentRatingRebalanceSync;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Demonstration {@link DataMigration}: the historical match-rating rescore that shipped as the
 * ad-hoc {@link SegmentRatingRebalanceSync} (ATW-gegc). Delegates to the sync so there is exactly
 * one repair implementation, and declares its {@code game_setting} marker as the legacy key —
 * installs where the sync already ran record a {@code LEGACY_SEEDED} history row instead of
 * re-running the rescore.
 *
 * <p>{@link SegmentRatingRebalanceSync} itself stays registered as a {@code DataSyncContributor}:
 * its flag keeps its own skip logic working and doubles as this migration's legacy marker.
 */
@Component
@Order(10)
public class SegmentRatingRebalanceMigration implements DataMigration {

  private final SegmentRatingRebalanceSync segmentRatingRebalanceSync;

  public SegmentRatingRebalanceMigration(final SegmentRatingRebalanceSync sync) {
    this.segmentRatingRebalanceSync = sync;
  }

  @Override
  public String id() {
    return "rescore-historical-match-ratings";
  }

  @Override
  public Optional<String> legacyGameSettingKey() {
    return Optional.of(SegmentRatingRebalanceSync.REBALANCE_DONE_KEY);
  }

  @Override
  public void migrate() {
    segmentRatingRebalanceSync.sync();
  }
}

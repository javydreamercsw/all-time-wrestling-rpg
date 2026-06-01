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
package com.github.javydreamercsw.management.service.ranking;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Percentile thresholds for tier distribution in {@link TierRecalculationService}. */
@Configuration
@ConfigurationProperties(prefix = "atw.ranking.tier-distribution")
@Data
public class TierDistributionConfig {

  /** Fraction of wrestlers in the ICON tier (top). Default: top 5%. */
  private double icon = 0.05;

  /** Fraction of wrestlers in the MAIN_EVENTER tier. Default: next 15%. */
  private double mainEventer = 0.15;

  /** Fraction of wrestlers in the MIDCARDER tier. Default: next 25%. */
  private double midcarder = 0.25;

  /** Fraction of wrestlers in the CONTENDER tier. Default: next 25%. */
  private double contender = 0.25;

  /** Fraction of wrestlers in the RISER tier. Default: next 20%. */
  private double riser = 0.20;

  // ROOKIE receives the remainder: 1.0 - (icon + mainEventer + midcarder + contender + riser)
}

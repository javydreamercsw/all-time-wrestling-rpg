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
package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InboxEventTypeConfig {

  @Bean
  public InboxEventType rivalryHeatChange() {
    return new InboxEventType("RIVALRY_HEAT_CHANGE", "Rivalry Heat Change");
  }

  @Bean
  public InboxEventType fanAdjudication() {
    return new InboxEventType("FAN_ADJUDICATION", "Fan Adjudication");
  }

  @Bean
  public InboxEventType wrestlerInjuryHealed() {
    return new InboxEventType("WRESTLER_INJURY_HEALED", "Wrestler Injury Healed");
  }

  @Bean
  public InboxEventType wrestlerInjuryObtained() {
    return new InboxEventType("WRESTLER_INJURY_OBTAINED", "Wrestler Injury Obtained");
  }

  @Bean
  public InboxEventType wrestlerBump() {
    return new InboxEventType("WRESTLER_BUMP", "Wrestler Bump");
  }

  @Bean
  public InboxEventType wrestlerBumpHealed() {
    return new InboxEventType("WRESTLER_BUMP_HEALED", "Wrestler Bump Healed");
  }

  @Bean
  public InboxEventType feudResolved() {
    return new InboxEventType("FEUD_RESOLVED", "Feud Resolved");
  }

  @Bean
  public InboxEventType adjudicationCompleted() {
    return new InboxEventType("ADJUDICATION_COMPLETED", "Adjudication Completed");
  }

  @Bean
  public InboxEventType championshipChange() {
    return new InboxEventType("CHAMPIONSHIP_CHANGE", "Championship Change");
  }

  @Bean
  public InboxEventType championshipDefended() {
    return new InboxEventType("CHAMPIONSHIP_DEFENDED", "Championship Defended");
  }

  @Bean
  public InboxEventType factionHeatChange() {
    return new InboxEventType("FACTION_HEAT_CHANGE", "Faction Heat Change");
  }

  @Bean
  public InboxEventType rivalryCompleted() {
    return new InboxEventType("RIVALRY_COMPLETED", "Rivalry Completed");
  }

  @Bean
  public InboxEventType rivalryContinues() {
    return new InboxEventType("RIVALRY_CONTINUES", "Rivalry Continues");
  }

  @Bean
  public InboxEventType segmentsApproved() {
    return new InboxEventType("SEGMENTS_APPROVED", "Segments Approved");
  }

  @Bean
  public InboxEventType feudHeatChange() {
    return new InboxEventType("FEUD_HEAT_CHANGE", "Feud Heat Change");
  }

  @Bean
  @Qualifier("DRAMA_EVENT_CREATED") InboxEventType dramaEventCreated() {
    return new InboxEventType("DRAMA_EVENT_CREATED", "Drama Event Created");
  }

  @Bean
  @Qualifier("MATCH_REQUEST") InboxEventType matchRequest() {

    return new InboxEventType("MATCH_REQUEST", "Match Request");
  }

  @Bean
  @Qualifier("DRAFT_STARTED") InboxEventType draftStarted() {

    return new InboxEventType("DRAFT_STARTED", "Draft Started");
  }

  @Bean
  @Qualifier("ACHIEVEMENT_UNLOCKED") InboxEventType achievementUnlocked() {
    return new InboxEventType("ACHIEVEMENT_UNLOCKED", "Achievement Unlocked");
  }
}

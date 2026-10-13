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
package com.github.javydreamercsw.management.event.listener;

import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.tournament.TournamentTemplateBookingService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Starts the next PLE cycle's tournaments when a PLE is adjudicated (ATW-o4ad follow-up): a PLE's
 * adjudication is the moment the following cycle begins, so SCHEDULED tournaments paired with a
 * future PLE template auto-start here — seeding and bracket generation happen now, and the weekly
 * shows leading to that PLE preview real qualifier pairings instead of placeholders.
 *
 * <p>Deliberately lenient: any failure is logged and swallowed so a tournament problem can never
 * fail a show's adjudication (the same contract the booking paths keep — approval never fails
 * because of a tournament). Only PLE adjudications trigger; weekly shows never start tournaments.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PleAdjudicationTournamentAutoStartListener
    implements ApplicationListener<AdjudicationCompletedEvent> {

  private final TournamentTemplateBookingService bookingService;

  @Override
  public void onApplicationEvent(@NonNull final AdjudicationCompletedEvent event) {
    try {
      if (event.getShow() == null || !event.getShow().isPremiumLiveEvent()) {
        return; // only a PLE's adjudication opens the next cycle
      }
      int started = bookingService.autoStartScheduledTournamentsForNextPle(event.getShow());
      if (started > 0) {
        log.info(
            "PLE '{}' adjudicated — auto-started {} scheduled tournament(s) for the next cycle",
            event.getShow().getName(),
            started);
      }
    } catch (Exception e) {
      log.error(
          "Tournament auto-start after PLE adjudication failed for show '{}' — adjudication is"
              + " unaffected",
          event.getShow() != null ? event.getShow().getName() : "?",
          e);
    }
  }
}

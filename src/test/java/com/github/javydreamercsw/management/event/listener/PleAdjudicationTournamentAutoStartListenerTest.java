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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.tournament.TournamentTemplateBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the PLE-adjudication auto-start listener's gating and leniency contract. */
@ExtendWith(MockitoExtension.class)
class PleAdjudicationTournamentAutoStartListenerTest {

  @Mock private TournamentTemplateBookingService bookingService;
  @Mock private ShowType pleShowType;

  @InjectMocks private PleAdjudicationTournamentAutoStartListener listener;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.lenient().when(pleShowType.getCategory()).thenReturn(ShowCategory.PLE);
  }

  @Test
  void pleAdjudication_delegatesToAutoStart() {
    Show ple = show(true);
    when(bookingService.autoStartScheduledTournamentsForNextPle(ple)).thenReturn(2);

    listener.onApplicationEvent(new AdjudicationCompletedEvent(this, ple));

    verify(bookingService).autoStartScheduledTournamentsForNextPle(ple);
  }

  @Test
  void weeklyAdjudication_ignored() {
    listener.onApplicationEvent(new AdjudicationCompletedEvent(this, show(false)));

    verify(bookingService, never()).autoStartScheduledTournamentsForNextPle(any());
  }

  @Test
  void nullShow_ignored() {
    listener.onApplicationEvent(new AdjudicationCompletedEvent(this, null));

    verify(bookingService, never()).autoStartScheduledTournamentsForNextPle(any());
  }

  @Test
  void bookingFailure_swallowed() {
    // Adjudication must never fail because of a tournament: any service exception is logged
    // and swallowed (the leniency contract the listener advertises).
    when(bookingService.autoStartScheduledTournamentsForNextPle(any()))
        .thenThrow(new IllegalStateException("boom"));

    assertDoesNotThrow(
        () -> listener.onApplicationEvent(new AdjudicationCompletedEvent(this, show(true))));
  }

  private Show show(boolean isPle) {
    ShowType type = new ShowType();
    type.setName(isPle ? "PLE" : "Weekly");
    Show s = new Show();
    s.setName(isPle ? "Crown PLE" : "Fed Weekly");
    if (isPle) {
      ShowTemplate template = new ShowTemplate();
      template.setShowType(pleShowType);
      s.setTemplate(template);
    } else {
      ShowTemplate template = new ShowTemplate();
      ShowType weekly = new ShowType();
      weekly.setName("Weekly");
      template.setShowType(weekly);
      s.setTemplate(template);
    }
    return s;
  }
}

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
package com.github.javydreamercsw.management.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Game-anchored clock (ATW-07hw): every game-domain timestamp derives from the in-game date ({@link
 * GameSettingService#getCurrentGameDate()}), never the wall clock — kayfabe time only moves when
 * the booker advances the in-game date.
 *
 * <p>Batch writes (NPC generation runs that stamp many rows at once) use staggered instants so
 * timestamp chains keep their ordering within a single game day.
 *
 * <p>Wall-clock uses are reserved for platform data (accounts, invites, password resets) — those
 * keep the raw {@link Clock} bean.
 */
@Service
@RequiredArgsConstructor
public class GameClock {

  /** Milliseconds between two staggered instants (max ~65k events stay within one game day). */
  private static final long STAGGER_MS = 1_000;

  private final GameSettingService gameSettingService;
  private final Clock clock;

  /** The in-game date (kayfabe "today"). */
  public LocalDate gameToday() {
    return gameSettingService.getCurrentGameDate();
  }

  /** Game-anchored "now": start of the in-game day. */
  public Instant gameNow() {
    return gameNowOn(gameToday());
  }

  /**
   * Game-anchored "now" on an explicit game date (e.g. the owning show's date). Falls back to the
   * in-game "today" when the caller has no date (e.g. an unscheduled show).
   */
  public Instant gameNowOn(LocalDate gameDate) {
    return (gameDate != null ? gameDate : gameToday()).atStartOfDay(clock.getZone()).toInstant();
  }

  /**
   * Game-anchored "now" staggered forward by one millisecond-step per call within this process, so
   * rows written in the same batch keep a strictly increasing timestamp. Stays on the game day
   * (wraps within it) so nothing drifts into tomorrow.
   */
  public Instant gameNowStaggered() {
    return gameNowStaggeredOn(gameToday());
  }

  /** Staggered variant of {@link #gameNowOn(LocalDate)}. */
  public Instant gameNowStaggeredOn(LocalDate gameDate) {
    long dayStartMillis = gameNowOn(gameDate).toEpochMilli();
    long dayMillis = 24L * 3600 * 1000;
    // Floor of the stagger counter: one slot per day so consecutive game days stay ordered.
    long slot = Math.floorMod(counter.getAndIncrement() * STAGGER_MS, dayMillis - 1);
    return Instant.ofEpochMilli(dayStartMillis + slot);
  }

  private final AtomicLong counter = new AtomicLong();
}

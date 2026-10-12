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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the game-anchored clock (ATW-07hw): every game-domain timestamp derives from the
 * in-game date, never the wall clock. Batch writes use staggered instants so timestamp chains keep
 * their order within a single game day.
 */
@ExtendWith(MockitoExtension.class)
class GameClockTest {

  private static final LocalDate GAME_DATE = LocalDate.of(2026, 7, 7);

  @Mock private GameSettingService gameSettingService;

  private GameClock gameClock;

  @BeforeEach
  void setUp() {
    gameClock = new GameClock(gameSettingService, ClockStub.atUtc());
    lenient().when(gameSettingService.getCurrentGameDate()).thenReturn(GAME_DATE);
  }

  @Test
  @DisplayName("gameToday returns the in-game date, not the wall clock")
  void gameToday_returnsGameDate() {
    assertThat(gameClock.gameToday()).isEqualTo(GAME_DATE);
  }

  @Test
  @DisplayName("gameNow anchors to the start of the in-game day")
  void gameNow_anchorsToGameDateStartOfDay() {
    Instant now = gameClock.gameNow();

    assertThat(now).isEqualTo(GAME_DATE.atStartOfDay(ZoneOffset.UTC).toInstant());
    // The wall clock is fixed at 2026-10-11 — the result must not carry it.
    assertThat(now).isBefore(LocalDate.of(2026, 7, 8).atStartOfDay(ZoneOffset.UTC).toInstant());
  }

  @Test
  @DisplayName("gameNowOn anchors to an explicit game date (e.g. the owning show's date)")
  void gameNowOn_anchorsToExplicitDate() {
    LocalDate showDate = LocalDate.of(2026, 6, 28);

    Instant onShow = gameClock.gameNowOn(showDate);

    assertThat(onShow).isEqualTo(showDate.atStartOfDay(ZoneOffset.UTC).toInstant());
  }

  @Test
  @DisplayName("staggered instants stay on the game day and strictly increase")
  void gameNowStaggered_increasesWithinGameDay() {
    Instant first = gameClock.gameNowStaggered();
    Instant second = gameClock.gameNowStaggered();
    Instant third = gameClock.gameNowStaggered();

    Instant dayEnd = GAME_DATE.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    assertThat(first.toEpochMilli()).isLessThan(second.toEpochMilli());
    assertThat(second.toEpochMilli()).isLessThan(third.toEpochMilli());
    assertThat(third.toEpochMilli()).isLessThan(dayEnd.toEpochMilli());
  }

  @Test
  @DisplayName("staggered instants on an explicit date behave the same")
  void gameNowStaggeredOn_increasesWithinGivenDay() {
    Instant first = gameClock.gameNowStaggeredOn(LocalDate.of(2026, 6, 28));
    Instant second = gameClock.gameNowStaggeredOn(LocalDate.of(2026, 6, 28));

    Instant dayStart = LocalDate.of(2026, 6, 28).atStartOfDay(ZoneOffset.UTC).toInstant();
    assertThat(first.toEpochMilli()).isLessThan(second.toEpochMilli());
    assertThat(first.toEpochMilli()).isGreaterThanOrEqualTo(dayStart.toEpochMilli());
  }

  @Test
  void gameNowOn_nullDateFallsBackToGameToday() {
    Instant expected = LocalDate.of(2026, 7, 7).atStartOfDay(ZoneOffset.UTC).toInstant();
    assertThat(gameClock.gameNowOn(null)).isEqualTo(expected);
  }

  /** Fixed wall clock (2026-10-11) — everything derived must ignore it. */
  private static final class ClockStub extends Clock {

    private static final Instant WALL_NOW = Instant.parse("2026-10-11T20:00:00Z");

    static Clock atUtc() {
      return new ClockStub();
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return WALL_NOW;
    }
  }
}

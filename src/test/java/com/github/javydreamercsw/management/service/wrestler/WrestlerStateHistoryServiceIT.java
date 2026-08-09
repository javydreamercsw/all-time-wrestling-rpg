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
package com.github.javydreamercsw.management.service.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateHistory;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateHistoryRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class WrestlerStateHistoryServiceIT extends ManagementIntegrationTest {

  @Autowired private WrestlerStateHistoryService historyService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerStateRepository stateRepository;
  @Autowired private WrestlerStateHistoryRepository historyRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerStatsService wrestlerStatsService;

  @Test
  @Transactional
  @DisplayName("recordSnapshot persists fans and tier")
  void recordSnapshot_persistsFansAndTier() {
    Universe universe = universeRepository.findAll().stream().findFirst().orElseThrow();
    Wrestler wrestler = wrestlerRepository.findAll().stream().findFirst().orElseThrow();
    WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), universe.getId());
    state.setFans(42_000L);
    state.setTier(WrestlerTier.RISER);
    stateRepository.save(state);

    historyService.recordSnapshot(state);

    List<WrestlerStateHistory> history =
        historyService.getHistory(wrestler.getId(), universe.getId());
    assertThat(history).isNotEmpty();
    WrestlerStateHistory last = history.get(history.size() - 1);
    assertThat(last.getFans()).isEqualTo(42_000L);
    assertThat(last.getTier()).isEqualTo(WrestlerTier.RISER);
    assertThat(last.getRecordedAt()).isNotNull();
  }

  @Test
  @Transactional
  @DisplayName("getHistory returns snapshots ordered chronologically")
  void getHistory_returnsOrderedChronologically() {
    Universe universe = universeRepository.findAll().stream().findFirst().orElseThrow();
    Wrestler wrestler = wrestlerRepository.findAll().stream().findFirst().orElseThrow();
    WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), universe.getId());

    state.setFans(1_000L);
    state.setTier(WrestlerTier.ROOKIE);
    stateRepository.save(state);
    historyService.recordSnapshot(state);

    state.setFans(5_000L);
    state.setTier(WrestlerTier.CONTENDER);
    stateRepository.save(state);
    historyService.recordSnapshot(state);

    List<WrestlerStateHistory> history =
        historyService.getHistory(wrestler.getId(), universe.getId());
    assertThat(history).hasSizeGreaterThanOrEqualTo(2);

    for (int i = 1; i < history.size(); i++) {
      assertThat(history.get(i).getRecordedAt())
          .isAfterOrEqualTo(history.get(i - 1).getRecordedAt());
    }
  }

  @Test
  @Transactional
  @DisplayName("getHistory returns empty list when no snapshots exist")
  void getHistory_returnsEmptyWhenNoSnapshots() {
    Universe universe = universeRepository.findAll().stream().findFirst().orElseThrow();
    Wrestler wrestler = wrestlerRepository.findAll().stream().findFirst().orElseThrow();

    List<WrestlerStateHistory> history =
        historyService.getHistory(wrestler.getId(), universe.getId());
    assertThat(history).isEmpty();
  }

  @Test
  @DisplayName("getWinRate returns 0.0 when no matches played")
  void getWinRate_zeroWhenNoMatches() {
    WrestlerStats stats = new WrestlerStats(0L, 0L, 0L);
    assertThat(stats.getWinRate()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("getWinRate computes wins / (wins + losses) * 100")
  void getWinRate_computesCorrectly() {
    WrestlerStats stats = new WrestlerStats(3L, 1L, 0L);
    assertThat(stats.getWinRate()).isEqualTo(75.0);
  }

  @Test
  @DisplayName("getWinRate returns 100.0 for undefeated record")
  void getWinRate_undefeated() {
    WrestlerStats stats = new WrestlerStats(5L, 0L, 0L);
    assertThat(stats.getWinRate()).isEqualTo(100.0);
  }

  @Test
  @Transactional
  @DisplayName("recordSnapshot does not mix data between universes")
  void recordSnapshot_isolatedPerUniverse() {
    List<Universe> universes = universeRepository.findAll();
    if (universes.size() < 2) {
      return;
    }
    Universe u1 = universes.get(0);
    Universe u2 = universes.get(1);
    Wrestler wrestler = wrestlerRepository.findAll().stream().findFirst().orElseThrow();

    WrestlerState s1 = wrestlerService.getOrCreateState(wrestler.getId(), u1.getId());
    s1.setFans(100L);
    stateRepository.save(s1);
    historyService.recordSnapshot(s1);

    List<WrestlerStateHistory> historyU1 = historyService.getHistory(wrestler.getId(), u1.getId());
    List<WrestlerStateHistory> historyU2 = historyService.getHistory(wrestler.getId(), u2.getId());

    assertThat(historyU1).isNotEmpty();
    assertThat(historyU2).isEmpty();
  }

  @Test
  @Transactional
  @DisplayName("getWrestlerStats win rate matches manual calculation")
  void winRateViaService_matchesManualCalculation() {
    Universe universe = universeRepository.findAll().stream().findFirst().orElseThrow();
    Wrestler wrestler = wrestlerRepository.findAll().stream().findFirst().orElseThrow();

    Optional<WrestlerStats> statsOpt =
        wrestlerStatsService.getWrestlerStats(wrestler.getId(), universe.getId());
    assertThat(statsOpt).isPresent();
    WrestlerStats stats = statsOpt.get();

    long total = stats.getWins() + stats.getLosses();
    double expected = total == 0 ? 0.0 : (double) stats.getWins() / total * 100.0;
    assertThat(stats.getWinRate()).isEqualTo(expected);
  }
}

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
package com.github.javydreamercsw.management.service.gm;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.wrestler.RetirementService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class GmModeService {

  private final LeagueRepository leagueRepository;
  private final SegmentRepository segmentRepository;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final SalaryCalculator salaryCalculator;
  private final RetirementService retirementService;

  private final Random random = new Random();

  /**
   * Applies post-show GM mode updates: stamina/morale changes, salary expenses, revenue
   * calculation, and locker-room morale aggregation.
   *
   * <p>No-ops when the show has no universe or the universe has no associated league.
   */
  public void processShowUpdates(
      @NonNull final Show show, @NonNull final Set<Long> participatingWrestlerIds) {
    if (show.getUniverse() == null) {
      return;
    }

    League league = leagueRepository.findByUniverse(show.getUniverse()).orElse(null);
    if (league == null) {
      return;
    }

    Long universeId = show.getUniverse().getId();

    BigDecimal totalExpenses = BigDecimal.ZERO;
    BigDecimal totalRevenue = BigDecimal.ZERO;

    double averageRating =
        segmentRepository.findByShow(show).stream()
            .mapToDouble(s -> s.getSegmentRating() != null ? s.getSegmentRating() : 0)
            .average()
            .orElse(0.0);

    if (show.getArena() != null && show.getArena().getCapacity() != null) {
      totalRevenue =
          BigDecimal.valueOf(show.getArena().getCapacity())
              .multiply(BigDecimal.valueOf(averageRating / 100.0))
              .multiply(new BigDecimal("10.00"));
    }

    List<Wrestler> allWrestlers = wrestlerRepository.findAll();
    for (Wrestler w : allWrestlers) {
      boolean participated = participatingWrestlerIds.contains(w.getId());

      WrestlerState state = wrestlerService.getOrCreateState(w.getId(), universeId);

      int currentStamina =
          state.getManagementStamina() != null ? state.getManagementStamina() : 100;
      if (participated) {
        state.setManagementStamina(Math.max(0, currentStamina - (10 + random.nextInt(11))));
      } else {
        state.setManagementStamina(Math.min(100, currentStamina + (15 + random.nextInt(11))));
      }

      int currentMorale = state.getMorale() != null ? state.getMorale() : 100;
      if (participated) {
        state.setMorale(Math.min(100, currentMorale + 2));
      } else if (state.getTier().ordinal() >= WrestlerTier.MIDCARDER.ordinal()) {
        state.setMorale(Math.max(0, currentMorale - 5));
      }

      wrestlerStateRepository.save(state);

      if (participated) {
        totalExpenses = totalExpenses.add(salaryCalculator.calculateWeeklySalary(w));
      }

      retirementService.checkRetirement(w, universeId);
    }

    BigDecimal currentBudget = league.getBudget() != null ? league.getBudget() : BigDecimal.ZERO;
    league.setBudget(currentBudget.add(totalRevenue).subtract(totalExpenses));

    List<WrestlerState> leagueStates =
        wrestlerStateRepository.findAll().stream()
            .filter(s -> s.getUniverse().equals(show.getUniverse()))
            .toList();

    if (!leagueStates.isEmpty()) {
      double avgMorale =
          leagueStates.stream().mapToInt(WrestlerState::getMorale).average().orElse(100.0);
      league.setLockerRoomMorale((int) avgMorale);
    }

    leagueRepository.save(league);

    log.info(
        "GM Mode Update for {}: Revenue: {}, Expenses: {}, New Budget: {}",
        show.getName(),
        totalRevenue,
        totalExpenses,
        league.getBudget());
  }
}

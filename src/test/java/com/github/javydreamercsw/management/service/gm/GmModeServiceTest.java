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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.wrestler.RetirementService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GmModeServiceTest {

  @Mock private LeagueRepository leagueRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private SalaryCalculator salaryCalculator;
  @Mock private RetirementService retirementService;

  private GmModeService service;

  private Universe universe;
  private Show show;
  private League league;
  private Wrestler wrestler;
  private WrestlerState state;

  @BeforeEach
  void setUp() {
    service =
        new GmModeService(
            leagueRepository,
            segmentRepository,
            wrestlerRepository,
            wrestlerService,
            wrestlerStateRepository,
            salaryCalculator,
            retirementService);

    universe = new Universe();
    universe.setId(1L);

    show = new Show();
    show.setName("Raw");
    show.setUniverse(universe);

    league = new League();
    league.setId(10L);
    league.setName("GM League");
    league.setBudget(new BigDecimal("5000.00"));

    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Alpha");

    state = new WrestlerState();
    state.setUniverse(universe);
    state.setManagementStamina(80);
    state.setMorale(70);
    state.setTier(WrestlerTier.MIDCARDER);

    when(leagueRepository.findByUniverse(universe)).thenReturn(Optional.of(league));
    when(segmentRepository.findByShow(show)).thenReturn(List.of());
    when(wrestlerRepository.findAll()).thenReturn(List.of(wrestler));
    when(wrestlerService.getOrCreateState(wrestler.getId(), universe.getId())).thenReturn(state);
    when(salaryCalculator.calculateWeeklySalary(wrestler)).thenReturn(new BigDecimal("100.00"));
    when(wrestlerStateRepository.findAll()).thenReturn(List.of(state));
  }

  @Test
  void processShowUpdates_noUniverse_skips() {
    show.setUniverse(null);

    service.processShowUpdates(show, Set.of());

    verify(leagueRepository, never()).findByUniverse(any());
  }

  @Test
  void processShowUpdates_noLeague_skips() {
    when(leagueRepository.findByUniverse(universe)).thenReturn(Optional.empty());

    service.processShowUpdates(show, Set.of());

    verify(wrestlerRepository, never()).findAll();
  }

  @Test
  void processShowUpdates_participatingWrestler_decreasesStamina() {
    state.setManagementStamina(80);

    service.processShowUpdates(show, Set.of(wrestler.getId()));

    Assertions.assertThat(state.getManagementStamina()).isLessThan(80);
    verify(wrestlerStateRepository).save(state);
  }

  @Test
  void processShowUpdates_restingWrestler_increasesStamina() {
    state.setManagementStamina(50);

    service.processShowUpdates(show, Set.of());

    Assertions.assertThat(state.getManagementStamina()).isGreaterThan(50);
    verify(wrestlerStateRepository).save(state);
  }

  @Test
  void processShowUpdates_restingMidcarder_decreasesMorale() {
    state.setMorale(70);
    state.setTier(WrestlerTier.MIDCARDER);

    service.processShowUpdates(show, Set.of());

    Assertions.assertThat(state.getMorale()).isLessThan(70);
  }

  @Test
  void processShowUpdates_participating_increasesMorale() {
    state.setMorale(70);

    service.processShowUpdates(show, Set.of(wrestler.getId()));

    Assertions.assertThat(state.getMorale()).isGreaterThan(70);
  }

  @Test
  void processShowUpdates_participating_chargesSalaryToLeague() {
    BigDecimal initialBudget = league.getBudget();

    service.processShowUpdates(show, Set.of(wrestler.getId()));

    verify(leagueRepository).save(league);
    Assertions.assertThat(league.getBudget()).isLessThan(initialBudget);
  }

  @Test
  void processShowUpdates_checksRetirementForEachWrestler() {
    service.processShowUpdates(show, Set.of());

    verify(retirementService).checkRetirement(wrestler, universe.getId());
  }

  @Test
  void processShowUpdates_updatesLockerRoomMorale() {
    state.setMorale(60);

    service.processShowUpdates(show, Set.of());

    verify(leagueRepository).save(league);
    Assertions.assertThat(league.getLockerRoomMorale()).isNotNull();
  }
}

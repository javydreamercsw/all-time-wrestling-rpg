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
package com.github.javydreamercsw.management.service.wrestler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WrestlerServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private UniverseRepository universeRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private TierBoundaryRepository tierBoundaryRepository;
  @Mock private TierRecalculationService tierRecalculationService;
  @Mock private ExpansionService expansionService;
  @Mock private com.github.javydreamercsw.management.service.injury.InjuryService injuryService;

  @InjectMocks private WrestlerService wrestlerService;

  private Wrestler wrestler;
  private WrestlerState wrestlerState;
  private List<Wrestler> wrestlers;
  private List<WrestlerState> wrestlerStates;

  @Mock private DiceBag diceBag;

  @BeforeEach
  void setUp() {
    when(expansionService.getEnabledExpansionCodes())
        .thenReturn(Collections.singletonList("BASE_GAME"));

    Universe universe = Universe.builder().id(1L).name("Default Universe").build();
    lenient().when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));

    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
    wrestler.setGender(Gender.MALE);
    wrestler.setActive(true);
    wrestler.setExpansionCode("BASE_GAME");

    wrestlerState =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(universe)
            .fans(0L)
            .tier(WrestlerTier.ROOKIE)
            .bumps(0)
            .physicalCondition(100)
            .build();

    lenient()
        .when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(eq(1L), anyLong()))
        .thenReturn(Optional.of(wrestlerState));
    lenient()
        .when(wrestlerStateRepository.save(any(WrestlerState.class)))
        .thenReturn(wrestlerState);

    initWrestlers(universe);
  }

  private void initWrestlers(Universe universe) {
    wrestlers = new ArrayList<>();
    wrestlerStates = new ArrayList<>();

    createAndAddWrestler(
        1L,
        "Active Player",
        true,
        true,
        WrestlerTier.MAIN_EVENTER,
        1000L,
        Gender.MALE,
        universe,
        AlignmentType.FACE);
    createAndAddWrestler(
        2L,
        "Active NPC",
        true,
        false,
        WrestlerTier.MAIN_EVENTER,
        900L,
        Gender.FEMALE,
        universe,
        AlignmentType.HEEL);
    createAndAddWrestler(
        3L,
        "Inactive Player",
        false,
        true,
        WrestlerTier.MIDCARDER,
        800L,
        Gender.MALE,
        universe,
        null);
    createAndAddWrestler(
        4L,
        "Inactive NPC",
        false,
        false,
        WrestlerTier.MIDCARDER,
        700L,
        Gender.FEMALE,
        universe,
        null);
    createAndAddWrestler(
        5L,
        "Active Midcarder",
        true,
        false,
        WrestlerTier.MIDCARDER,
        600L,
        Gender.MALE,
        universe,
        AlignmentType.FACE);
  }

  private void createAndAddWrestler(
      Long id,
      String name,
      boolean active,
      boolean isPlayer,
      WrestlerTier tier,
      long fans,
      Gender gender,
      Universe universe,
      AlignmentType alignment) {
    Wrestler w =
        Wrestler.builder()
            .id(id)
            .name(name)
            .active(active)
            .isPlayer(isPlayer)
            .gender(gender)
            .expansionCode("BASE_GAME")
            .build();
    if (alignment != null) {
      w.setAlignment(WrestlerAlignment.builder().alignmentType(alignment).build());
    }

    WrestlerState s =
        WrestlerState.builder()
            .wrestler(w)
            .universe(universe)
            .fans(fans)
            .tier(tier)
            .bumps(0)
            .build();

    wrestlers.add(w);
    wrestlerStates.add(s);
    lenient()
        .when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(eq(id), anyLong()))
        .thenReturn(Optional.of(s));
  }

  @Test
  void testFindAllFiltered() {
    // Given
    when(wrestlerRepository.findAllByActiveTrue())
        .thenReturn(wrestlers.stream().filter(Wrestler::getActive).toList());

    // Test 1: Filter by Alignment (FACE)
    List<Wrestler> faceWrestlers = wrestlerService.findAllFiltered(AlignmentType.FACE, null, null);
    assertEquals(2, faceWrestlers.size());

    // Test 2: Filter by Gender (FEMALE)
    List<Wrestler> femaleWrestlers = wrestlerService.findAllFiltered(null, Gender.FEMALE, null);
    assertEquals(1, femaleWrestlers.size());
    assertEquals("Active NPC", femaleWrestlers.get(0).getName());
  }

  @Test
  void testAddBump_PublishesEvent() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));

    // When
    wrestlerService.addBump(1L, 1L);

    // Then
    verify(eventPublisher, atLeastOnce()).publishEvent(any(WrestlerBumpEvent.class));
  }

  @Test
  void testHealChance_PublishesEvent() {
    // Given
    wrestlerState.setBumps(1);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(diceBag.roll()).thenReturn(4); // Ensure bump is healed

    // When
    wrestlerService.healChance(1L, 1L, diceBag);

    // Then
    verify(eventPublisher, atLeastOnce()).publishEvent(any(WrestlerBumpHealedEvent.class));
  }

  @Test
  void testRecalibrateFanCounts() {
    // Given
    WrestlerTier tier = WrestlerTier.CONTENDER;
    wrestlerState.setTier(tier);
    wrestlerState.setFans(500_000L);

    TierBoundary boundary = new TierBoundary();
    boundary.setTier(tier);
    boundary.setMinFans(40000L);
    boundary.setGender(Gender.MALE);

    when(wrestlerRepository.findAll()).thenReturn(Collections.singletonList(wrestler));
    when(tierBoundaryRepository.findAll()).thenReturn(Collections.singletonList(boundary));

    // When
    wrestlerService.recalibrateFanCounts(1L);

    // Then
    assertEquals(40000L, wrestlerState.getFans());
    verify(wrestlerStateRepository).save(wrestlerState);
  }

  @Test
  void testGetPlayerWrestlers() {
    // Given
    when(wrestlerRepository.findAllByActiveTrue())
        .thenReturn(wrestlers.stream().filter(Wrestler::getActive).toList());

    // When
    List<Wrestler> result = wrestlerService.getPlayerWrestlers();

    // Then
    assertEquals(1, result.size());
    assertEquals("Active Player", result.get(0).getName());
  }

  @Test
  void testGetWrestlersByTier() {
    // Given
    when(wrestlerRepository.findAllByActiveTrue())
        .thenReturn(wrestlers.stream().filter(Wrestler::getActive).toList());

    // When
    List<Wrestler> result = wrestlerService.getWrestlersByTier(WrestlerTier.MAIN_EVENTER, 1L);

    // Then
    assertEquals(2, result.size());
  }
}

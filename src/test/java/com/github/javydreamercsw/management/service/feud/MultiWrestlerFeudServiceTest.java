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
package com.github.javydreamercsw.management.service.feud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudRole;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.FeudHeatChangeEvent;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MultiWrestlerFeudServiceTest {

  @Mock private MultiWrestlerFeudRepository multiWrestlerFeudRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseContextService universeContextService;
  @Mock private Clock clock;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private MultiWrestlerFeudService multiWrestlerFeudService;

  private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;
  private MultiWrestlerFeud activeFeud;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(FIXED_INSTANT);
    when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

    wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler One");

    wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler Two");

    wrestler3 = new Wrestler();
    wrestler3.setId(3L);
    wrestler3.setName("Wrestler Three");

    activeFeud = new MultiWrestlerFeud();
    activeFeud.setId(10L);
    activeFeud.setName("Active Feud");
    activeFeud.setDescription("A test feud");
    activeFeud.setStorylineNotes("Some notes");
    activeFeud.setHeat(5);
    activeFeud.setIsActive(true);
    activeFeud.setStartedDate(FIXED_INSTANT);
    activeFeud.setCreationDate(FIXED_INSTANT);
    activeFeud.setParticipants(new ArrayList<>());
  }

  // ==================== getAllFeuds ====================

  @Test
  void getAllFeuds_delegatesToRepository() {
    Page<MultiWrestlerFeud> page = new PageImpl<>(List.of(activeFeud));
    when(multiWrestlerFeudRepository.findAllBy(any(Pageable.class))).thenReturn(page);

    Page<MultiWrestlerFeud> result = multiWrestlerFeudService.getAllFeuds(Pageable.unpaged());

    assertThat(result.getContent()).containsExactly(activeFeud);
    verify(multiWrestlerFeudRepository).findAllBy(Pageable.unpaged());
  }

  // ==================== getFeudById ====================

  @Test
  void getFeudById_found_returnsFeud() {
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));

    Optional<MultiWrestlerFeud> result = multiWrestlerFeudService.getFeudById(10L);

    assertThat(result).isPresent().contains(activeFeud);
  }

  @Test
  void getFeudById_notFound_returnsEmpty() {
    when(multiWrestlerFeudRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<MultiWrestlerFeud> result = multiWrestlerFeudService.getFeudById(99L);

    assertThat(result).isEmpty();
  }

  // ==================== getFeudByName ====================

  @Test
  void getFeudByName_found_returnsFeud() {
    when(multiWrestlerFeudRepository.findByName("Active Feud")).thenReturn(Optional.of(activeFeud));

    Optional<MultiWrestlerFeud> result = multiWrestlerFeudService.getFeudByName("Active Feud");

    assertThat(result).isPresent().contains(activeFeud);
  }

  @Test
  void getFeudByName_notFound_returnsEmpty() {
    when(multiWrestlerFeudRepository.findByName("Unknown")).thenReturn(Optional.empty());

    Optional<MultiWrestlerFeud> result = multiWrestlerFeudService.getFeudByName("Unknown");

    assertThat(result).isEmpty();
  }

  // ==================== getActiveFeuds ====================

  @Test
  void getActiveFeuds_delegatesToRepository() {
    when(multiWrestlerFeudRepository.findByIsActiveTrue()).thenReturn(List.of(activeFeud));

    List<MultiWrestlerFeud> result = multiWrestlerFeudService.getActiveFeuds();

    assertThat(result).containsExactly(activeFeud);
    verify(multiWrestlerFeudRepository).findByIsActiveTrue();
  }

  // ==================== getActiveFeudsForWrestler ====================

  @Test
  void getActiveFeudsForWrestler_wrestlerNotFound_returnsEmptyList() {
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());

    List<MultiWrestlerFeud> result = multiWrestlerFeudService.getActiveFeudsForWrestler(99L);

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).findActiveFeudsForWrestler(any());
  }

  @Test
  void getActiveFeudsForWrestler_wrestlerFound_delegatesToRepository() {
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(multiWrestlerFeudRepository.findActiveFeudsForWrestler(wrestler1))
        .thenReturn(List.of(activeFeud));

    List<MultiWrestlerFeud> result = multiWrestlerFeudService.getActiveFeudsForWrestler(1L);

    assertThat(result).containsExactly(activeFeud);
    verify(multiWrestlerFeudRepository).findActiveFeudsForWrestler(wrestler1);
  }

  // ==================== createFeud (3-arg) ====================

  @Test
  void createFeud_threeArgs_delegatesTo4ArgOverload() {
    when(multiWrestlerFeudRepository.existsByName("New Feud")).thenReturn(false);
    when(multiWrestlerFeudRepository.saveAndFlush(any(MultiWrestlerFeud.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.createFeud("New Feud", "Description", "Notes");

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("New Feud");
    assertThat(result.get().getParticipants()).isEmpty();
  }

  // ==================== createFeud (4-arg) ====================

  @Test
  void createFeud_nameAlreadyExists_returnsEmpty() {
    when(multiWrestlerFeudRepository.existsByName("Active Feud")).thenReturn(true);

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.createFeud("Active Feud", "Desc", "Notes", List.of());

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void createFeud_withEmptyWrestlerList_createsFeudWithNoParticipants() {
    when(multiWrestlerFeudRepository.existsByName("Brand New Feud")).thenReturn(false);
    when(multiWrestlerFeudRepository.saveAndFlush(any(MultiWrestlerFeud.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.createFeud("Brand New Feud", "Desc", "Notes", List.of());

    assertThat(result).isPresent();
    MultiWrestlerFeud feud = result.get();
    assertThat(feud.getName()).isEqualTo("Brand New Feud");
    assertThat(feud.getDescription()).isEqualTo("Desc");
    assertThat(feud.getStorylineNotes()).isEqualTo("Notes");
    assertThat(feud.getIsActive()).isTrue();
    assertThat(feud.getHeat()).isZero();
    assertThat(feud.getParticipants()).isEmpty();
  }

  @Test
  void createFeud_withValidWrestlers_addsParticipants() {
    when(multiWrestlerFeudRepository.existsByName("Multi Feud")).thenReturn(false);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler2));
    when(multiWrestlerFeudRepository.saveAndFlush(any(MultiWrestlerFeud.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.createFeud("Multi Feud", "Desc", "Notes", List.of(1L, 2L));

    assertThat(result).isPresent();
    assertThat(result.get().getParticipants()).hasSize(2);
  }

  @Test
  void createFeud_withMissingWrestlerId_skipsMissingAndAddsRest() {
    when(multiWrestlerFeudRepository.existsByName("Partial Feud")).thenReturn(false);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findById(999L)).thenReturn(Optional.empty());
    when(multiWrestlerFeudRepository.saveAndFlush(any(MultiWrestlerFeud.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.createFeud("Partial Feud", "Desc", "Notes", List.of(1L, 999L));

    assertThat(result).isPresent();
    // Only wrestler1 was found; 999L was skipped
    assertThat(result.get().getParticipants()).hasSize(1);
    assertThat(result.get().getParticipants().get(0).getWrestler()).isEqualTo(wrestler1);
  }

  @Test
  void createFeud_setsStartedDateAndCreationDateFromClock() {
    when(multiWrestlerFeudRepository.existsByName("Timed Feud")).thenReturn(false);
    when(multiWrestlerFeudRepository.saveAndFlush(any(MultiWrestlerFeud.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.createFeud("Timed Feud", "Desc", "Notes", List.of());

    assertThat(result).isPresent();
    assertThat(result.get().getStartedDate()).isEqualTo(FIXED_INSTANT);
    assertThat(result.get().getCreationDate()).isEqualTo(FIXED_INSTANT);
  }

  // ==================== addParticipant ====================

  @Test
  void addParticipant_feudNotFound_returnsEmpty() {
    when(multiWrestlerFeudRepository.findById(99L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.addParticipant(99L, 1L, FeudRole.PROTAGONIST);

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void addParticipant_wrestlerNotFound_returnsEmpty() {
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.addParticipant(10L, 99L, FeudRole.ANTAGONIST);

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void addParticipant_feudInactive_returnsEmpty() {
    activeFeud.setIsActive(false);
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.addParticipant(10L, 1L, FeudRole.PROTAGONIST);

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void addParticipant_wrestlerAlreadyInFeud_returnsEmpty() {
    // Pre-add wrestler1 to the feud participants list
    activeFeud.addParticipant(wrestler1, FeudRole.NEUTRAL);
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.addParticipant(10L, 1L, FeudRole.PROTAGONIST);

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void addParticipant_success_savesAndReturnsUpdatedFeud() {
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(multiWrestlerFeudRepository.saveAndFlush(activeFeud)).thenReturn(activeFeud);

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.addParticipant(10L, 1L, FeudRole.PROTAGONIST);

    assertThat(result).isPresent().contains(activeFeud);
    assertThat(activeFeud.hasParticipant(wrestler1)).isTrue();
    verify(multiWrestlerFeudRepository).saveAndFlush(activeFeud);
  }

  // ==================== removeParticipant ====================

  @Test
  void removeParticipant_feudNotFound_returnsEmpty() {
    when(multiWrestlerFeudRepository.findById(99L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.removeParticipant(99L, 1L, "Gone");

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void removeParticipant_wrestlerNotFound_returnsEmpty() {
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.removeParticipant(10L, 99L, "Gone");

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void removeParticipant_wrestlerNotInFeud_returnsEmpty() {
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    // wrestler1 has NOT been added to activeFeud

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.removeParticipant(10L, 1L, "Never was here");

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void removeParticipant_success_savesAndReturnsUpdatedFeud() {
    activeFeud.addParticipant(wrestler1, FeudRole.PROTAGONIST);
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(multiWrestlerFeudRepository.saveAndFlush(activeFeud)).thenReturn(activeFeud);

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.removeParticipant(10L, 1L, "Injury");

    assertThat(result).isPresent().contains(activeFeud);
    // After removal, hasParticipant should return false (participant is marked inactive)
    assertThat(activeFeud.hasParticipant(wrestler1)).isFalse();
    verify(multiWrestlerFeudRepository).saveAndFlush(activeFeud);
  }

  // ==================== addHeat ====================

  @Test
  void addHeat_feudNotFound_returnsEmpty() {
    when(multiWrestlerFeudRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<MultiWrestlerFeud> result = multiWrestlerFeudService.addHeat(99L, 5, "Promo battle");

    assertThat(result).isEmpty();
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void addHeat_feudInactive_returnsEmpty() {
    activeFeud.setIsActive(false);
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));

    Optional<MultiWrestlerFeud> result = multiWrestlerFeudService.addHeat(10L, 5, "Promo battle");

    assertThat(result).isEmpty();
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void addHeat_success_updatesHeatAndPublishesEvent() {
    int initialHeat = activeFeud.getHeat(); // 5
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));
    when(multiWrestlerFeudRepository.saveAndFlush(activeFeud)).thenReturn(activeFeud);

    Optional<MultiWrestlerFeud> result =
        multiWrestlerFeudService.addHeat(10L, 10, "Big match aftermath");

    assertThat(result).isPresent();
    assertThat(result.get().getHeat()).isEqualTo(initialHeat + 10);
    verify(multiWrestlerFeudRepository).saveAndFlush(activeFeud);
    verify(eventPublisher).publishEvent(any(FeudHeatChangeEvent.class));
  }

  // ==================== endFeud ====================

  @Test
  void endFeud_feudNotFound_returnsEmpty() {
    when(multiWrestlerFeudRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<MultiWrestlerFeud> result = multiWrestlerFeudService.endFeud(99L, "Over");

    assertThat(result).isEmpty();
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void endFeud_feudAlreadyInactive_returnsFeudWithoutSaving() {
    activeFeud.setIsActive(false);
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));

    Optional<MultiWrestlerFeud> result = multiWrestlerFeudService.endFeud(10L, "Already done");

    assertThat(result).isPresent().contains(activeFeud);
    verify(multiWrestlerFeudRepository, never()).saveAndFlush(any());
  }

  @Test
  void endFeud_activeFeud_endsAndSaves() {
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));
    when(multiWrestlerFeudRepository.saveAndFlush(activeFeud)).thenReturn(activeFeud);

    Optional<MultiWrestlerFeud> result = multiWrestlerFeudService.endFeud(10L, "Champion decided");

    assertThat(result).isPresent();
    assertThat(result.get().getIsActive()).isFalse();
    verify(multiWrestlerFeudRepository).saveAndFlush(activeFeud);
  }

  // ==================== getFeudsRequiringMatches ====================

  @Test
  void getFeudsRequiringMatches_delegatesToRepository() {
    when(multiWrestlerFeudRepository.findFeudsRequiringMatches()).thenReturn(List.of(activeFeud));

    List<MultiWrestlerFeud> result = multiWrestlerFeudService.getFeudsRequiringMatches();

    assertThat(result).containsExactly(activeFeud);
    verify(multiWrestlerFeudRepository).findFeudsRequiringMatches();
  }

  // ==================== getFeudsEligibleForResolution ====================

  @Test
  void getFeudsEligibleForResolution_delegatesToRepository() {
    when(multiWrestlerFeudRepository.findFeudsEligibleForResolution())
        .thenReturn(List.of(activeFeud));

    List<MultiWrestlerFeud> result = multiWrestlerFeudService.getFeudsEligibleForResolution();

    assertThat(result).containsExactly(activeFeud);
    verify(multiWrestlerFeudRepository).findFeudsEligibleForResolution();
  }

  // ==================== getHottestFeuds ====================

  @Test
  void getHottestFeuds_delegatesToRepository() {
    when(multiWrestlerFeudRepository.findHottestFeuds(any(Pageable.class)))
        .thenReturn(List.of(activeFeud));

    List<MultiWrestlerFeud> result = multiWrestlerFeudService.getHottestFeuds(5);

    assertThat(result).containsExactly(activeFeud);
    verify(multiWrestlerFeudRepository).findHottestFeuds(any(Pageable.class));
  }

  // ==================== getLargestFeuds ====================

  @Test
  void getLargestFeuds_delegatesToRepository() {
    when(multiWrestlerFeudRepository.findLargestFeuds(any(Pageable.class)))
        .thenReturn(List.of(activeFeud));

    List<MultiWrestlerFeud> result = multiWrestlerFeudService.getLargestFeuds(3);

    assertThat(result).containsExactly(activeFeud);
    verify(multiWrestlerFeudRepository).findLargestFeuds(any(Pageable.class));
  }

  // ==================== countActiveFeudsForWrestler ====================

  @Test
  void countActiveFeudsForWrestler_wrestlerNotFound_returnsZero() {
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());

    long count = multiWrestlerFeudService.countActiveFeudsForWrestler(99L);

    assertThat(count).isZero();
    verify(multiWrestlerFeudRepository, never()).countActiveFeudsForWrestler(any());
  }

  @Test
  void countActiveFeudsForWrestler_wrestlerFound_delegatesToRepository() {
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(multiWrestlerFeudRepository.countActiveFeudsForWrestler(wrestler1)).thenReturn(3L);

    long count = multiWrestlerFeudService.countActiveFeudsForWrestler(1L);

    assertThat(count).isEqualTo(3L);
    verify(multiWrestlerFeudRepository).countActiveFeudsForWrestler(wrestler1);
  }

  // ==================== getRecentFeuds ====================

  @Test
  void getRecentFeuds_delegatesToRepository() {
    Instant expectedSince = FIXED_INSTANT.minusSeconds(7L * 24 * 3600);
    when(multiWrestlerFeudRepository.findRecentFeuds(expectedSince))
        .thenReturn(List.of(activeFeud));

    List<MultiWrestlerFeud> result = multiWrestlerFeudService.getRecentFeuds(7);

    assertThat(result).containsExactly(activeFeud);
    verify(multiWrestlerFeudRepository).findRecentFeuds(expectedSince);
  }

  // ==================== isValidMultiWrestlerFeud ====================

  @Test
  void isValidMultiWrestlerFeud_feudNotFound_returnsFalse() {
    when(multiWrestlerFeudRepository.findById(99L)).thenReturn(Optional.empty());

    boolean result = multiWrestlerFeudService.isValidMultiWrestlerFeud(99L);

    assertThat(result).isFalse();
  }

  @Test
  void isValidMultiWrestlerFeud_feud_withLessThanThreeParticipants_returnsFalse() {
    // Add only 2 participants
    activeFeud.addParticipant(wrestler1, FeudRole.PROTAGONIST);
    activeFeud.addParticipant(wrestler2, FeudRole.ANTAGONIST);
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));

    boolean result = multiWrestlerFeudService.isValidMultiWrestlerFeud(10L);

    assertThat(result).isFalse();
  }

  @Test
  void isValidMultiWrestlerFeud_feudWithThreePlusParticipants_returnsTrue() {
    activeFeud.addParticipant(wrestler1, FeudRole.PROTAGONIST);
    activeFeud.addParticipant(wrestler2, FeudRole.ANTAGONIST);
    activeFeud.addParticipant(wrestler3, FeudRole.NEUTRAL);
    when(multiWrestlerFeudRepository.findById(10L)).thenReturn(Optional.of(activeFeud));

    boolean result = multiWrestlerFeudService.isValidMultiWrestlerFeud(10L);

    assertThat(result).isTrue();
  }

  // ==================== getFeudStatistics ====================

  @Test
  void getFeudStatistics_noActiveFeuds_returnsZeroStats() {
    when(multiWrestlerFeudRepository.findByIsActiveTrue()).thenReturn(List.of());
    when(multiWrestlerFeudRepository.findValidMultiWrestlerFeuds()).thenReturn(List.of());

    MultiWrestlerFeudService.FeudStatistics stats = multiWrestlerFeudService.getFeudStatistics();

    assertThat(stats.totalActiveFeuds()).isZero();
    assertThat(stats.validMultiWrestlerFeuds()).isZero();
    assertThat(stats.totalParticipants()).isZero();
    assertThat(stats.averageParticipants()).isZero();
  }

  @Test
  void getFeudStatistics_computesTotalsCorrectly() {
    // feud1: 3 participants
    MultiWrestlerFeud feud1 = new MultiWrestlerFeud();
    feud1.setId(1L);
    feud1.setName("Feud One");
    feud1.setIsActive(true);
    feud1.setParticipants(new ArrayList<>());
    feud1.addParticipant(wrestler1, FeudRole.PROTAGONIST);
    feud1.addParticipant(wrestler2, FeudRole.ANTAGONIST);
    feud1.addParticipant(wrestler3, FeudRole.NEUTRAL);

    // feud2: 1 participant
    Wrestler wrestler4 = new Wrestler();
    wrestler4.setId(4L);
    wrestler4.setName("Wrestler Four");
    MultiWrestlerFeud feud2 = new MultiWrestlerFeud();
    feud2.setId(2L);
    feud2.setName("Feud Two");
    feud2.setIsActive(true);
    feud2.setParticipants(new ArrayList<>());
    feud2.addParticipant(wrestler4, FeudRole.WILD_CARD);

    when(multiWrestlerFeudRepository.findByIsActiveTrue()).thenReturn(List.of(feud1, feud2));
    when(multiWrestlerFeudRepository.findValidMultiWrestlerFeuds()).thenReturn(List.of(feud1));

    MultiWrestlerFeudService.FeudStatistics stats = multiWrestlerFeudService.getFeudStatistics();

    assertThat(stats.totalActiveFeuds()).isEqualTo(2);
    assertThat(stats.validMultiWrestlerFeuds()).isEqualTo(1);
    assertThat(stats.totalParticipants()).isEqualTo(4); // 3 + 1
    assertThat(stats.averageParticipants()).isEqualTo(2.0); // 4 / 2
  }

  // ==================== deleteFeud ====================

  @Test
  void deleteFeud_delegatesToRepository() {
    multiWrestlerFeudService.deleteFeud(10L);

    verify(multiWrestlerFeudRepository).deleteById(10L);
  }
}

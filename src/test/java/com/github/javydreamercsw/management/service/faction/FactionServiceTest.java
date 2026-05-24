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
package com.github.javydreamercsw.management.service.faction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FactionServiceTest {

  @Mock private FactionRepository factionRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseRepository universeRepository;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private ExpansionService expansionService;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseSettingsService universeSettingsService;
  @Mock private Clock clock;
  @Mock private DefaultImageService imageService;

  @InjectMocks private FactionService factionService;

  private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
  private static final String BASE_EXPANSION = "BASE_GAME";

  private Universe universe;
  private Wrestler leader;
  private WrestlerState leaderState;
  private Faction faction;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(FIXED_INSTANT);
    when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(List.of(BASE_EXPANSION));
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    when(universeSettingsService.getEnabledExpansionCodesForUniverse(any()))
        .thenReturn(Set.of(BASE_EXPANSION));

    universe = Universe.builder().build();
    universe.setId(1L);

    leader = Wrestler.builder().build();
    leader.setId(10L);
    leader.setName("Test Leader");
    leader.setExpansionCode(BASE_EXPANSION);

    leaderState =
        WrestlerState.builder()
            .wrestler(leader)
            .universe(universe)
            .tier(WrestlerTier.ROOKIE)
            .build();
    leaderState.setId(100L);

    faction = Faction.builder().build();
    faction.setId(1L);
    faction.setName("Test Faction");
    faction.setActive(true);
    faction.setFormedDate(FIXED_INSTANT);
    faction.setCreationDate(FIXED_INSTANT);
    faction.setUniverse(universe);
  }

  // ==================== findAll ====================

  @Test
  void findAll_returnsFilteredList() {
    // Faction has no members — passes expansion filter vacuously
    when(factionRepository.findAll()).thenReturn(List.of(faction));

    List<Faction> result = factionService.findAll();

    assertThat(result).containsExactly(faction);
    verify(factionRepository).findAll();
  }

  @Test
  void findAll_excludesFactionWhoseMemberHasDisabledExpansion() {
    Wrestler disabledWrestler = Wrestler.builder().build();
    disabledWrestler.setId(99L);
    disabledWrestler.setExpansionCode("DISABLED_EXPANSION");

    WrestlerState disabledState =
        WrestlerState.builder()
            .wrestler(disabledWrestler)
            .universe(universe)
            .tier(WrestlerTier.ROOKIE)
            .build();

    Set<WrestlerState> members = new HashSet<>();
    members.add(disabledState);

    Faction disabledFaction = Faction.builder().members(members).build();
    disabledFaction.setId(2L);
    disabledFaction.setName("Disabled Faction");

    when(factionRepository.findAll()).thenReturn(List.of(faction, disabledFaction));

    List<Faction> result = factionService.findAll();

    // Only the faction with no members (passes vacuously) is returned
    assertThat(result).containsExactly(faction);
  }

  // ==================== findAllByUniverse ====================

  @Test
  void findAllByUniverse_found_returnsFilteredList() {
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(factionRepository.findByUniverseWithLeaderAndManager(universe))
        .thenReturn(List.of(faction));

    List<Faction> result = factionService.findAllByUniverse(1L);

    assertThat(result).containsExactly(faction);
    verify(universeRepository).findById(1L);
    verify(factionRepository).findByUniverseWithLeaderAndManager(universe);
  }

  @Test
  void findAllByUniverse_universeNotFound_throwsException() {
    when(universeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> factionService.findAllByUniverse(99L))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void findAllByUniverse_emptyList_returnsEmpty() {
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(factionRepository.findByUniverseWithLeaderAndManager(universe)).thenReturn(List.of());

    List<Faction> result = factionService.findAllByUniverse(1L);

    assertThat(result).isEmpty();
  }

  // ==================== getAllFactions ====================

  @Test
  void getAllFactions_delegatesToFindAll() {
    when(factionRepository.findAll()).thenReturn(List.of(faction));

    List<Faction> result = factionService.getAllFactions();

    assertThat(result).containsExactly(faction);
  }

  // ==================== getAllFactions(Pageable) ====================

  @Test
  void getAllFactions_withPageable_returnsPage() {
    when(factionRepository.findAll()).thenReturn(List.of(faction));
    Pageable pageable = PageRequest.of(0, 10);

    Page<Faction> result = factionService.getAllFactions(pageable);

    assertThat(result.getContent()).containsExactly(faction);
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void getAllFactions_withUnpaged_returnsAllInSinglePage() {
    when(factionRepository.findAll()).thenReturn(List.of(faction));

    Page<Faction> result = factionService.getAllFactions(Pageable.unpaged());

    assertThat(result.getContent()).containsExactly(faction);
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void getAllFactions_withPageOffsetBeyondResults_returnsEmptyPage() {
    when(factionRepository.findAll()).thenReturn(List.of(faction));
    Pageable pageable = PageRequest.of(5, 10); // offset 50, only 1 element

    Page<Faction> result = factionService.getAllFactions(pageable);

    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  // ==================== getFactionById ====================

  @Test
  void getFactionById_found_returnsFaction() {
    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));

    Optional<Faction> result = factionService.getFactionById(1L);

    assertThat(result).isPresent().contains(faction);
  }

  @Test
  void getFactionById_notFound_returnsEmpty() {
    when(factionRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Faction> result = factionService.getFactionById(99L);

    assertThat(result).isEmpty();
  }

  // ==================== getFactionByName ====================

  @Test
  void getFactionByName_found_returnsFaction() {
    when(factionRepository.findByName("Test Faction")).thenReturn(Optional.of(faction));

    Optional<Faction> result = factionService.getFactionByName("Test Faction");

    assertThat(result).isPresent().contains(faction);
  }

  @Test
  void getFactionByName_notFound_returnsEmpty() {
    when(factionRepository.findByName("Unknown")).thenReturn(Optional.empty());

    Optional<Faction> result = factionService.getFactionByName("Unknown");

    assertThat(result).isEmpty();
  }

  // ==================== getActiveFactions ====================

  @Test
  void getActiveFactions_returnsList() {
    when(factionRepository.findByIsActiveTrue()).thenReturn(List.of(faction));

    List<Faction> result = factionService.getActiveFactions();

    assertThat(result).containsExactly(faction);
    verify(factionRepository).findByIsActiveTrue();
  }

  @Test
  void getActiveFactions_excludesMembersWithDisabledExpansion() {
    Wrestler offExpansion = Wrestler.builder().build();
    offExpansion.setId(55L);
    offExpansion.setExpansionCode("OTHER");

    WrestlerState offState =
        WrestlerState.builder()
            .wrestler(offExpansion)
            .universe(universe)
            .tier(WrestlerTier.ROOKIE)
            .build();

    Set<WrestlerState> members = new HashSet<>();
    members.add(offState);

    Faction filteredOut = Faction.builder().members(members).build();
    filteredOut.setId(5L);
    filteredOut.setName("Off Expansion");
    filteredOut.setActive(true);

    when(factionRepository.findByIsActiveTrue()).thenReturn(List.of(faction, filteredOut));

    List<Faction> result = factionService.getActiveFactions();

    assertThat(result).containsExactly(faction);
  }

  // ==================== createFaction ====================

  @Test
  void createFaction_success_returnsCreatedFaction() {
    when(factionRepository.existsByName("New Faction")).thenReturn(false);
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(10L, 1L))
        .thenReturn(Optional.of(leaderState));
    when(factionRepository.saveAndFlush(any(Faction.class))).thenAnswer(inv -> inv.getArgument(0));

    Optional<Faction> result = factionService.createFaction("New Faction", "A faction", 10L, 1L);

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("New Faction");
    assertThat(result.get().isActive()).isTrue();
    assertThat(result.get().getFormedDate()).isEqualTo(FIXED_INSTANT);
    verify(factionRepository).saveAndFlush(any(Faction.class));
  }

  @Test
  void createFaction_nameAlreadyExists_returnsEmpty() {
    when(factionRepository.existsByName("Test Faction")).thenReturn(true);

    Optional<Faction> result = factionService.createFaction("Test Faction", "desc", 10L, 1L);

    assertThat(result).isEmpty();
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  void createFaction_universeNotFound_throwsException() {
    when(factionRepository.existsByName("New Faction")).thenReturn(false);
    when(universeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> factionService.createFaction("New Faction", "desc", 10L, 99L))
        .isInstanceOf(java.util.NoSuchElementException.class);

    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  void createFaction_leaderNotFound_stillCreatesWithoutLeader() {
    when(factionRepository.existsByName("New Faction")).thenReturn(false);
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(wrestlerRepository.findById(999L)).thenReturn(Optional.empty());
    when(factionRepository.saveAndFlush(any(Faction.class))).thenAnswer(inv -> inv.getArgument(0));

    Optional<Faction> result = factionService.createFaction("New Faction", "desc", 999L, 1L);

    assertThat(result).isPresent();
    assertThat(result.get().getLeader()).isNull();
  }

  @Test
  void createFaction_createsWrestlerStateWhenMissing() {
    when(factionRepository.existsByName("New Faction")).thenReturn(false);
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(10L, 1L))
        .thenReturn(Optional.empty());
    when(wrestlerStateRepository.save(any(WrestlerState.class))).thenReturn(leaderState);
    when(factionRepository.saveAndFlush(any(Faction.class))).thenAnswer(inv -> inv.getArgument(0));

    Optional<Faction> result = factionService.createFaction("New Faction", "desc", 10L, 1L);

    assertThat(result).isPresent();
    verify(wrestlerStateRepository).save(any(WrestlerState.class));
  }

  // ==================== addMemberToFaction ====================

  @Test
  void addMemberToFaction_success_addsMemberAndSaves() {
    Wrestler newMember = Wrestler.builder().build();
    newMember.setId(20L);
    newMember.setName("New Member");

    WrestlerState newState =
        WrestlerState.builder()
            .wrestler(newMember)
            .universe(universe)
            .tier(WrestlerTier.ROOKIE)
            .build();
    newState.setId(200L);

    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));
    when(wrestlerRepository.findById(20L)).thenReturn(Optional.of(newMember));
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(20L, 1L))
        .thenReturn(Optional.of(newState));
    when(factionRepository.saveAndFlush(faction)).thenReturn(faction);

    Optional<Faction> result = factionService.addMemberToFaction(1L, 20L);

    assertThat(result).isPresent().contains(faction);
    verify(factionRepository).saveAndFlush(faction);
  }

  @Test
  void addMemberToFaction_factionNotFound_returnsEmpty() {
    when(factionRepository.findById(99L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));

    Optional<Faction> result = factionService.addMemberToFaction(99L, 10L);

    assertThat(result).isEmpty();
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  void addMemberToFaction_wrestlerNotFound_returnsEmpty() {
    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Faction> result = factionService.addMemberToFaction(1L, 99L);

    assertThat(result).isEmpty();
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  void addMemberToFaction_factionHasNoUniverse_returnsEmpty() {
    Faction noUniverseFaction = Faction.builder().build();
    noUniverseFaction.setId(2L);
    noUniverseFaction.setName("No Universe");
    // universe is null by default

    when(factionRepository.findById(2L)).thenReturn(Optional.of(noUniverseFaction));
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));

    Optional<Faction> result = factionService.addMemberToFaction(2L, 10L);

    assertThat(result).isEmpty();
    verify(factionRepository, never()).saveAndFlush(any());
  }

  // ==================== removeMemberFromFaction ====================

  @Test
  void removeMemberFromFaction_success_removesMemberAndSaves() {
    faction.addMember(leaderState);

    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(10L, 1L))
        .thenReturn(Optional.of(leaderState));
    when(factionRepository.saveAndFlush(faction)).thenReturn(faction);

    Optional<Faction> result = factionService.removeMemberFromFaction(1L, 10L, "Left");

    assertThat(result).isPresent().contains(faction);
    verify(factionRepository).saveAndFlush(faction);
  }

  @Test
  void removeMemberFromFaction_factionNotFound_returnsEmpty() {
    when(factionRepository.findById(99L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));

    Optional<Faction> result = factionService.removeMemberFromFaction(99L, 10L, "reason");

    assertThat(result).isEmpty();
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  void removeMemberFromFaction_removingLeader_clearsLeader() {
    faction.setLeader(leader);
    faction.addMember(leaderState);

    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(10L, 1L))
        .thenReturn(Optional.of(leaderState));
    when(factionRepository.saveAndFlush(faction)).thenReturn(faction);

    Optional<Faction> result = factionService.removeMemberFromFaction(1L, 10L, "Stepped down");

    assertThat(result).isPresent();
    assertThat(result.get().getLeader()).isNull();
  }

  // ==================== changeFactionLeader ====================

  @Test
  void changeFactionLeader_success_updatesLeaderAndSaves() {
    Wrestler newLeader = Wrestler.builder().build();
    newLeader.setId(20L);
    newLeader.setName("New Leader");

    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));
    when(wrestlerRepository.findById(20L)).thenReturn(Optional.of(newLeader));
    when(factionRepository.saveAndFlush(faction)).thenReturn(faction);

    Optional<Faction> result = factionService.changeFactionLeader(1L, 20L);

    assertThat(result).isPresent();
    assertThat(faction.getLeader()).isEqualTo(newLeader);
    verify(factionRepository).saveAndFlush(faction);
  }

  @Test
  void changeFactionLeader_factionNotFound_returnsEmpty() {
    when(factionRepository.findById(99L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));

    Optional<Faction> result = factionService.changeFactionLeader(99L, 10L);

    assertThat(result).isEmpty();
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  void changeFactionLeader_newLeaderNotFound_returnsEmpty() {
    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Faction> result = factionService.changeFactionLeader(1L, 99L);

    assertThat(result).isEmpty();
    verify(factionRepository, never()).saveAndFlush(any());
  }

  // ==================== disbandFaction ====================

  @Test
  void disbandFaction_success_disbandsFactionAndSaves() {
    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));
    when(wrestlerStateRepository.saveAllAndFlush(any())).thenReturn(List.of());
    when(factionRepository.saveAndFlush(faction)).thenReturn(faction);

    Optional<Faction> result = factionService.disbandFaction(1L, "Story complete");

    assertThat(result).isPresent();
    assertThat(faction.isActive()).isFalse();
    verify(factionRepository).saveAndFlush(faction);
  }

  @Test
  void disbandFaction_notFound_returnsEmpty() {
    when(factionRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Faction> result = factionService.disbandFaction(99L, "reason");

    assertThat(result).isEmpty();
    verify(factionRepository, never()).saveAndFlush(any());
  }

  @Test
  void disbandFaction_alreadyDisbanded_returnsExistingFactionWithoutResaving() {
    faction.setActive(false);
    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));

    Optional<Faction> result = factionService.disbandFaction(1L, "Already done");

    assertThat(result).isPresent().contains(faction);
    verify(factionRepository, never()).saveAndFlush(any());
  }

  // ==================== getFactionForWrestler ====================

  @Test
  void getFactionForWrestler_found_returnsFaction() {
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));
    when(factionRepository.findActiveFactionByMember(leader)).thenReturn(Optional.of(faction));

    Optional<Faction> result = factionService.getFactionForWrestler(10L);

    assertThat(result).isPresent().contains(faction);
  }

  @Test
  void getFactionForWrestler_wrestlerNotInFaction_returnsEmpty() {
    when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(leader));
    when(factionRepository.findActiveFactionByMember(leader)).thenReturn(Optional.empty());

    Optional<Faction> result = factionService.getFactionForWrestler(10L);

    assertThat(result).isEmpty();
  }

  @Test
  void getFactionForWrestler_wrestlerNotFound_returnsEmpty() {
    when(wrestlerRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Faction> result = factionService.getFactionForWrestler(99L);

    assertThat(result).isEmpty();
    verify(factionRepository, never()).findActiveFactionByMember(any());
  }

  // ==================== getLargestFactions ====================

  @Test
  void getLargestFactions_returnsLimitedList() {
    when(factionRepository.findLargestFactions(any(Pageable.class))).thenReturn(List.of(faction));

    List<Faction> result = factionService.getLargestFactions(5);

    assertThat(result).containsExactly(faction);
    verify(factionRepository).findLargestFactions(any(Pageable.class));
  }

  // ==================== save ====================

  @Test
  void save_existingFaction_delegatesToRepository() {
    when(factionRepository.saveAndFlush(faction)).thenReturn(faction);

    Faction result = factionService.save(faction);

    assertThat(result).isEqualTo(faction);
    verify(factionRepository).saveAndFlush(faction);
  }

  @Test
  void save_newFaction_setsCreationDateAndFormedDate() {
    Faction newFaction = Faction.builder().build();
    newFaction.setName("Brand New");
    // id is null — new entity
    when(factionRepository.saveAndFlush(newFaction)).thenReturn(newFaction);

    factionService.save(newFaction);

    assertThat(newFaction.getCreationDate()).isEqualTo(FIXED_INSTANT);
    assertThat(newFaction.getFormedDate()).isEqualTo(FIXED_INSTANT);
  }

  @Test
  void save_newFactionWithExistingFormedDate_preservesFormedDate() {
    Instant preExistingFormedDate = Instant.parse("2025-06-01T00:00:00Z");
    Faction newFaction = Faction.builder().formedDate(preExistingFormedDate).build();
    newFaction.setName("Pre-dated");
    when(factionRepository.saveAndFlush(newFaction)).thenReturn(newFaction);

    factionService.save(newFaction);

    assertThat(newFaction.getFormedDate()).isEqualTo(preExistingFormedDate);
  }

  // ==================== deleteById ====================

  @Test
  void deleteById_delegatesToRepository() {
    factionService.deleteById(1L);

    verify(factionRepository).deleteById(1L);
  }

  // ==================== count ====================

  @Test
  void count_returnsRepositoryCount() {
    when(factionRepository.count()).thenReturn(7L);

    long result = factionService.count();

    assertThat(result).isEqualTo(7L);
    verify(factionRepository).count();
  }

  // ==================== addAffinity ====================

  @Test
  void addAffinity_factionFound_updatesAffinityAndSaves() {
    faction.setAffinity(50);
    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));
    when(factionRepository.saveAndFlush(faction)).thenReturn(faction);

    factionService.addAffinity(1L, 20);

    assertThat(faction.getAffinity()).isEqualTo(70);
    verify(factionRepository).saveAndFlush(faction);
  }

  @Test
  void addAffinity_capsAffinityAt100() {
    faction.setAffinity(90);
    when(factionRepository.findById(1L)).thenReturn(Optional.of(faction));
    when(factionRepository.saveAndFlush(faction)).thenReturn(faction);

    factionService.addAffinity(1L, 50);

    assertThat(faction.getAffinity()).isEqualTo(100);
    verify(factionRepository).saveAndFlush(faction);
  }

  @Test
  void addAffinity_factionNotFound_doesNothing() {
    when(factionRepository.findById(99L)).thenReturn(Optional.empty());

    factionService.addAffinity(99L, 10);

    verify(factionRepository, never()).saveAndFlush(any());
  }

  // ==================== getAffinityBetween ====================

  @Test
  void getAffinityBetween_sameFaction_returnsAffinityValue() {
    faction.setAffinity(60);
    when(factionRepository.findActiveFactionByMember(leader)).thenReturn(Optional.of(faction));

    Wrestler partner = Wrestler.builder().build();
    partner.setId(20L);
    partner.setName("Partner");
    when(factionRepository.findActiveFactionByMember(partner)).thenReturn(Optional.of(faction));

    int result = factionService.getAffinityBetween(leader, partner);

    assertThat(result).isEqualTo(60);
  }

  @Test
  void getAffinityBetween_differentFactions_returnsZero() {
    Faction otherFaction = Faction.builder().build();
    otherFaction.setId(2L);
    otherFaction.setName("Other Faction");

    Wrestler other = Wrestler.builder().build();
    other.setId(20L);
    other.setName("Other Wrestler");

    when(factionRepository.findActiveFactionByMember(leader)).thenReturn(Optional.of(faction));
    when(factionRepository.findActiveFactionByMember(other)).thenReturn(Optional.of(otherFaction));

    int result = factionService.getAffinityBetween(leader, other);

    assertThat(result).isZero();
  }

  @Test
  void getAffinityBetween_wrestlerNotInFaction_returnsZero() {
    Wrestler loner = Wrestler.builder().build();
    loner.setId(30L);
    loner.setName("Loner");

    when(factionRepository.findActiveFactionByMember(leader)).thenReturn(Optional.of(faction));
    when(factionRepository.findActiveFactionByMember(loner)).thenReturn(Optional.empty());

    int result = factionService.getAffinityBetween(leader, loner);

    assertThat(result).isZero();
  }

  @Test
  void getAffinityBetween_nullWrestler1_returnsZero() {
    int result = factionService.getAffinityBetween(null, leader);

    assertThat(result).isZero();
    verify(factionRepository, never()).findActiveFactionByMember(any());
  }

  @Test
  void getAffinityBetween_nullWrestler2_returnsZero() {
    int result = factionService.getAffinityBetween(leader, null);

    assertThat(result).isZero();
    verify(factionRepository, never()).findActiveFactionByMember(any());
  }
}

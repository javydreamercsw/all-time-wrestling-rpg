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
package com.github.javydreamercsw.management.service.universe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UniverseServiceTest {

  @Mock private UniverseRepository universeRepository;
  @Mock private ShowRepository showRepository;
  @Mock private FactionRepository factionRepository;
  @Mock private LeagueRepository leagueRepository;
  @Mock private TitleRepository titleRepository;
  @Mock private CampaignRepository campaignRepository;

  @InjectMocks private UniverseService universeService;

  private Universe universe;

  @BeforeEach
  void setUp() {
    universe = Universe.builder().name("Test Universe").type(Universe.UniverseType.GLOBAL).build();
    universe.setId(1L);
  }

  // -------------------------------------------------------------------------
  // findAll
  // -------------------------------------------------------------------------

  @Test
  void findAll_returnsAllUniverses() {
    when(universeRepository.findAll()).thenReturn(List.of(universe));

    List<Universe> result = universeService.findAll();

    assertThat(result).hasSize(1).containsExactly(universe);
  }

  @Test
  void findAll_emptyRepository_returnsEmptyList() {
    when(universeRepository.findAll()).thenReturn(Collections.emptyList());

    List<Universe> result = universeService.findAll();

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // findById
  // -------------------------------------------------------------------------

  @Test
  void findById_existingId_returnsUniverse() {
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));

    Optional<Universe> result = universeService.findById(1L);

    assertThat(result).isPresent().contains(universe);
  }

  @Test
  void findById_missingId_returnsEmpty() {
    when(universeRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Universe> result = universeService.findById(99L);

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // findByName
  // -------------------------------------------------------------------------

  @Test
  void findByName_existingName_returnsUniverse() {
    when(universeRepository.findByName("Test Universe")).thenReturn(Optional.of(universe));

    Optional<Universe> result = universeService.findByName("Test Universe");

    assertThat(result).isPresent().contains(universe);
  }

  @Test
  void findByName_unknownName_returnsEmpty() {
    when(universeRepository.findByName("Unknown")).thenReturn(Optional.empty());

    Optional<Universe> result = universeService.findByName("Unknown");

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // save
  // -------------------------------------------------------------------------

  @Test
  void save_newUniverse_persistsAndReturns() {
    when(universeRepository.findByName("Test Universe")).thenReturn(Optional.empty());
    when(universeRepository.save(universe)).thenReturn(universe);

    Universe result = universeService.save(universe);

    assertThat(result).isEqualTo(universe);
    verify(universeRepository).save(universe);
  }

  @Test
  void save_blankName_throwsIllegalArgument() {
    Universe blank = Universe.builder().name("   ").build();

    assertThatThrownBy(() -> universeService.save(blank))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name must not be blank");
  }

  @Test
  void save_nullName_throwsIllegalArgument() {
    Universe noName = Universe.builder().build();
    noName.setId(5L);

    assertThatThrownBy(() -> universeService.save(noName))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name must not be blank");
  }

  @Test
  void save_duplicateNameDifferentId_throwsIllegalArgument() {
    Universe other = Universe.builder().name("Test Universe").build();
    other.setId(99L);

    when(universeRepository.findByName("Test Universe")).thenReturn(Optional.of(other));

    assertThatThrownBy(() -> universeService.save(universe))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void save_sameIdUpdate_succeeds() {
    // Saving an existing universe whose name resolves to itself — should not throw.
    when(universeRepository.findByName("Test Universe")).thenReturn(Optional.of(universe));
    when(universeRepository.save(universe)).thenReturn(universe);

    Universe result = universeService.save(universe);

    assertThat(result).isEqualTo(universe);
  }

  // -------------------------------------------------------------------------
  // delete
  // -------------------------------------------------------------------------

  @Test
  void delete_cleanUniverse_deletesSuccessfully() {
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(showRepository.existsByUniverse(universe)).thenReturn(false);
    when(factionRepository.existsByUniverse(universe)).thenReturn(false);
    when(leagueRepository.existsByUniverse(universe)).thenReturn(false);
    when(titleRepository.existsByUniverse(universe)).thenReturn(false);
    when(campaignRepository.existsByUniverse(universe)).thenReturn(false);

    universeService.delete(1L);

    verify(universeRepository).delete(universe);
  }

  @Test
  void delete_unknownId_throwsIllegalArgument() {
    when(universeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> universeService.delete(99L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Universe not found");
  }

  @Test
  void delete_universeWithShows_throwsIllegalState() {
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(showRepository.existsByUniverse(universe)).thenReturn(true);

    assertThatThrownBy(() -> universeService.delete(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("shows");

    verify(universeRepository, never()).delete(any());
  }

  @Test
  void delete_universeWithFactions_throwsIllegalState() {
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(showRepository.existsByUniverse(universe)).thenReturn(false);
    when(factionRepository.existsByUniverse(universe)).thenReturn(true);

    assertThatThrownBy(() -> universeService.delete(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("factions");

    verify(universeRepository, never()).delete(any());
  }

  @Test
  void delete_universeWithLeagues_throwsIllegalState() {
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(showRepository.existsByUniverse(universe)).thenReturn(false);
    when(factionRepository.existsByUniverse(universe)).thenReturn(false);
    when(leagueRepository.existsByUniverse(universe)).thenReturn(true);

    assertThatThrownBy(() -> universeService.delete(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("leagues");

    verify(universeRepository, never()).delete(any());
  }

  @Test
  void delete_universeWithTitles_throwsIllegalState() {
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(showRepository.existsByUniverse(universe)).thenReturn(false);
    when(factionRepository.existsByUniverse(universe)).thenReturn(false);
    when(leagueRepository.existsByUniverse(universe)).thenReturn(false);
    when(titleRepository.existsByUniverse(universe)).thenReturn(true);

    assertThatThrownBy(() -> universeService.delete(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("titles");

    verify(universeRepository, never()).delete(any());
  }

  @Test
  void delete_universeWithCampaigns_throwsIllegalState() {
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));
    when(showRepository.existsByUniverse(universe)).thenReturn(false);
    when(factionRepository.existsByUniverse(universe)).thenReturn(false);
    when(leagueRepository.existsByUniverse(universe)).thenReturn(false);
    when(titleRepository.existsByUniverse(universe)).thenReturn(false);
    when(campaignRepository.existsByUniverse(universe)).thenReturn(true);

    assertThatThrownBy(() -> universeService.delete(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("campaigns");

    verify(universeRepository, never()).delete(any());
  }

  // -------------------------------------------------------------------------
  // getDeletionBlockers
  // -------------------------------------------------------------------------

  @Test
  void getDeletionBlockers_noBlockers_returnsEmptyList() {
    when(showRepository.existsByUniverse(universe)).thenReturn(false);
    when(factionRepository.existsByUniverse(universe)).thenReturn(false);
    when(leagueRepository.existsByUniverse(universe)).thenReturn(false);
    when(titleRepository.existsByUniverse(universe)).thenReturn(false);
    when(campaignRepository.existsByUniverse(universe)).thenReturn(false);

    List<String> blockers = universeService.getDeletionBlockers(universe);

    assertThat(blockers).isEmpty();
  }

  @Test
  void getDeletionBlockers_allBlockersPresent_returnsAllFive() {
    when(showRepository.existsByUniverse(universe)).thenReturn(true);
    when(factionRepository.existsByUniverse(universe)).thenReturn(true);
    when(leagueRepository.existsByUniverse(universe)).thenReturn(true);
    when(titleRepository.existsByUniverse(universe)).thenReturn(true);
    when(campaignRepository.existsByUniverse(universe)).thenReturn(true);

    List<String> blockers = universeService.getDeletionBlockers(universe);

    assertThat(blockers).containsExactly("Shows", "Factions", "Leagues", "Titles", "Campaigns");
  }

  @Test
  void getDeletionBlockers_onlyShows_returnsShows() {
    when(showRepository.existsByUniverse(universe)).thenReturn(true);
    when(factionRepository.existsByUniverse(universe)).thenReturn(false);
    when(leagueRepository.existsByUniverse(universe)).thenReturn(false);
    when(titleRepository.existsByUniverse(universe)).thenReturn(false);
    when(campaignRepository.existsByUniverse(universe)).thenReturn(false);

    List<String> blockers = universeService.getDeletionBlockers(universe);

    assertThat(blockers).containsExactly("Shows");
  }

  // -------------------------------------------------------------------------
  // count
  // -------------------------------------------------------------------------

  @Test
  void count_returnsRepositoryCount() {
    when(universeRepository.count()).thenReturn(7L);

    long result = universeService.count();

    assertThat(result).isEqualTo(7L);
  }

  @Test
  void count_emptyRepository_returnsZero() {
    when(universeRepository.count()).thenReturn(0L);

    long result = universeService.count();

    assertThat(result).isZero();
  }
}

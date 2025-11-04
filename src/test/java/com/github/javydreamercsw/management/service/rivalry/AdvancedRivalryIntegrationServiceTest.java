package com.github.javydreamercsw.management.service.rivalry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.storyline.StorylineBranchingService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test for Advanced Rivalry Integration Service. Tests faction rivalries, multi-wrestler
 * feuds, and storyline branching with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdvancedRivalryIntegrationService Tests")
class AdvancedRivalryIntegrationServiceTest {

  @Mock private RivalryService rivalryService;
  @Mock private FactionService factionService;
  @Mock private FactionRivalryService factionRivalryService;
  @Mock private MultiWrestlerFeudService multiWrestlerFeudService;
  @Mock private StorylineBranchingService storylineBranchingService;

  @InjectMocks private AdvancedRivalryIntegrationService advancedRivalryService;

  private Clock fixedClock;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;
  private Wrestler wrestler4;
  private Faction faction1;
  private Faction faction2;
  private Show testShow;
  private SegmentType singlesSegmentType;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    // Create test wrestlers
    wrestler1 = createWrestler("Stone Cold Steve Austin", 1L);
    wrestler2 = createWrestler("The Rock", 2L);
    wrestler3 = createWrestler("Triple H", 3L);
    wrestler4 = createWrestler("The Undertaker", 4L);

    // Create test factions
    faction1 = createFaction("The Corporation", 1L);
    faction2 = createFaction("D-Generation X", 2L);

    // Create test show and segment type
    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setShowDate(java.time.LocalDate.now());

    // Match types are loaded by DataInitializer, no need to create here
    singlesSegmentType = new SegmentType();
    singlesSegmentType.setName("Singles Match");
  }

  @Test
  void testCreateComplexStorylineWithTwoWrestlers() {
    // Given
    List<Long> wrestlerIds = List.of(wrestler1.getId(), wrestler2.getId());
    Rivalry mockRivalry = createRivalry(wrestler1, wrestler2, 10);

    when(rivalryService.createRivalry(wrestler1.getId(), wrestler2.getId(), "Epic rivalry"))
        .thenReturn(Optional.of(mockRivalry));

    // When
    AdvancedRivalryIntegrationService.ComplexStorylineResult result =
        advancedRivalryService.createComplexStoryline(
            "Austin vs Rock", wrestlerIds, "Epic rivalry");

    // Then
    assertThat(result.individualRivalry).isNotNull();
    assertThat(result.individualRivalry.getWrestler1()).isEqualTo(wrestler1);
    assertThat(result.individualRivalry.getWrestler2()).isEqualTo(wrestler2);
    assertThat(result.individualRivalry.getHeat()).isEqualTo(10);
    assertThat(result.individualRivalry.getIsActive()).isTrue();
  }

  @Test
  void testCreateComplexStorylineWithMultipleWrestlers() {
    // Given
    List<Long> wrestlerIds = List.of(wrestler1.getId(), wrestler2.getId(), wrestler3.getId());
    MultiWrestlerFeud mockFeud = createMultiWrestlerFeud("Triple Threat Feud", 1L);

    when(multiWrestlerFeudService.createFeud(anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(mockFeud));
    when(multiWrestlerFeudService.addParticipant(eq(1L), anyLong(), any()))
        .thenReturn(Optional.of(mockFeud));

    // When
    AdvancedRivalryIntegrationService.ComplexStorylineResult result =
        advancedRivalryService.createComplexStoryline(
            "Triple Threat Feud", wrestlerIds, "Three-way rivalry");

    // Then
    assertThat(result.multiWrestlerFeud).isNotNull();
    assertThat(result.multiWrestlerFeud.getName()).isEqualTo("Triple Threat Feud");
    assertThat(result.multiWrestlerFeud.getIsActive()).isTrue();

    // Verify that participants were added
    verify(multiWrestlerFeudService, times(3)).addParticipant(eq(1L), anyLong(), any());
  }

  @Test
  void testProcessSegmentOutcome() {
    // Given
    Segment segment = new Segment();
    segment.setShow(testShow);
    segment.setSegmentType(singlesSegmentType);
    segment.setSegmentDate(fixedClock.instant());
    segment.setIsTitleSegment(false);
    segment.setIsNpcGenerated(false);

    // Add participants
    segment.addParticipant(wrestler1);
    segment.addParticipant(wrestler2);
    segment.setWinners(java.util.List.of(wrestler1));

    // When
    advancedRivalryService.processSegmentOutcome(segment);

    // Then - Verify that the storyline branching service was called
    verify(storylineBranchingService).processSegmentOutcome(segment);
  }

  @Test
  void testGetWrestlerRivalryOverview() {
    // Given
    Long wrestlerId = wrestler1.getId();
    List<Rivalry> mockRivalries = List.of(createRivalry(wrestler1, wrestler2, 15));
    List<MultiWrestlerFeud> mockFeuds = List.of(createMultiWrestlerFeud("Test Feud", 1L));

    when(rivalryService.getRivalriesForWrestler(wrestlerId)).thenReturn(mockRivalries);
    when(factionService.getFactionForWrestler(wrestlerId)).thenReturn(Optional.empty());
    when(multiWrestlerFeudService.getActiveFeudsForWrestler(wrestlerId)).thenReturn(mockFeuds);
    when(storylineBranchingService.getActiveBranches()).thenReturn(List.of());

    // When
    AdvancedRivalryIntegrationService.WrestlerRivalryOverview overview =
        advancedRivalryService.getWrestlerRivalryOverview(wrestlerId);

    // Then
    assertThat(overview.individualRivalries).hasSize(1);
    assertThat(overview.multiWrestlerFeuds).hasSize(1);
    assertThat(overview.faction).isNull();
    assertThat(overview.factionRivalries).isNull();
    assertThat(overview.activeStorylineBranches).isEmpty();
  }

  // Helper methods for creating test objects
  private Wrestler createWrestler(String name, Long id) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(id);
    wrestler.setName(name);
    wrestler.setFans(50000L);
    wrestler.setStartingHealth(15);
    wrestler.setIsPlayer(true);
    return wrestler;
  }

  private Faction createFaction(String name, Long id) {
    Faction faction = Faction.builder().build();
    faction.setId(id);
    faction.setName(name);
    faction.setIsActive(true);
    faction.setFormedDate(fixedClock.instant());
    faction.setCreationDate(fixedClock.instant());
    return faction;
  }

  private Rivalry createRivalry(Wrestler wrestler1, Wrestler wrestler2, int heat) {
    Rivalry rivalry = new Rivalry();
    rivalry.setId(1L);
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(heat);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(fixedClock.instant());
    return rivalry;
  }

  private MultiWrestlerFeud createMultiWrestlerFeud(String name, Long id) {
    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setId(id);
    feud.setName(name);
    feud.setIsActive(true);
    feud.setHeat(0);
    feud.setStartedDate(fixedClock.instant());
    feud.setCreationDate(fixedClock.instant());
    return feud;
  }
}

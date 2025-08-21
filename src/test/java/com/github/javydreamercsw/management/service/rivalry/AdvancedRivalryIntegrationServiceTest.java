package com.github.javydreamercsw.management.service.rivalry;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionAlignment;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.MatchResult;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for Advanced Rivalry Features. Tests faction rivalries, multi-wrestler feuds,
 * and storyline branching.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdvancedRivalryIntegrationServiceTest {

  @Autowired private AdvancedRivalryIntegrationService advancedRivalryService;

  @Autowired private RivalryService rivalryService;

  @Autowired private WrestlerService wrestlerService;

  @Autowired private FactionService factionService;

  @Autowired private FactionRivalryService factionRivalryService;

  @Autowired private MultiWrestlerFeudService multiWrestlerFeudService;

  @Autowired private Clock clock;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;
  private Wrestler wrestler4;
  private Faction faction1;
  private Faction faction2;
  private Show testShow;
  private MatchType singlesMatchType;

  @BeforeEach
  void setUp() {
    // Create test wrestlers
    wrestler1 = wrestlerService.createAtwWrestler("Stone Cold Steve Austin", true, null, null);
    wrestler2 = wrestlerService.createAtwWrestler("The Rock", true, null, null);
    wrestler3 = wrestlerService.createAtwWrestler("Triple H", true, null, null);
    wrestler4 = wrestlerService.createAtwWrestler("The Undertaker", true, null, null);

    // Create test factions
    Optional<Faction> faction1Opt =
        factionService.createFaction(
            "The Corporation", "Corporate heel faction", FactionAlignment.HEEL, wrestler1.getId());
    Optional<Faction> faction2Opt =
        factionService.createFaction(
            "D-Generation X", "Rebellious face faction", FactionAlignment.FACE, wrestler3.getId());

    assertThat(faction1Opt).isPresent();
    assertThat(faction2Opt).isPresent();

    faction1 = faction1Opt.get();
    faction2 = faction2Opt.get();

    // Add members to factions
    factionService.addMemberToFaction(faction1.getId(), wrestler2.getId());
    factionService.addMemberToFaction(faction2.getId(), wrestler4.getId());

    // Create test show and match type
    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setShowDate(java.time.LocalDate.now());

    singlesMatchType = new MatchType();
    singlesMatchType.setName("Singles Match");
  }

  @Test
  void testCreateComplexStorylineWithTwoWrestlers() {
    // Test creating a storyline with 2 wrestlers (should create individual rivalry)
    List<Long> wrestlerIds = List.of(wrestler1.getId(), wrestler2.getId());

    AdvancedRivalryIntegrationService.ComplexStorylineResult result =
        advancedRivalryService.createComplexStoryline(
            "Austin vs Rock", wrestlerIds, "Epic rivalry");

    assertThat(result.individualRivalry).isNotNull();
    assertThat(result.individualRivalry.getWrestler1()).isEqualTo(wrestler1);
    assertThat(result.individualRivalry.getWrestler2()).isEqualTo(wrestler2);
    assertThat(result.individualRivalry.getHeat()).isEqualTo(0);
    assertThat(result.individualRivalry.getIsActive()).isTrue();
  }

  @Test
  void testCreateComplexStorylineWithMultipleWrestlers() {
    // Test creating a storyline with 3+ wrestlers (should create multi-wrestler feud)
    List<Long> wrestlerIds = List.of(wrestler1.getId(), wrestler2.getId(), wrestler3.getId());

    AdvancedRivalryIntegrationService.ComplexStorylineResult result =
        advancedRivalryService.createComplexStoryline(
            "Triple Threat Feud", wrestlerIds, "Three-way rivalry");

    assertThat(result.multiWrestlerFeud).isNotNull();
    assertThat(result.multiWrestlerFeud.getName()).isEqualTo("Triple Threat Feud");
    assertThat(result.multiWrestlerFeud.getActiveParticipantCount()).isEqualTo(3);
    assertThat(result.multiWrestlerFeud.getIsActive()).isTrue();
  }

  @Test
  void testCreateComplexStorylineWithFactionMembers() {
    // Test creating a storyline with wrestlers from different factions
    List<Long> wrestlerIds =
        List.of(wrestler1.getId(), wrestler3.getId()); // Leaders of opposing factions

    AdvancedRivalryIntegrationService.ComplexStorylineResult result =
        advancedRivalryService.createComplexStoryline(
            "Faction War", wrestlerIds, "Corporation vs DX");

    assertThat(result.individualRivalry).isNotNull();
    assertThat(result.factionRivalry).isNotNull();
    assertThat(result.factionRivalry.getFaction1()).isEqualTo(faction1);
    assertThat(result.factionRivalry.getFaction2()).isEqualTo(faction2);
  }

  @Test
  void testProcessMatchOutcomeCreatesHeat() {
    // Create a match result
    MatchResult matchResult = new MatchResult();
    matchResult.setShow(testShow);
    matchResult.setMatchType(singlesMatchType);
    matchResult.setWinner(wrestler1);
    matchResult.setMatchDate(clock.instant());
    matchResult.setIsTitleMatch(false);
    matchResult.setIsNpcGenerated(false);

    // Add participants
    matchResult.addParticipant(wrestler1, true);
    matchResult.addParticipant(wrestler2, false);

    // Process the match outcome
    advancedRivalryService.processMatchOutcome(matchResult);

    // Verify individual rivalry was created/updated
    List<Rivalry> wrestler1Rivalries = rivalryService.getRivalriesForWrestler(wrestler1.getId());
    assertThat(wrestler1Rivalries).hasSize(1);

    Rivalry rivalry = wrestler1Rivalries.get(0);
    assertThat(rivalry.getHeat()).isGreaterThan(0);
    assertThat(rivalry.involvesWrestler(wrestler1)).isTrue();
    assertThat(rivalry.involvesWrestler(wrestler2)).isTrue();
  }

  @Test
  void testProcessMatchOutcomeCreatesFactionHeat() {
    // Create a match result between faction members
    MatchResult matchResult = new MatchResult();
    matchResult.setShow(testShow);
    matchResult.setMatchType(singlesMatchType);
    matchResult.setWinner(wrestler1); // Corporation member
    matchResult.setMatchDate(clock.instant());
    matchResult.setIsTitleMatch(false);
    matchResult.setIsNpcGenerated(false);

    // Add participants from different factions
    matchResult.addParticipant(wrestler1, true); // Corporation
    matchResult.addParticipant(wrestler3, false); // DX

    // Process the match outcome
    advancedRivalryService.processMatchOutcome(matchResult);

    // Verify faction rivalry was created/updated
    List<FactionRivalry> faction1Rivalries =
        factionRivalryService.getActiveRivalriesForFaction(faction1.getId());

    assertThat(faction1Rivalries).hasSize(1);

    FactionRivalry factionRivalry = faction1Rivalries.get(0);
    assertThat(factionRivalry.getHeat()).isGreaterThan(0);
    assertThat(factionRivalry.involvesFaction(faction1)).isTrue();
    assertThat(factionRivalry.involvesFaction(faction2)).isTrue();
  }

  @Test
  void testEscalateRivalryToFactionRivalry() {
    // Create an individual rivalry first
    Optional<Rivalry> rivalryOpt =
        rivalryService.createRivalry(wrestler1.getId(), wrestler3.getId(), "Test rivalry");
    assertThat(rivalryOpt).isPresent();

    Rivalry rivalry = rivalryOpt.get();

    // Add some heat to make it eligible for escalation
    rivalryService.addHeat(rivalry.getId(), 15, "Building tension");

    // Escalate the rivalry
    AdvancedRivalryIntegrationService.RivalryEscalationResult result =
        advancedRivalryService.escalateRivalry(rivalry.getId(), "Faction involvement");

    assertThat(result.escalated).isTrue();
    assertThat(result.escalationType).isEqualTo("FACTION_RIVALRY");
    assertThat(result.factionRivalry).isNotNull();
    assertThat(result.factionRivalry.involvesFaction(faction1)).isTrue();
    assertThat(result.factionRivalry.involvesFaction(faction2)).isTrue();
  }

  @Test
  void testGetWrestlerRivalryOverview() {
    // Create various rivalries for wrestler1
    rivalryService.createRivalry(wrestler1.getId(), wrestler2.getId(), "Individual rivalry");

    // Create a multi-wrestler feud
    Optional<MultiWrestlerFeud> feudOpt =
        multiWrestlerFeudService.createFeud("Test Feud", "Test description", "Test notes");
    assertThat(feudOpt).isPresent();

    MultiWrestlerFeud feud = feudOpt.get();
    multiWrestlerFeudService.addParticipant(
        feud.getId(),
        wrestler1.getId(),
        com.github.javydreamercsw.management.domain.feud.FeudRole.ANTAGONIST);

    // Get overview
    AdvancedRivalryIntegrationService.WrestlerRivalryOverview overview =
        advancedRivalryService.getWrestlerRivalryOverview(wrestler1.getId());

    assertThat(overview.individualRivalries).hasSize(1);
    assertThat(overview.faction).isEqualTo(faction1);
    assertThat(overview.multiWrestlerFeuds).hasSize(1);
    assertThat(overview.multiWrestlerFeuds.get(0)).isEqualTo(feud);
  }

  @Test
  void testFactionAlignmentHeatMultiplier() {
    // Create a match between Face and Heel faction members
    MatchResult matchResult = new MatchResult();
    matchResult.setShow(testShow);
    matchResult.setMatchType(singlesMatchType);
    matchResult.setWinner(wrestler1); // Heel faction member
    matchResult.setMatchDate(clock.instant());
    matchResult.setIsTitleMatch(false);
    matchResult.setIsNpcGenerated(false);

    matchResult.addParticipant(wrestler1, true); // Corporation (Heel)
    matchResult.addParticipant(wrestler3, false); // DX (Face)

    // Process the match outcome
    advancedRivalryService.processMatchOutcome(matchResult);

    // Verify faction rivalry has appropriate heat (Face vs Heel should have multiplier)
    List<FactionRivalry> rivalries = factionRivalryService.getActiveFactionRivalries();

    assertThat(rivalries).hasSize(1);

    FactionRivalry rivalry = rivalries.get(0);
    // Face vs Heel should generate more heat than normal
    assertThat(rivalry.getAlignmentHeatMultiplier()).isEqualTo(1.5);
  }

  @Test
  void testMultiWrestlerFeudHeatGeneration() {
    // Create a multi-wrestler feud
    Optional<MultiWrestlerFeud> feudOpt =
        multiWrestlerFeudService.createFeud(
            "Four-Way Feud", "Epic four-way rivalry", "Test storyline");
    assertThat(feudOpt).isPresent();

    MultiWrestlerFeud feud = feudOpt.get();

    // Add all wrestlers to the feud
    multiWrestlerFeudService.addParticipant(
        feud.getId(),
        wrestler1.getId(),
        com.github.javydreamercsw.management.domain.feud.FeudRole.ANTAGONIST);
    multiWrestlerFeudService.addParticipant(
        feud.getId(),
        wrestler2.getId(),
        com.github.javydreamercsw.management.domain.feud.FeudRole.PROTAGONIST);
    multiWrestlerFeudService.addParticipant(
        feud.getId(),
        wrestler3.getId(),
        com.github.javydreamercsw.management.domain.feud.FeudRole.SECONDARY_ANTAGONIST);
    multiWrestlerFeudService.addParticipant(
        feud.getId(),
        wrestler4.getId(),
        com.github.javydreamercsw.management.domain.feud.FeudRole.NEUTRAL);

    // Create a match with multiple feud participants
    MatchResult matchResult = new MatchResult();
    matchResult.setShow(testShow);
    matchResult.setMatchType(singlesMatchType);
    matchResult.setWinner(wrestler1);
    matchResult.setMatchDate(clock.instant());
    matchResult.setIsTitleMatch(false);
    matchResult.setIsNpcGenerated(false);

    matchResult.addParticipant(wrestler1, true);
    matchResult.addParticipant(wrestler2, false);
    matchResult.addParticipant(wrestler3, false);

    // Process the match outcome
    advancedRivalryService.processMatchOutcome(matchResult);

    // Verify feud heat increased
    Optional<MultiWrestlerFeud> updatedFeudOpt = multiWrestlerFeudService.getFeudById(feud.getId());
    assertThat(updatedFeudOpt).isPresent();

    MultiWrestlerFeud updatedFeud = updatedFeudOpt.get();
    assertThat(updatedFeud.getHeat()).isGreaterThan(0);
  }
}

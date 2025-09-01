package com.github.javydreamercsw.base.ai.mock;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.ai.MatchNarrationService.MatchNarrationContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.MatchTypeContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.Move;
import com.github.javydreamercsw.base.ai.MatchNarrationService.MoveSet;
import com.github.javydreamercsw.base.ai.MatchNarrationService.NPCContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.RefereeContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.VenueContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.WrestlerContext;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for MockMatchNarrationService. Tests the mock AI provider's ability to generate
 * realistic wrestling match narrations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Mock Match Narration Service Tests")
class MockMatchNarrationServiceTest {

  private MockMatchNarrationService mockService;
  private MatchNarrationContext testContext;

  @BeforeEach
  void setUp() {
    mockService = new MockMatchNarrationService();
    testContext = createTestMatchContext();
  }

  @Test
  @DisplayName("Should always be available")
  void shouldAlwaysBeAvailable() {
    assertThat(mockService.isAvailable()).isTrue();
  }

  @Test
  @DisplayName("Should return correct provider name")
  void shouldReturnCorrectProviderName() {
    assertThat(mockService.getProviderName()).isEqualTo("Mock AI");
  }

  @Test
  @DisplayName("Should generate non-empty match narration")
  void shouldGenerateNonEmptyMatchNarration() {
    String narration = mockService.narrateMatch(testContext);

    assertThat(narration)
        .isNotNull()
        .isNotEmpty()
        .hasSizeGreaterThan(100); // Should be substantial content
  }

  @Test
  @DisplayName("Should include wrestler names in narration")
  void shouldIncludeWrestlerNamesInNarration() {
    String narration = mockService.narrateMatch(testContext);

    assertThat(narration).contains("Stone Cold Steve Austin").contains("The Rock");
  }

  @Test
  @DisplayName("Should include venue information in narration")
  void shouldIncludeVenueInformationInNarration() {
    String narration = mockService.narrateMatch(testContext);

    assertThat(narration).containsIgnoringCase("WrestleMania");
  }

  @Test
  @DisplayName("Should include match type in narration")
  void shouldIncludeMatchTypeInNarration() {
    String narration = mockService.narrateMatch(testContext);

    assertThat(narration).containsIgnoringCase("Singles Match");
  }

  @Test
  @DisplayName("Should generate different narrations for different contexts")
  void shouldGenerateDifferentNarrationsForDifferentContexts() {
    MatchNarrationContext context1 = createTestMatchContext();
    MatchNarrationContext context2 = createAlternativeMatchContext();

    String narration1 = mockService.narrateMatch(context1);
    String narration2 = mockService.narrateMatch(context2);

    assertThat(narration1).isNotEqualTo(narration2);
    assertThat(narration2).contains("Undertaker").contains("Kane");
  }

  @Test
  @DisplayName("Should handle minimal context gracefully")
  void shouldHandleMinimalContextGracefully() {
    MatchNarrationContext minimalContext = new MatchNarrationContext();

    // Set only required fields
    MatchTypeContext matchType = new MatchTypeContext();
    matchType.setMatchType("Singles Match");
    minimalContext.setMatchType(matchType);

    WrestlerContext wrestler1 = new WrestlerContext();
    wrestler1.setName("Wrestler A");
    WrestlerContext wrestler2 = new WrestlerContext();
    wrestler2.setName("Wrestler B");
    minimalContext.setWrestlers(Arrays.asList(wrestler1, wrestler2));

    String narration = mockService.narrateMatch(minimalContext);

    assertThat(narration).isNotNull().isNotEmpty().contains("Wrestler A").contains("Wrestler B");
  }

  @Test
  @DisplayName("Should simulate processing time")
  void shouldSimulateProcessingTime() {
    long startTime = System.currentTimeMillis();

    mockService.narrateMatch(testContext);

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // Should take at least 1 second (simulated processing time)
    assertThat(duration).isGreaterThanOrEqualTo(1000);
  }

  @Test
  @DisplayName("Should generate structured narration with multiple sections")
  void shouldGenerateStructuredNarrationWithMultipleSections() {
    String narration = mockService.narrateMatch(testContext);

    // Should contain multiple paragraphs/sections
    String[] sections = narration.split("\n\n");
    assertThat(sections).hasSizeGreaterThanOrEqualTo(3);
  }

  /** Creates a comprehensive test match context. */
  private MatchNarrationContext createTestMatchContext() {
    MatchNarrationContext context = new MatchNarrationContext();

    // Match Type
    MatchTypeContext matchType = new MatchTypeContext();
    matchType.setMatchType("Singles Match");
    matchType.setStipulation("WWE Championship");
    matchType.setRules(Arrays.asList("Standard Rules"));
    matchType.setTimeLimit(30);
    context.setMatchType(matchType);

    // Venue
    VenueContext venue = new VenueContext();
    venue.setName("WrestleMania");
    venue.setLocation("MetLife Stadium, New Jersey");
    venue.setType("Stadium");
    venue.setCapacity(82500);
    venue.setDescription("The Grandest Stage of Them All");
    venue.setAtmosphere("Electric WrestleMania atmosphere");
    venue.setSignificance("The biggest event in sports entertainment");
    context.setVenue(venue);

    // Wrestlers
    WrestlerContext austin = new WrestlerContext();
    austin.setName("Stone Cold Steve Austin");
    austin.setDescription("Texas Rattlesnake - Anti-hero with beer-drinking attitude");

    MoveSet austinMoves = new MoveSet();
    austinMoves.setFinishers(
        Arrays.asList(new Move("Stone Cold Stunner", "Jaw-dropping finishing move", "finisher")));
    austinMoves.setTrademarks(
        Arrays.asList(new Move("Lou Thesz Press", "Explosive takedown with punches", "trademark")));
    austin.setMoveSet(austinMoves);
    austin.setFeudsAndHeat(Arrays.asList("Austin vs McMahon"));

    WrestlerContext rock = new WrestlerContext();
    rock.setName("The Rock");
    rock.setDescription("The People's Champion - Charismatic superstar");

    MoveSet rockMoves = new MoveSet();
    rockMoves.setFinishers(
        Arrays.asList(
            new Move("Rock Bottom", "Devastating slam", "finisher"),
            new Move("People's Elbow", "Electrifying elbow drop", "finisher")));
    rock.setMoveSet(rockMoves);
    rock.setFeudsAndHeat(Arrays.asList("Corporate Champion"));

    context.setWrestlers(Arrays.asList(austin, rock));

    // Referee
    RefereeContext referee = new RefereeContext();
    referee.setName("Earl Hebner");
    referee.setDescription("Veteran referee");
    referee.setPersonality("Fair but can be influenced");
    context.setReferee(referee);

    // NPCs
    NPCContext announcer = new NPCContext();
    announcer.setName("Howard Finkel");
    announcer.setRole("Ring Announcer");
    announcer.setDescription("The Fink - Legendary announcer");
    announcer.setPersonality("Professional and enthusiastic");

    NPCContext commentator = new NPCContext();
    commentator.setName("Jim Ross");
    commentator.setRole("Play-by-Play Commentator");
    commentator.setDescription("Good ol' JR");
    commentator.setPersonality("Passionate and iconic");

    context.setNpcs(Arrays.asList(announcer, commentator));

    // Context
    context.setAudience("82,500 screaming fans");
    context.setDeterminedOutcome("Stone Cold Steve Austin wins via Stone Cold Stunner");
    context.setRecentMatchNarrations(Arrays.asList("Previous epic encounter..."));

    return context;
  }

  /** Creates an alternative match context for comparison testing. */
  private MatchNarrationContext createAlternativeMatchContext() {
    MatchNarrationContext context = new MatchNarrationContext();

    // Match Type
    MatchTypeContext matchType = new MatchTypeContext();
    matchType.setMatchType("Hell in a Cell");
    matchType.setStipulation("Brothers of Destruction");
    matchType.setRules(Arrays.asList("No Escape", "Hardcore Rules"));
    context.setMatchType(matchType);

    // Venue
    VenueContext venue = new VenueContext();
    venue.setName("Badd Blood");
    venue.setLocation("St. Louis, Missouri");
    context.setVenue(venue);

    // Wrestlers
    WrestlerContext undertaker = new WrestlerContext();
    undertaker.setName("The Undertaker");
    undertaker.setDescription("The Deadman");

    WrestlerContext kane = new WrestlerContext();
    kane.setName("Kane");
    kane.setDescription("The Big Red Machine");

    context.setWrestlers(Arrays.asList(undertaker, kane));
    context.setAudience("Shocked crowd");
    context.setDeterminedOutcome("Kane debuts and destroys The Undertaker");

    return context;
  }
}

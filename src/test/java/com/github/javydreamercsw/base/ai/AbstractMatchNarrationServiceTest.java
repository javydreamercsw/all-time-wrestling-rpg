package com.github.javydreamercsw.base.ai;

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

/**
 * Unit tests for AbstractMatchNarrationService. Tests the common prompt building logic that all AI
 * providers use.
 */
@DisplayName("Abstract Match Narration Service Tests")
class AbstractMatchNarrationServiceTest {

  private TestableMatchNarrationService service;
  private MatchNarrationContext testContext;

  @BeforeEach
  void setUp() {
    service = new TestableMatchNarrationService();
    testContext = createComprehensiveTestContext();
  }

  @Test
  @DisplayName("Should build comprehensive prompt with all context elements")
  void shouldBuildComprehensivePromptWithAllContextElements() {
    String prompt = service.buildTestPrompt(testContext);

    assertThat(prompt)
        .isNotNull()
        .isNotEmpty()
        .contains("professional wrestling play-by-play commentator")
        .contains("MATCH SETUP:")
        .contains("WRESTLERS:")
        .contains("REFEREE:")
        .contains("SUPPORTING CHARACTERS:")
        .contains("PREDETERMINED OUTCOME:")
        .contains("NARRATION INSTRUCTIONS:");
  }

  @Test
  @DisplayName("Should include match type and stipulation in prompt")
  void shouldIncludeMatchTypeAndStipulationInPrompt() {
    String prompt = service.buildTestPrompt(testContext);

    assertThat(prompt)
        .contains("Match Type: Championship Match")
        .contains("Stipulation: World Heavyweight Championship")
        .contains("Special Rules: No DQ, Falls Count Anywhere")
        .contains("Time Limit: 60 minutes");
  }

  @Test
  @DisplayName("Should include detailed venue information in prompt")
  void shouldIncludeDetailedVenueInformationInPrompt() {
    String prompt = service.buildTestPrompt(testContext);

    assertThat(prompt)
        .contains("Venue: Madison Square Garden")
        .contains("Location: New York City, New York")
        .contains("Type: Indoor Arena")
        .contains("Capacity: 20000")
        .contains("Description: The World's Most Famous Arena")
        .contains("Atmosphere: Electric and historic")
        .contains("Significance: The Mecca of professional wrestling")
        .contains("Notable Matches: Hulk Hogan vs Andre the Giant");
  }

  @Test
  @DisplayName("Should include wrestler details with movesets in prompt")
  void shouldIncludeWrestlerDetailsWithMovesetsInPrompt() {
    String prompt = service.buildTestPrompt(testContext);

    assertThat(prompt)
        .contains("- John Cena:")
        .contains("Description: The Leader of Cenation")
        .contains("Finishers: Attitude Adjustment (Fireman's carry slam)")
        .contains("Trademark Moves: Five Knuckle Shuffle (Theatrical fist drop)")
        .contains("Current Feuds/Heat: Face of the company")
        .contains("Recent Match History: Defeated Randy Orton");
  }

  @Test
  @DisplayName("Should include referee personality in prompt")
  void shouldIncludeRefereePersonalityInPrompt() {
    String prompt = service.buildTestPrompt(testContext);

    assertThat(prompt)
        .contains("REFEREE:")
        .contains("- Charles Robinson:")
        .contains("Description: Veteran WWE referee")
        .contains("Personality: Fair and professional");
  }

  @Test
  @DisplayName("Should include NPC characters in prompt")
  void shouldIncludeNPCCharactersInPrompt() {
    String prompt = service.buildTestPrompt(testContext);

    assertThat(prompt)
        .contains("SUPPORTING CHARACTERS:")
        .contains("- Michael Cole (Play-by-Play Commentator):")
        .contains("Description: Lead WWE announcer")
        .contains("Personality: Energetic and professional");
  }

  @Test
  @DisplayName("Should include predetermined outcome in prompt")
  void shouldIncludePredeterminedOutcomeInPrompt() {
    String prompt = service.buildTestPrompt(testContext);

    assertThat(prompt)
        .contains("PREDETERMINED OUTCOME:")
        .contains("John Cena wins via Attitude Adjustment");
  }

  @Test
  @DisplayName("Should include recent match context for continuity")
  void shouldIncludeRecentMatchContextForContinuity() {
    String prompt = service.buildTestPrompt(testContext);

    assertThat(prompt)
        .contains("RECENT MATCH CONTEXT")
        .contains("Recent Match 1: Previous epic encounter");
  }

  @Test
  @DisplayName("Should include comprehensive narration instructions")
  void shouldIncludeComprehensiveNarrationInstructions() {
    String prompt = service.buildTestPrompt(testContext);

    assertThat(prompt)
        .contains("NARRATION INSTRUCTIONS:")
        .contains("1. Create a compelling 3-act structure")
        .contains("2. Use the wrestlers' signature moves")
        .contains("3. Include realistic crowd reactions")
        .contains("4. Incorporate the referee's personality")
        .contains("5. Reference the feuds/heat")
        .contains("10. Create a detailed, comprehensive match narration of 1500-2500 words")
        .contains("Begin the match narration now:");
  }

  @Test
  @DisplayName("Should handle minimal context gracefully")
  void shouldHandleMinimalContextGracefully() {
    MatchNarrationContext minimalContext = new MatchNarrationContext();

    // Set only required fields
    MatchTypeContext matchType = new MatchTypeContext();
    matchType.setMatchType("Singles Match");
    matchType.setStipulation("Regular Match");
    matchType.setRules(Arrays.asList());
    minimalContext.setMatchType(matchType);

    WrestlerContext wrestler1 = new WrestlerContext();
    wrestler1.setName("Wrestler A");
    WrestlerContext wrestler2 = new WrestlerContext();
    wrestler2.setName("Wrestler B");
    minimalContext.setWrestlers(Arrays.asList(wrestler1, wrestler2));

    minimalContext.setDeterminedOutcome("Wrestler A wins");
    minimalContext.setAudience("Crowd");

    String prompt = service.buildTestPrompt(minimalContext);

    assertThat(prompt)
        .isNotNull()
        .isNotEmpty()
        .contains("Wrestler A")
        .contains("Wrestler B")
        .contains("Singles Match");
  }

  /** Creates a comprehensive test context with all possible fields populated. */
  private MatchNarrationContext createComprehensiveTestContext() {
    MatchNarrationContext context = new MatchNarrationContext();

    // Match Type
    MatchTypeContext matchType = new MatchTypeContext();
    matchType.setMatchType("Championship Match");
    matchType.setStipulation("World Heavyweight Championship");
    matchType.setRules(Arrays.asList("No DQ", "Falls Count Anywhere"));
    matchType.setTimeLimit(60);
    context.setMatchType(matchType);

    // Venue
    VenueContext venue = new VenueContext();
    venue.setName("Madison Square Garden");
    venue.setLocation("New York City, New York");
    venue.setType("Indoor Arena");
    venue.setCapacity(20000);
    venue.setDescription("The World's Most Famous Arena");
    venue.setAtmosphere("Electric and historic");
    venue.setSignificance("The Mecca of professional wrestling");
    venue.setNotableMatches(
        Arrays.asList("Hulk Hogan vs Andre the Giant", "Shawn Michaels vs Razor Ramon"));
    context.setVenue(venue);

    // Wrestlers
    WrestlerContext cena = new WrestlerContext();
    cena.setName("John Cena");
    cena.setDescription("The Leader of Cenation - Never Give Up");

    MoveSet cenaMoves = new MoveSet();
    cenaMoves.setFinishers(
        Arrays.asList(
            new Move("Attitude Adjustment", "Fireman's carry slam", "finisher"),
            new Move("STF/STFU", "Submission hold", "finisher")));
    cenaMoves.setTrademarks(
        Arrays.asList(new Move("Five Knuckle Shuffle", "Theatrical fist drop", "trademark")));
    cena.setMoveSet(cenaMoves);
    cena.setFeudsAndHeat(Arrays.asList("Face of the company", "Polarizing figure"));
    cena.setRecentMatches(Arrays.asList("Defeated Randy Orton", "Beat Edge for title"));

    WrestlerContext edge = new WrestlerContext();
    edge.setName("Edge");
    edge.setDescription("The Rated R Superstar - Ultimate Opportunist");
    context.setWrestlers(Arrays.asList(cena, edge));

    // Referee
    RefereeContext referee = new RefereeContext();
    referee.setName("Charles Robinson");
    referee.setDescription("Veteran WWE referee known for his athleticism");
    referee.setPersonality("Fair and professional");
    context.setReferee(referee);

    // NPCs
    NPCContext announcer = new NPCContext();
    announcer.setName("Lilian Garcia");
    announcer.setRole("Ring Announcer");
    announcer.setDescription("Veteran WWE ring announcer");
    announcer.setPersonality("Professional and clear");

    NPCContext commentator = new NPCContext();
    commentator.setName("Michael Cole");
    commentator.setRole("Play-by-Play Commentator");
    commentator.setDescription("Lead WWE announcer");
    commentator.setPersonality("Energetic and professional");

    context.setNpcs(Arrays.asList(announcer, commentator));

    // Context
    context.setAudience("Sold-out crowd of 20,000 passionate WWE fans");
    context.setDeterminedOutcome(
        "John Cena wins via Attitude Adjustment to retain the World Heavyweight Championship");
    context.setRecentMatchNarrations(
        Arrays.asList("Previous epic encounter between these two rivals..."));

    return context;
  }

  /** Testable implementation of AbstractMatchNarrationService for testing prompt building. */
  private static class TestableMatchNarrationService extends AbstractMatchNarrationService {

    @Override
    protected String callAIProvider(String prompt) {
      return "Test narration response";
    }

    @Override
    public String getProviderName() {
      return "Test Provider";
    }

    @Override
    public boolean isAvailable() {
      return true;
    }

    // Expose the protected method for testing
    public String buildTestPrompt(MatchNarrationContext context) {
      return buildMatchNarrationPrompt(context);
    }
  }
}

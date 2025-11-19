package com.github.javydreamercsw.base.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.Move;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.MoveSet;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.NPCContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.RefereeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.VenueContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;
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
  private SegmentNarrationContext testContext;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    service = new TestableMatchNarrationService();
    testContext = createComprehensiveTestContext();
  }

  @Test
  @DisplayName("Should build comprehensive prompt with all context elements")
  void shouldBuildComprehensivePromptWithAllContextElements() throws JsonProcessingException {
    String prompt = service.buildTestPrompt(testContext);
    String jsonContext =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testContext);

    assertThat(prompt)
        .isNotNull()
        .isNotEmpty()
        .contains("You are a professional wrestling commentator and storyteller.")
        .contains("You will be provided with a context object in JSON format.")
        .contains("Generate a compelling wrestling narration based on the data in the JSON object.")
        .contains("The JSON object contains instructions that you must follow.")
        .contains("Here is the JSON context:")
        .contains(jsonContext);
  }

  @Test
  @DisplayName("Should handle minimal context gracefully")
  void shouldHandleMinimalContextGracefully() throws JsonProcessingException {
    SegmentNarrationContext minimalContext = new SegmentNarrationContext();

    // Set only required fields
    SegmentTypeContext matchType = new SegmentTypeContext();
    matchType.setSegmentType("Singles Match");
    matchType.setStipulation("Regular Match");
    matchType.setRules(List.of());
    minimalContext.setSegmentType(matchType);

    WrestlerContext wrestler1 = new WrestlerContext();
    wrestler1.setName("Wrestler A");
    WrestlerContext wrestler2 = new WrestlerContext();
    wrestler2.setName("Wrestler B");
    minimalContext.setWrestlers(Arrays.asList(wrestler1, wrestler2));

    minimalContext.setDeterminedOutcome("Wrestler A wins");
    minimalContext.setAudience("Crowd");

    String prompt = service.buildTestPrompt(minimalContext);
    String jsonContext =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(minimalContext);

    assertThat(prompt).isNotNull().isNotEmpty().contains(jsonContext);
  }

  /** Creates a comprehensive test context with all possible fields populated. */
  private SegmentNarrationContext createComprehensiveTestContext() {
    SegmentNarrationContext context = new SegmentNarrationContext();

    // Match Type
    SegmentTypeContext matchType = new SegmentTypeContext();
    matchType.setSegmentType("Championship Match");
    matchType.setStipulation("World Heavyweight Championship");
    matchType.setRules(Arrays.asList("No DQ", "Falls Count Anywhere"));
    matchType.setTimeLimit(60);
    context.setSegmentType(matchType);

    // Venue
    VenueContext venue = new VenueContext();
    venue.setName("Madison Square Garden");
    venue.setLocation("New York City, New York");
    venue.setType("Indoor Arena");
    venue.setCapacity(20000);
    venue.setDescription("The World's Most Famous Arena");
    venue.setAtmosphere("Electric and historic");
    venue.setSignificance("The Mecca of professional wrestling");
    venue.setNotableSegments(
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
        List.of(new Move("Five Knuckle Shuffle", "Theatrical fist drop", "trademark")));
    cena.setMoveSet(cenaMoves);
    cena.setFeudsAndHeat(Arrays.asList("Face of the company", "Polarizing figure"));
    cena.setRecentSegments(Arrays.asList("Defeated Randy Orton", "Beat Edge for title"));

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
    context.setRecentSegmentNarrations(
        List.of("Previous epic encounter between these two rivals..."));

    return context;
  }

  /** Testable implementation of AbstractMatchNarrationService for testing prompt building. */
  private static class TestableMatchNarrationService extends AbstractSegmentNarrationService {

    @Override
    protected String callAIProvider(@NonNull String prompt) {
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

    @Override
    public String generateText(@NonNull String prompt) {
      return callAIProvider(prompt);
    }

    // Expose the protected method for testing
    public String buildTestPrompt(SegmentNarrationContext context) {
      return buildSegmentNarrationPrompt(context);
    }
  }
}

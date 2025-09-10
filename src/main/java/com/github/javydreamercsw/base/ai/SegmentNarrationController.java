package com.github.javydreamercsw.base.ai;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.Move;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.MoveSet;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.NPCContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.RefereeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.VenueContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import com.github.javydreamercsw.base.service.segment.SegmentOutcomeProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AI-powered wrestling segment narration. Provides endpoints for generating
 * detailed segment stories using multiple AI providers including Google Gemini, OpenAI GPT, and
 * Anthropic Claude.
 */
@RestController
@RequestMapping("/api/segment-narration")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "AI Services",
    description = "AI-powered features including segment narration and story generation")
public class SegmentNarrationController {

  private final SegmentNarrationServiceFactory serviceFactory;
  private final SegmentNarrationConfig config;
  private final Environment environment;
  private final SegmentOutcomeProvider segmentOutcomeService;

  /**
   * Gets the appropriate service based on the current environment. In test profile, always use the
   * testing service to avoid costs.
   */
  private SegmentNarrationService getAppropriateService() {
    if (isTestProfile()) {
      log.debug("Test profile detected, using testing service");
      return serviceFactory.getTestingService();
    }
    return serviceFactory.getBestAvailableService();
  }

  /** Checks if the test profile is active. */
  private boolean isTestProfile() {
    return environment.matchesProfiles("test");
  }

  @Operation(
      summary = "Get AI provider limits and configuration",
      description =
          "Returns current AI provider information, rate limits, and configuration settings")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Provider information retrieved successfully"),
        @ApiResponse(responseCode = "503", description = "No AI providers available")
      })
  @GetMapping("/limits")
  public ResponseEntity<Map<String, Object>> getLimits() {
    SegmentNarrationService currentService = getAppropriateService();

    return ResponseEntity.ok(
        Map.of(
            "currentProvider",
            currentService != null ? currentService.getProviderName() : "None Available",
            "available",
            currentService != null,
            "availableServices",
            serviceFactory.getAvailableServices(),
            "currentConfig",
            Map.of(
                "maxOutputTokens",
                config.getAi().getMaxOutputTokens(),
                "temperature",
                config.getAi().getTemperature(),
                "timeoutSeconds",
                config.getAi().getTimeoutSeconds(),
                "targetWordCount",
                Map.of(
                    "minimum",
                    config.getAi().getWordCount().getMinimum(),
                    "maximum",
                    config.getAi().getWordCount().getMaximum(),
                    "target",
                    config.getAi().getWordCount().getTarget())),
            "providerLimits",
            Arrays.stream(SegmentNarrationConfig.ProviderLimits.values())
                .map(
                    limit ->
                        Map.of(
                            "provider",
                            limit.getDisplayName(),
                            "requestsPerMinute",
                            limit.getRequestsPerMinute(),
                            "requestsPerDay",
                            limit.getRequestsPerDay(),
                            "maxOutputTokens",
                            limit.getMaxOutputTokens(),
                            "maxInputTokens",
                            limit.getMaxInputTokens(),
                            "estimatedMaxWords",
                            (int) (limit.getMaxOutputTokens() * 0.75) // rough estimate
                            ))
                .toList()));
  }

  @Operation(
      summary = "Generate AI-powered segment narration",
      description =
          """
          Generates a detailed wrestling segment narration using AI based on the provided context.

          The system automatically selects the best available AI provider (Google Gemini, OpenAI GPT, or Anthropic Claude)
          based on availability, cost, and quality. The narration includes detailed play-by-play action,
          crowd reactions, commentary, and dramatic moments.

          **Features:**
          - Multiple AI provider support with automatic fallback
          - Customizable segment context (wrestlers, venue, outcome)
          - **Automatic outcome determination** when no outcome is provided
          - Rate limiting and cost estimation
          - Rich storytelling with wrestling terminology

          **Context Requirements:**
          - At least 1 wrestler (2+ recommended for segments)
          - Segment type specification
          - Optional: predetermined outcome, venue details, referee, commentators

          **Automatic Outcome Logic:**
          - Uses wrestler stats, tier bonuses, and fan weight for probability calculations
          - Weighted random selection determines winner
          - Considers wrestler health and injury status
          - Generates realistic finishing move descriptions
          """)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Segment narration generated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples =
                        @ExampleObject(
                            name = "Successful Narration",
                            value =
                                """
                                {
                                  "provider": "Google Gemini",
                                  "narration": "The bell rings as John Cena and The Rock circle each other...",
                                  "segmentType": "One on One",
                                  "wrestlers": ["John Cena", "The Rock"],
                                  "outcome": "John Cena wins via Attitude Adjustment",
                                  "estimatedCost": 0.0025
                                }
                                """))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid segment context or no AI service available"),
        @ApiResponse(responseCode = "500", description = "AI service error or rate limit exceeded")
      })
  @PostMapping("/narrate")
  public ResponseEntity<Map<String, Object>> narrateSegment(
      @Parameter(
              description =
                  "Match context including wrestlers, segment type, venue, and optional outcome",
              required = true,
              schema = @Schema(implementation = SegmentNarrationContext.class))
          @RequestBody
          SegmentNarrationContext context) {

    SegmentNarrationService service = getAppropriateService();
    if (service == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "No segment narration service available"));
    }

    try {
      // Determine outcome if not provided
      context = segmentOutcomeService.determineOutcomeIfNeeded(context);

      String narration = service.narrateSegment(context);
      double estimatedCost = serviceFactory.getEstimatedSegmentCost(service.getProviderName());

      // Build response map with null-safe values
      Map<String, Object> response = new HashMap<>();
      response.put(
          "provider", service.getProviderName() != null ? service.getProviderName() : "Unknown");
      response.put("narration", narration != null ? narration : "No narration generated");
      response.put(
          "segmentType",
          context.getSegmentType() != null && context.getSegmentType().getSegmentType() != null
              ? context.getSegmentType().getSegmentType()
              : "Unknown");
      response.put(
          "wrestlers",
          context.getWrestlers() != null
              ? context.getWrestlers().stream()
                  .filter(w -> w != null && w.getName() != null)
                  .map(WrestlerContext::getName)
                  .toList()
              : List.of());
      response.put(
          "outcome",
          context.getDeterminedOutcome() != null ? context.getDeterminedOutcome() : "Undetermined");
      response.put("estimatedCost", estimatedCost);

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error narrating segment", e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put(
          "error", e.getMessage() != null ? e.getMessage() : "Unknown error occurred");
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  @Operation(
      summary = "Test specific AI provider",
      description =
          """
          Tests a specific AI provider by generating a sample segment narration.

          This endpoint allows you to test individual AI providers (gemini, openai, claude, mock)
          to compare their output quality, response time, and cost. Useful for debugging
          and provider comparison.

          **Available Providers:**
          - `gemini` - Google Gemini (free tier available)
          - `openai` - OpenAI GPT models
          - `claude` - Anthropic Claude
          - `mock` - Mock AI for testing (no cost)
          """)
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Provider test completed successfully"),
        @ApiResponse(responseCode = "400", description = "Provider not available or not found"),
        @ApiResponse(responseCode = "500", description = "Provider test failed")
      })
  @PostMapping("/test/{provider}")
  public ResponseEntity<Map<String, Object>> testSpecificProvider(
      @Parameter(
              description = "AI provider name to test (gemini, openai, claude, mock)",
              required = true,
              example = "gemini")
          @PathVariable
          String provider) {
    SegmentNarrationService service = serviceFactory.getServiceByProvider(provider);
    if (service == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Provider '" + provider + "' not available or not found"));
    }

    try {
      SegmentNarrationContext context = createSampleSegmentContext();
      String narration = service.narrateSegment(context);
      double estimatedCost = serviceFactory.getEstimatedSegmentCost(service.getProviderName());

      // Build response map with null-safe values
      Map<String, Object> contextMap = new HashMap<>();
      contextMap.put(
          "segmentType",
          context.getSegmentType() != null && context.getSegmentType().getSegmentType() != null
              ? context.getSegmentType().getSegmentType()
              : "Unknown");
      contextMap.put(
          "wrestlers",
          context.getWrestlers() != null
              ? context.getWrestlers().stream()
                  .filter(w -> w != null && w.getName() != null)
                  .map(WrestlerContext::getName)
                  .toList()
              : List.of());
      contextMap.put(
          "outcome",
          context.getDeterminedOutcome() != null ? context.getDeterminedOutcome() : "Undetermined");

      Map<String, Object> response = new HashMap<>();
      response.put(
          "provider", service.getProviderName() != null ? service.getProviderName() : "Unknown");
      response.put("narration", narration != null ? narration : "No narration generated");
      response.put("testProvider", provider != null ? provider : "Unknown");
      response.put("estimatedCost", estimatedCost);
      response.put("context", contextMap);

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error testing provider: " + provider, e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put(
          "error", e.getMessage() != null ? e.getMessage() : "Unknown error occurred");
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  @Operation(
      summary = "Generate test segment narration",
      description =
          """
          Generates a sample segment narration using the mock AI provider for testing purposes.

          This endpoint is perfect for:
          - Testing the API without incurring AI costs
          - Development and integration testing
          - Demonstrating the narration format
          - Performance testing

          The mock AI generates realistic-looking narrations with consistent formatting
          and wrestling terminology, but without actual AI processing costs.
          """)
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Test narration generated successfully"),
        @ApiResponse(responseCode = "503", description = "Mock AI service unavailable")
      })
  @PostMapping("/test")
  public ResponseEntity<Map<String, Object>> createTestSegment() {
    SegmentNarrationService service = serviceFactory.getTestingService();
    if (service == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "No testing service available"));
    }

    try {
      SegmentNarrationContext context = createSampleSegmentContext();
      String narration = service.narrateSegment(context);
      double estimatedCost = serviceFactory.getEstimatedSegmentCost(service.getProviderName());

      return ResponseEntity.ok(
          Map.of(
              "provider",
              service.getProviderName(),
              "narration",
              narration,
              "testSegment",
              true,
              "estimatedCost",
              estimatedCost,
              "context",
              Map.of(
                  "segmentType",
                  context.getSegmentType().getSegmentType(),
                  "wrestlers",
                  context.getWrestlers().stream().map(WrestlerContext::getName).toList(),
                  "outcome",
                  context.getDeterminedOutcome())));

    } catch (Exception e) {
      log.error("Error creating test segment narration", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Create a sample segment narration for testing purposes. */
  @PostMapping("/sample")
  public ResponseEntity<Map<String, Object>> createSampleSegment() {

    SegmentNarrationService service = getAppropriateService();
    if (service == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "No segment narration service available"));
    }

    try {
      SegmentNarrationContext context = createSampleSegmentContext();
      String narration = service.narrateSegment(context);
      double estimatedCost = serviceFactory.getEstimatedSegmentCost(service.getProviderName());

      return ResponseEntity.ok(
          Map.of(
              "provider",
              service.getProviderName(),
              "narration",
              narration,
              "sampleSegment",
              true,
              "estimatedCost",
              estimatedCost,
              "context",
              Map.of(
                  "segmentType",
                  context.getSegmentType().getSegmentType(),
                  "wrestlers",
                  context.getWrestlers().stream().map(WrestlerContext::getName).toList(),
                  "outcome",
                  context.getDeterminedOutcome())));

    } catch (Exception e) {
      log.error("Error creating sample segment narration", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  @Operation(
      summary = "Generate test promo narration",
      description = "Generates a test promo narration using a sample single wrestler context")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Test promo narration generated successfully"),
        @ApiResponse(responseCode = "503", description = "Mock AI service unavailable")
      })
  @PostMapping("/test-promo")
  public ResponseEntity<Map<String, Object>> createTestPromo() {
    SegmentNarrationService service = serviceFactory.getTestingService();
    if (service == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "No testing service available"));
    }

    try {
      SegmentNarrationContext context = createSamplePromoContext();
      String narration = service.narrateSegment(context);
      double estimatedCost = serviceFactory.getEstimatedSegmentCost(service.getProviderName());

      Map<String, Object> response = new HashMap<>();
      response.put(
          "provider", service.getProviderName() != null ? service.getProviderName() : "Unknown");
      response.put("narration", narration != null ? narration : "No narration generated");
      response.put("testPromo", true);
      response.put("estimatedCost", estimatedCost);
      response.put(
          "context",
          Map.of(
              "segmentType",
              context.getSegmentType() != null && context.getSegmentType().getSegmentType() != null
                  ? context.getSegmentType().getSegmentType()
                  : "Unknown",
              "wrestlers",
              context.getWrestlers() != null
                  ? context.getWrestlers().stream()
                      .filter(w -> w != null && w.getName() != null)
                      .map(WrestlerContext::getName)
                      .toList()
                  : List.of(),
              "outcome",
              context.getDeterminedOutcome() != null
                  ? context.getDeterminedOutcome()
                  : "Undetermined"));

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error creating test promo narration", e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put(
          "error", e.getMessage() != null ? e.getMessage() : "Unknown error occurred");
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  /** Creates a sample segment context for testing. */
  private SegmentNarrationContext createSampleSegmentContext() {
    SegmentNarrationContext context = new SegmentNarrationContext();

    // Segment Type
    SegmentTypeContext segmentType = new SegmentTypeContext();
    segmentType.setSegmentType("Singles Match");
    segmentType.setStipulation("World Championship");
    segmentType.setRules(Arrays.asList("Standard Rules", "Falls Count Anywhere"));
    segmentType.setTimeLimit(30);
    context.setSegmentType(segmentType);

    // Wrestlers
    WrestlerContext rvd = new WrestlerContext();
    rvd.setName("Rob Van Dam");
    rvd.setDescription(
        "The Whole F'n Show - High-flying ECW legend known for his laid-back attitude and"
            + " innovative offense");

    MoveSet rvdMoves = new MoveSet();
    rvdMoves.setFinishers(
        Arrays.asList(
            new Move(
                "Five-Star Frog Splash",
                "High-impact top-rope splash with perfect form",
                "finisher"),
            new Move("Van Daminator", "Spinning heel kick using a steel chair", "finisher")));
    rvdMoves.setTrademarks(
        Arrays.asList(
            new Move("Rolling Thunder", "Forward roll into a senton splash", "trademark"),
            new Move(
                "Split-Legged Moonsault", "Moonsault with legs split in mid-air", "trademark")));
    rvd.setMoveSet(rvdMoves);
    rvd.setFeudsAndHeat(
        Arrays.asList("Challenging for the World Championship", "ECW vs WWE rivalry"));
    rvd.setRecentSegments(
        Arrays.asList("Defeated Sabu in hardcore segment", "Lost to Kurt Angle by submission"));

    WrestlerContext angle = new WrestlerContext();
    angle.setName("Kurt Angle");
    angle.setDescription(
        "Olympic Hero turned ruthless champion - Technical wrestling machine with legitimate"
            + " amateur background");

    MoveSet angleMoves = new MoveSet();
    angleMoves.setFinishers(
        Arrays.asList(
            new Move("Angle Slam", "Fireman's carry slam with devastating impact", "finisher"),
            new Move(
                "Ankle Lock", "Submission hold targeting the ankle and achilles", "finisher")));
    angleMoves.setTrademarks(
        Arrays.asList(
            new Move("German Suplex", "Release German suplex with perfect technique", "trademark"),
            new Move("Belly-to-Belly Suplex", "Overhead belly-to-belly throw", "trademark")));
    angle.setMoveSet(angleMoves);
    angle.setFeudsAndHeat(Arrays.asList("Defending World Championship", "Anti-ECW sentiment"));
    angle.setRecentSegments(
        Arrays.asList("Retained title against The Rock", "Defeated Rob Van Dam by submission"));

    context.setWrestlers(Arrays.asList(rvd, angle));

    // Referee
    RefereeContext referee = new RefereeContext();
    referee.setName("Earl Hebner");
    referee.setDescription("Veteran referee known for controversial decisions and fast counts");
    referee.setPersonality("Strict but can be influenced by crowd and storylines");
    context.setReferee(referee);

    // NPCs
    NPCContext announcer = new NPCContext();
    announcer.setName("Tony Chimel");
    announcer.setRole("Ring Announcer");
    announcer.setDescription("SmackDown's veteran ring announcer with distinctive voice");
    announcer.setPersonality("Professional and enthusiastic");

    NPCContext commentator1 = new NPCContext();
    commentator1.setName("Michael Cole");
    commentator1.setRole("Play-by-Play Commentator");
    commentator1.setDescription("Lead commentator calling the action");
    commentator1.setPersonality("Energetic and dramatic");

    NPCContext commentator2 = new NPCContext();
    commentator2.setName("Tazz");
    commentator2.setRole("Color Commentator");
    commentator2.setDescription("Former ECW champion providing expert analysis");
    commentator2.setPersonality("Knowledgeable with ECW bias");

    context.setNpcs(Arrays.asList(announcer, commentator1, commentator2));

    // Venue
    SegmentNarrationService.VenueContext venue = new SegmentNarrationService.VenueContext();
    venue.setName("Madison Square Garden");
    venue.setLocation("New York City, New York");
    venue.setType("Indoor Arena");
    venue.setCapacity(20000);
    venue.setDescription("The World's Most Famous Arena - Iconic venue in the heart of Manhattan");
    venue.setAtmosphere("Electric and historic - where legends are made");
    venue.setSignificance(
        "The Mecca of professional wrestling, host to countless legendary segments");
    venue.setNotableSegments(
        Arrays.asList(
            "Hulk Hogan vs Andre the Giant (1988)",
            "Shawn Michaels vs Razor Ramon Ladder Segment (1994)",
            "Stone Cold vs The Rock (1999)"));
    context.setVenue(venue);

    // Audience
    context.setAudience("Sold-out crowd of 20,000 with strong ECW contingent");
    context.setDeterminedOutcome(
        "Rob Van Dam wins the World Championship after hitting the Five-Star Frog Splash, ending"
            + " Kurt Angle's 6-month reign");
    context.setRecentSegmentNarrations(
        Arrays.asList(
            "Previous segment saw intense back-and-forth action with multiple near-falls...",
            "Last encounter ended in controversy when Angle used the ropes for leverage..."));

    return context;
  }

  /** Creates a sample promo context for testing. */
  private SegmentNarrationContext createSamplePromoContext() {
    SegmentNarrationContext context = new SegmentNarrationContext();

    // Segment Type - Set as Promo
    SegmentTypeContext segmentType = new SegmentTypeContext();
    segmentType.setSegmentType("Promo");
    segmentType.setStipulation("Championship Announcement");
    segmentType.setRules(Arrays.asList("No physical contact", "Microphone time"));
    segmentType.setTimeLimit(10); // 10 minutes for promo
    context.setSegmentType(segmentType);

    // Single Wrestler for Promo
    WrestlerContext rvd = new WrestlerContext();
    rvd.setName("Rob Van Dam");
    rvd.setDescription(
        "The Whole F'n Show - Charismatic ECW legend with laid-back attitude and strong mic"
            + " skills");

    MoveSet rvdMoves = new MoveSet();
    rvdMoves.setFinishers(
        Arrays.asList(
            new Move("Five-Star Frog Splash", "High-impact top-rope splash", "finisher"),
            new Move("Van Daminator", "Spinning heel kick using a steel chair", "finisher")));
    rvd.setMoveSet(rvdMoves);
    rvd.setFeudsAndHeat(
        Arrays.asList("Challenging for the World Championship", "ECW vs WWE rivalry"));
    rvd.setRecentSegments(
        Arrays.asList(
            "Defeated Sabu in hardcore segment", "Earned title shot by beating Kurt Angle"));

    context.setWrestlers(List.of(rvd));

    // Venue
    VenueContext venue = new VenueContext();
    venue.setName("Madison Square Garden");
    venue.setLocation("New York City, NY");
    venue.setDescription("The World's Most Famous Arena");
    venue.setAtmosphere("Electric anticipation for championship announcement");
    context.setVenue(venue);

    // Audience
    context.setAudience("Sold-out crowd of 20,000 ECW fans chanting 'RVD! RVD!'");
    context.setDeterminedOutcome(
        "Rob Van Dam delivers passionate promo about earning his championship opportunity and"
            + " promises to bring the ECW spirit to the main event");

    return context;
  }

  @Operation(
      summary = "Determine segment outcome automatically",
      description =
          """
          Determines the segment outcome automatically based on wrestler stats and ATW RPG mechanics.

          This endpoint uses the same logic as the main narration endpoint but only returns the
          determined outcome without generating the full narration. Useful for:
          - Testing outcome determination logic
          - Pre-determining outcomes before narration
          - Understanding how the system calculates segment results

          **Outcome Determination Logic:**
          - Calculates wrestler weights based on fan weight, tier bonuses, and health penalties
          - Uses weighted random selection to determine winner
          - Considers wrestler signature moves for finishing descriptions
          - Handles singles, tag team, and multi-wrestler scenarios
          """)
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Segment outcome determined successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid segment context provided")
      })
  @PostMapping("/determine-outcome")
  public ResponseEntity<Map<String, Object>> determineSegmentOutcome(
      @Parameter(
              description =
                  "Segment context with wrestlers and segment type (outcome field will be ignored)",
              required = true,
              schema = @Schema(implementation = SegmentNarrationContext.class))
          @RequestBody
          SegmentNarrationContext context) {

    try {
      // Clear any existing outcome to force determination
      context.setDeterminedOutcome(null);

      // Determine outcome
      SegmentNarrationContext contextWithOutcome =
          segmentOutcomeService.determineOutcomeIfNeeded(context);

      // Build response
      Map<String, Object> response = new HashMap<>();
      response.put(
          "determinedOutcome",
          contextWithOutcome.getDeterminedOutcome() != null
              ? contextWithOutcome.getDeterminedOutcome()
              : "Unable to determine outcome");
      response.put(
          "segmentType",
          context.getSegmentType() != null && context.getSegmentType().getSegmentType() != null
              ? context.getSegmentType().getSegmentType()
              : "Unknown");
      response.put(
          "wrestlers",
          context.getWrestlers() != null
              ? context.getWrestlers().stream()
                  .filter(w -> w != null && w.getName() != null)
                  .map(WrestlerContext::getName)
                  .toList()
              : List.of());
      response.put(
          "wrestlerCount", context.getWrestlers() != null ? context.getWrestlers().size() : 0);
      response.put("outcomeMethod", getOutcomeMethod(context.getWrestlers()));

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Error determining segment outcome", e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put(
          "error", e.getMessage() != null ? e.getMessage() : "Unknown error occurred");
      return ResponseEntity.internalServerError().body(errorResponse);
    }
  }

  /** Gets a description of the outcome determination method based on wrestler count. */
  private String getOutcomeMethod(List<WrestlerContext> wrestlers) {
    if (wrestlers == null || wrestlers.isEmpty()) {
      return "No wrestlers provided";
    }

    return switch (wrestlers.size()) {
      case 1 -> "Single wrestler exhibition/promo";
      case 2 -> "Two-wrestler segment using weighted probability based on stats";
      default -> "Multi-wrestler segment using weighted random selection";
    };
  }
}

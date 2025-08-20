package com.github.javydreamercsw.base.ai;

import com.github.javydreamercsw.base.ai.MatchNarrationService.MatchNarrationContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.MatchTypeContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.Move;
import com.github.javydreamercsw.base.ai.MatchNarrationService.MoveSet;
import com.github.javydreamercsw.base.ai.MatchNarrationService.NPCContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.RefereeContext;
import com.github.javydreamercsw.base.ai.MatchNarrationService.WrestlerContext;
import java.util.Arrays;
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

/** REST controller for AI-powered wrestling match narration. */
@RestController
@RequestMapping("/api/match-narration")
@RequiredArgsConstructor
@Slf4j
public class MatchNarrationController {

  private final MatchNarrationServiceFactory serviceFactory;
  private final MatchNarrationConfig config;
  private final Environment environment;

  /**
   * Gets the appropriate service based on the current environment. In test profile, always use the
   * testing service to avoid costs.
   */
  private MatchNarrationService getAppropriateService() {
    if (isTestProfile()) {
      log.debug("Test profile detected, using testing service");
      return serviceFactory.getTestingService();
    }
    return serviceFactory.getBestAvailableService();
  }

  /** Checks if the test profile is active. */
  private boolean isTestProfile() {
    return environment.acceptsProfiles("test");
  }

  /** Get current AI provider limits and configuration. */
  @GetMapping("/limits")
  public ResponseEntity<Map<String, Object>> getLimits() {
    MatchNarrationService currentService = getAppropriateService();

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
            Arrays.stream(MatchNarrationConfig.ProviderLimits.values())
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

  /** Narrate a wrestling match based on provided context. */
  @PostMapping("/narrate")
  public ResponseEntity<Map<String, Object>> narrateMatch(
      @RequestBody MatchNarrationContext context) {

    MatchNarrationService service = getAppropriateService();
    if (service == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "No match narration service available"));
    }

    try {
      String narration = service.narrateMatch(context);
      double estimatedCost = serviceFactory.getEstimatedMatchCost(service.getProviderName());

      return ResponseEntity.ok(
          Map.of(
              "provider",
              service.getProviderName(),
              "narration",
              narration,
              "matchType",
              context.getMatchType().getMatchType(),
              "wrestlers",
              context.getWrestlers().stream().map(WrestlerContext::getName).toList(),
              "outcome",
              context.getDeterminedOutcome(),
              "estimatedCost",
              estimatedCost));

    } catch (Exception e) {
      log.error("Error narrating match", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Test a specific AI provider by name. */
  @PostMapping("/test/{provider}")
  public ResponseEntity<Map<String, Object>> testSpecificProvider(@PathVariable String provider) {
    MatchNarrationService service = serviceFactory.getServiceByProvider(provider);
    if (service == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Provider '" + provider + "' not available or not found"));
    }

    try {
      MatchNarrationContext context = createSampleContext();
      String narration = service.narrateMatch(context);
      double estimatedCost = serviceFactory.getEstimatedMatchCost(service.getProviderName());

      return ResponseEntity.ok(
          Map.of(
              "provider",
              service.getProviderName(),
              "narration",
              narration,
              "testProvider",
              provider,
              "estimatedCost",
              estimatedCost,
              "context",
              Map.of(
                  "matchType",
                  context.getMatchType().getMatchType(),
                  "wrestlers",
                  context.getWrestlers().stream().map(WrestlerContext::getName).toList(),
                  "outcome",
                  context.getDeterminedOutcome())));

    } catch (Exception e) {
      log.error("Error testing provider: " + provider, e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Create a sample match narration using mock AI for testing purposes. */
  @PostMapping("/test")
  public ResponseEntity<Map<String, Object>> createTestMatch() {
    MatchNarrationService service = serviceFactory.getTestingService();
    if (service == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "No testing service available"));
    }

    try {
      MatchNarrationContext context = createSampleContext();
      String narration = service.narrateMatch(context);
      double estimatedCost = serviceFactory.getEstimatedMatchCost(service.getProviderName());

      return ResponseEntity.ok(
          Map.of(
              "provider",
              service.getProviderName(),
              "narration",
              narration,
              "testMatch",
              true,
              "estimatedCost",
              estimatedCost,
              "context",
              Map.of(
                  "matchType",
                  context.getMatchType().getMatchType(),
                  "wrestlers",
                  context.getWrestlers().stream().map(WrestlerContext::getName).toList(),
                  "outcome",
                  context.getDeterminedOutcome())));

    } catch (Exception e) {
      log.error("Error creating test match narration", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Create a sample match narration for testing purposes. */
  @PostMapping("/sample")
  public ResponseEntity<Map<String, Object>> createSampleMatch() {

    MatchNarrationService service = getAppropriateService();
    if (service == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "No match narration service available"));
    }

    try {
      MatchNarrationContext context = createSampleContext();
      String narration = service.narrateMatch(context);
      double estimatedCost = serviceFactory.getEstimatedMatchCost(service.getProviderName());

      return ResponseEntity.ok(
          Map.of(
              "provider",
              service.getProviderName(),
              "narration",
              narration,
              "sampleMatch",
              true,
              "estimatedCost",
              estimatedCost,
              "context",
              Map.of(
                  "matchType",
                  context.getMatchType().getMatchType(),
                  "wrestlers",
                  context.getWrestlers().stream().map(WrestlerContext::getName).toList(),
                  "outcome",
                  context.getDeterminedOutcome())));

    } catch (Exception e) {
      log.error("Error creating sample match narration", e);
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }

  /** Creates a sample match context for testing. */
  private MatchNarrationContext createSampleContext() {
    MatchNarrationContext context = new MatchNarrationContext();

    // Match Type
    MatchTypeContext matchType = new MatchTypeContext();
    matchType.setMatchType("Singles Match");
    matchType.setStipulation("World Championship");
    matchType.setRules(Arrays.asList("Standard Rules", "Falls Count Anywhere"));
    matchType.setTimeLimit(30);
    context.setMatchType(matchType);

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
    rvd.setRecentMatches(
        Arrays.asList("Defeated Sabu in hardcore match", "Lost to Kurt Angle by submission"));

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
    angle.setRecentMatches(
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
    MatchNarrationService.VenueContext venue = new MatchNarrationService.VenueContext();
    venue.setName("Madison Square Garden");
    venue.setLocation("New York City, New York");
    venue.setType("Indoor Arena");
    venue.setCapacity(20000);
    venue.setDescription("The World's Most Famous Arena - Iconic venue in the heart of Manhattan");
    venue.setAtmosphere("Electric and historic - where legends are made");
    venue.setSignificance(
        "The Mecca of professional wrestling, host to countless legendary matches");
    venue.setNotableMatches(
        Arrays.asList(
            "Hulk Hogan vs Andre the Giant (1988)",
            "Shawn Michaels vs Razor Ramon Ladder Match (1994)",
            "Stone Cold vs The Rock (1999)"));
    context.setVenue(venue);

    // Audience
    context.setAudience("Sold-out crowd of 20,000 with strong ECW contingent");
    context.setDeterminedOutcome(
        "Rob Van Dam wins the World Championship after hitting the Five-Star Frog Splash, ending"
            + " Kurt Angle's 6-month reign");
    context.setRecentMatchNarrations(
        Arrays.asList(
            "Previous match saw intense back-and-forth action with multiple near-falls...",
            "Last encounter ended in controversy when Angle used the ropes for leverage..."));

    return context;
  }
}

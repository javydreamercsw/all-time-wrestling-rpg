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
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

  private SegmentNarrationService getAppropriateService() {
    if (isTestProfile()) {
      log.debug("Test profile detected, using testing service");
      return serviceFactory.getTestingService();
    }
    return serviceFactory.getBestAvailableService();
  }

  private boolean isTestProfile() {
    return environment.matchesProfiles("test");
  }

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
                            (int) (limit.getMaxOutputTokens() * 0.75)))
                .collect(Collectors.toList())));
  }

  @PostMapping("/narrate")
  public ResponseEntity<Map<String, Object>> narrateSegment(
      @RequestBody SegmentNarrationContext context) {
    return narrateSegmentWithProvider(null, context);
  }

  @PostMapping("/narrate/{provider}")
  public ResponseEntity<Map<String, Object>> narrateSegmentWithProvider(
      @PathVariable(required = false) String provider,
      @RequestBody SegmentNarrationContext context) {
    SegmentNarrationService service =
        provider == null ? getAppropriateService() : serviceFactory.getServiceByProvider(provider);
    if (service == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Provider '" + provider + "' not available or not found"));
    }
    try {
      context = segmentOutcomeService.determineOutcomeIfNeeded(context);
      log.info(context.toString());
      String narration = service.narrateSegment(context);
      double estimatedCost = serviceFactory.getEstimatedSegmentCost(service.getProviderName());
      Map<String, Object> response = new HashMap<>();
      response.put("provider", service.getProviderName());
      response.put("narration", narration);
      response.put("segmentType", context.getSegmentType().getSegmentType());
      response.put(
          "wrestlers",
          context.getWrestlers() == null
              ? List.of()
              : context.getWrestlers().stream()
                  .map(WrestlerContext::getName)
                  .collect(Collectors.toList()));
      response.put("outcome", context.getDeterminedOutcome());
      response.put("estimatedCost", estimatedCost);
      return ResponseEntity.ok(response);
    } catch (AIServiceException e) {
      log.error("AI service error: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("provider", e.getProvider());
      errorResponse.put("statusCode", e.getStatusCode());
      errorResponse.put("error", e.getMessage());
      List<Map<String, Object>> alternativeProviders =
          serviceFactory.getAvailableServices().stream()
              .filter(p -> !p.providerName().equals(e.getProvider()))
              .map(
                  p -> {
                    Map<String, Object> providerMap = new HashMap<>();
                    providerMap.put("provider", p.providerName());
                    providerMap.put(
                        "estimatedCost", serviceFactory.getEstimatedSegmentCost(p.providerName()));
                    return providerMap;
                  })
              .collect(Collectors.toList());
      if (!alternativeProviders.isEmpty()) {
        errorResponse.put("alternativeProviders", alternativeProviders);
      }
      return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    } catch (Exception e) {
      log.error("Error narrating segment", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Unknown error occurred: " + e.getMessage()));
    }
  }

  @PostMapping("/test/{provider}")
  public ResponseEntity<Map<String, Object>> testSpecificProvider(@PathVariable String provider) {
    SegmentNarrationService service = serviceFactory.getServiceByProvider(provider);
    if (service == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Provider '" + provider + "' not available or not found"));
    }
    try {
      SegmentNarrationContext context = createSampleSegmentContext();
      String narration = service.narrateSegment(context);
      double estimatedCost = serviceFactory.getEstimatedSegmentCost(service.getProviderName());
      Map<String, Object> response = new HashMap<>();
      response.put("provider", service.getProviderName());
      response.put("narration", narration);
      response.put("testProvider", provider);
      response.put("estimatedCost", estimatedCost);
      response.put(
          "context",
          Map.of(
              "segmentType",
              context.getSegmentType().getSegmentType(),
              "wrestlers",
              context.getWrestlers().stream()
                  .map(WrestlerContext::getName)
                  .collect(Collectors.toList()),
              "outcome",
              context.getDeterminedOutcome()));
      return ResponseEntity.ok(response);
    } catch (AIServiceException e) {
      log.error("AI service error testing provider: {}", e.getMessage());
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("provider", e.getProvider());
      errorResponse.put("statusCode", e.getStatusCode());
      errorResponse.put("error", e.getMessage());
      List<Map<String, Object>> alternativeProviders =
          serviceFactory.getAvailableServices().stream()
              .filter(p -> !p.providerName().equals(e.getProvider()))
              .map(
                  p -> {
                    Map<String, Object> providerMap = new HashMap<>();
                    providerMap.put("provider", p.providerName());
                    providerMap.put(
                        "estimatedCost", serviceFactory.getEstimatedSegmentCost(p.providerName()));
                    return providerMap;
                  })
              .collect(Collectors.toList());
      if (!alternativeProviders.isEmpty()) {
        errorResponse.put("alternativeProviders", alternativeProviders);
      }
      return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    } catch (Exception e) {
      log.error("Error testing provider: " + provider, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Unknown error occurred: " + e.getMessage()));
    }
  }

  private SegmentNarrationContext createSampleSegmentContext() {
    SegmentNarrationContext context = new SegmentNarrationContext();
    SegmentTypeContext segmentType = new SegmentTypeContext();
    segmentType.setSegmentType("Singles Match");
    segmentType.setStipulation("World Championship");
    segmentType.setRules(Arrays.asList("Standard Rules", "Falls Count Anywhere"));
    segmentType.setTimeLimit(30);
    context.setSegmentType(segmentType);
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
    RefereeContext referee = new RefereeContext();
    referee.setName("Earl Hebner");
    referee.setDescription("Veteran referee known for controversial decisions and fast counts");
    referee.setPersonality("Strict but can be influenced by crowd and storylines");
    context.setReferee(referee);
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
    VenueContext venue = new VenueContext();
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
}

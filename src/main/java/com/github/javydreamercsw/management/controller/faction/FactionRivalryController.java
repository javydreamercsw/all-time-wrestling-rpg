package com.github.javydreamercsw.management.controller.faction;

import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService.ResolutionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for managing faction rivalries in the ATW RPG system. */
@RestController
@RequestMapping("/api/faction-rivalries")
@RequiredArgsConstructor
@Tag(name = "Faction Rivalries", description = "Faction rivalry management operations")
public class FactionRivalryController {

  private final FactionRivalryService factionRivalryService;

  @Operation(
      summary = "Get all faction rivalries",
      description = "Retrieve all faction rivalries with pagination")
  @GetMapping
  public ResponseEntity<Page<FactionRivalry>> getAllFactionRivalries(Pageable pageable) {
    Page<FactionRivalry> rivalries = factionRivalryService.getAllFactionRivalries(pageable);
    return ResponseEntity.ok(rivalries);
  }

  @Operation(
      summary = "Get faction rivalry by ID",
      description = "Retrieve a specific faction rivalry by its ID")
  @GetMapping("/{id}")
  public ResponseEntity<FactionRivalry> getFactionRivalryById(@PathVariable Long id) {
    Optional<FactionRivalry> rivalry = factionRivalryService.getFactionRivalryById(id);
    return rivalry.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get active faction rivalries",
      description = "Retrieve all active faction rivalries")
  @GetMapping("/active")
  public ResponseEntity<List<FactionRivalry>> getActiveFactionRivalries() {
    List<FactionRivalry> rivalries = factionRivalryService.getActiveFactionRivalries();
    return ResponseEntity.ok(rivalries);
  }

  @Operation(
      summary = "Get rivalries for faction",
      description = "Retrieve active rivalries for a specific faction")
  @GetMapping("/faction/{factionId}")
  public ResponseEntity<List<FactionRivalry>> getRivalriesForFaction(@PathVariable Long factionId) {
    List<FactionRivalry> rivalries = factionRivalryService.getActiveRivalriesForFaction(factionId);
    return ResponseEntity.ok(rivalries);
  }

  @Operation(
      summary = "Get rivalries requiring matches",
      description = "Retrieve rivalries that require matches at next show")
  @GetMapping("/requiring-matches")
  public ResponseEntity<List<FactionRivalry>> getRivalriesRequiringMatches() {
    List<FactionRivalry> rivalries = factionRivalryService.getRivalriesRequiringMatches();
    return ResponseEntity.ok(rivalries);
  }

  @Operation(
      summary = "Get rivalries eligible for resolution",
      description = "Retrieve rivalries eligible for resolution")
  @GetMapping("/eligible-for-resolution")
  public ResponseEntity<List<FactionRivalry>> getRivalriesEligibleForResolution() {
    List<FactionRivalry> rivalries = factionRivalryService.getRivalriesEligibleForResolution();
    return ResponseEntity.ok(rivalries);
  }

  @Operation(
      summary = "Get rivalries requiring rule matches",
      description = "Retrieve rivalries requiring rule matches")
  @GetMapping("/requiring-rule")
  public ResponseEntity<List<FactionRivalry>> getRivalriesRequiringStipulationMatches() {
    List<FactionRivalry> rivalries =
        factionRivalryService.getRivalriesRequiringStipulationMatches();
    return ResponseEntity.ok(rivalries);
  }

  @Operation(
      summary = "Get hottest rivalries",
      description = "Retrieve the hottest faction rivalries")
  @GetMapping("/hottest")
  public ResponseEntity<List<FactionRivalry>> getHottestRivalries(
      @RequestParam(defaultValue = "10") int limit) {
    List<FactionRivalry> rivalries = factionRivalryService.getHottestRivalries(limit);
    return ResponseEntity.ok(rivalries);
  }

  @Operation(
      summary = "Get tag team rivalries",
      description = "Retrieve tag team faction rivalries")
  @GetMapping("/tag-team")
  public ResponseEntity<List<FactionRivalry>> getTagTeamRivalries() {
    List<FactionRivalry> rivalries = factionRivalryService.getTagTeamRivalries();
    return ResponseEntity.ok(rivalries);
  }

  @Operation(summary = "Get stable rivalries", description = "Retrieve rivalries involving stables")
  @GetMapping("/involving-stables")
  public ResponseEntity<List<FactionRivalry>> getRivalriesInvolvingStables() {
    List<FactionRivalry> rivalries = factionRivalryService.getRivalriesInvolvingStables();
    return ResponseEntity.ok(rivalries);
  }

  @Operation(summary = "Create faction rivalry", description = "Create a new faction rivalry")
  @PostMapping
  public ResponseEntity<Object> createFactionRivalry(
      @Valid @RequestBody CreateFactionRivalryRequest request) {
    Optional<FactionRivalry> rivalry =
        factionRivalryService.createFactionRivalry(
            request.faction1Id(), request.faction2Id(), request.storylineNotes());

    if (rivalry.isPresent()) {
      return ResponseEntity.ok(rivalry.get());
    } else {
      return ResponseEntity.badRequest()
          .body(
              new ErrorResponse(
                  "Cannot create faction rivalry - factions not found or rivalry already exists"));
    }
  }

  @Operation(
      summary = "Add heat to faction rivalry",
      description = "Add heat to an existing faction rivalry")
  @PostMapping("/{id}/heat")
  public ResponseEntity<Object> addHeat(
      @PathVariable Long id, @Valid @RequestBody AddHeatRequest request) {
    Optional<FactionRivalry> rivalry =
        factionRivalryService.addHeat(id, request.heatGain(), request.reason());

    if (rivalry.isPresent()) {
      return ResponseEntity.ok(rivalry.get());
    } else {
      return ResponseEntity.badRequest()
          .body(new ErrorResponse("Cannot add heat - faction rivalry not found or inactive"));
    }
  }

  @Operation(
      summary = "Add heat between factions",
      description = "Add heat between two factions (creates rivalry if needed)")
  @PostMapping("/heat")
  public ResponseEntity<Object> addHeatBetweenFactions(
      @Valid @RequestBody AddHeatBetweenFactionsRequest request) {
    Optional<FactionRivalry> rivalry =
        factionRivalryService.addHeatBetweenFactions(
            request.faction1Id(), request.faction2Id(), request.heatGain(), request.reason());

    if (rivalry.isPresent()) {
      return ResponseEntity.ok(rivalry.get());
    } else {
      return ResponseEntity.badRequest()
          .body(new ErrorResponse("Cannot add heat - factions not found"));
    }
  }

  @Operation(
      summary = "Attempt rivalry resolution",
      description = "Attempt to resolve a faction rivalry with dice rolls")
  @PostMapping("/{id}/resolve")
  public ResponseEntity<ResolutionResult> attemptResolution(
      @PathVariable Long id, @Valid @RequestBody AttemptResolutionRequest request) {
    ResolutionResult result =
        factionRivalryService.attemptResolution(id, request.faction1Roll(), request.faction2Roll());

    return ResponseEntity.ok(result);
  }

  @Operation(summary = "End faction rivalry", description = "End a faction rivalry")
  @DeleteMapping("/{id}")
  public ResponseEntity<Object> endFactionRivalry(
      @PathVariable Long id, @RequestParam(defaultValue = "Rivalry ended") String reason) {
    Optional<FactionRivalry> rivalry = factionRivalryService.endFactionRivalry(id, reason);

    if (rivalry.isPresent()) {
      return ResponseEntity.ok(rivalry.get());
    } else {
      return ResponseEntity.badRequest()
          .body(new ErrorResponse("Cannot end faction rivalry - rivalry not found"));
    }
  }

  @Operation(
      summary = "Get rivalry statistics",
      description = "Get statistics about faction rivalries")
  @GetMapping("/statistics")
  public ResponseEntity<FactionRivalryStatistics> getRivalryStatistics() {
    List<FactionRivalry> activeRivalries = factionRivalryService.getActiveFactionRivalries();
    Long totalWrestlers = factionRivalryService.getTotalWrestlersInRivalries();

    FactionRivalryStatistics stats =
        new FactionRivalryStatistics(
            activeRivalries.size(),
            factionRivalryService.getRivalriesRequiringMatches().size(),
            factionRivalryService.getRivalriesEligibleForResolution().size(),
            factionRivalryService.getRivalriesRequiringStipulationMatches().size(),
            totalWrestlers.intValue());

    return ResponseEntity.ok(stats);
  }

  // ==================== REQUEST/RESPONSE RECORDS ====================

  public record CreateFactionRivalryRequest(
      Long faction1Id, Long faction2Id, String storylineNotes) {}

  public record AddHeatRequest(int heatGain, String reason) {}

  public record AddHeatBetweenFactionsRequest(
      Long faction1Id, Long faction2Id, int heatGain, String reason) {}

  public record AttemptResolutionRequest(Integer faction1Roll, Integer faction2Roll) {}

  public record FactionRivalryStatistics(
      int totalActiveRivalries,
      int rivalriesRequiringMatches,
      int rivalriesEligibleForResolution,
      int rivalriesRequiringStipulation,
      int totalWrestlersInvolved) {}

  public record ErrorResponse(String message) {}
}

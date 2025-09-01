package com.github.javydreamercsw.management.controller.faction;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.service.faction.FactionService;
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

/** REST controller for managing factions in the ATW RPG system. */
@RestController
@RequestMapping("/api/factions")
@RequiredArgsConstructor
@Tag(name = "Factions", description = "Faction management operations")
public class FactionController {

  private final FactionService factionService;

  @Operation(summary = "Get all factions", description = "Retrieve all factions with pagination")
  @GetMapping
  public ResponseEntity<Page<Faction>> getAllFactions(Pageable pageable) {
    Page<Faction> factions = factionService.getAllFactions(pageable);
    return ResponseEntity.ok(factions);
  }

  @Operation(summary = "Get faction by ID", description = "Retrieve a specific faction by its ID")
  @GetMapping("/{id}")
  public ResponseEntity<Faction> getFactionById(@PathVariable Long id) {
    Optional<Faction> faction = factionService.getFactionById(id);
    return faction.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get faction by name",
      description = "Retrieve a specific faction by its name")
  @GetMapping("/name/{name}")
  public ResponseEntity<Faction> getFactionByName(@PathVariable String name) {
    Optional<Faction> faction = factionService.getFactionByName(name);
    return faction.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Get active factions", description = "Retrieve all active factions")
  @GetMapping("/active")
  public ResponseEntity<List<Faction>> getActiveFactions() {
    List<Faction> factions = factionService.getActiveFactions();
    return ResponseEntity.ok(factions);
  }

  @Operation(
      summary = "Get factions by type",
      description = "Retrieve factions by type (singles, tag, stable)")
  @GetMapping("/type/{type}")
  public ResponseEntity<List<Faction>> getFactionsByType(@PathVariable String type) {
    List<Faction> factions = factionService.getFactionsByType(type);
    return ResponseEntity.ok(factions);
  }

  @Operation(
      summary = "Get largest factions",
      description = "Retrieve the largest factions by member count")
  @GetMapping("/largest")
  public ResponseEntity<List<Faction>> getLargestFactions(
      @RequestParam(defaultValue = "10") int limit) {
    List<Faction> factions = factionService.getLargestFactions(limit);
    return ResponseEntity.ok(factions);
  }

  @Operation(
      summary = "Get factions with rivalries",
      description = "Retrieve factions that have active rivalries")
  @GetMapping("/with-rivalries")
  public ResponseEntity<List<Faction>> getFactionsWithRivalries() {
    List<Faction> factions = factionService.getFactionsWithActiveRivalries();
    return ResponseEntity.ok(factions);
  }

  @Operation(
      summary = "Get faction for wrestler",
      description = "Retrieve the faction a wrestler belongs to")
  @GetMapping("/wrestler/{wrestlerId}")
  public ResponseEntity<Faction> getFactionForWrestler(@PathVariable Long wrestlerId) {
    Optional<Faction> faction = factionService.getFactionForWrestler(wrestlerId);
    return faction.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Create faction", description = "Create a new faction")
  @PostMapping
  public ResponseEntity<Object> createFaction(@Valid @RequestBody CreateFactionRequest request) {
    Optional<Faction> faction =
        factionService.createFaction(request.name(), request.description(), request.leaderId());

    if (faction.isPresent()) {
      return ResponseEntity.ok(faction.get());
    } else {
      return ResponseEntity.badRequest()
          .body(
              new ErrorResponse(
                  "Cannot create faction - name may already exist or leader not found"));
    }
  }

  @Operation(summary = "Add member to faction", description = "Add a wrestler to a faction")
  @PostMapping("/{id}/members")
  public ResponseEntity<Object> addMember(
      @PathVariable Long id, @Valid @RequestBody AddMemberRequest request) {
    Optional<Faction> faction = factionService.addMemberToFaction(id, request.wrestlerId());

    if (faction.isPresent()) {
      return ResponseEntity.ok(faction.get());
    } else {
      return ResponseEntity.badRequest()
          .body(
              new ErrorResponse(
                  "Cannot add member - faction or wrestler not found, or wrestler already in"
                      + " another faction"));
    }
  }

  @Operation(
      summary = "Remove member from faction",
      description = "Remove a wrestler from a faction")
  @DeleteMapping("/{id}/members/{wrestlerId}")
  public ResponseEntity<Object> removeMember(
      @PathVariable Long id,
      @PathVariable Long wrestlerId,
      @RequestParam(defaultValue = "Removed from faction") String reason) {
    Optional<Faction> faction = factionService.removeMemberFromFaction(id, wrestlerId, reason);

    if (faction.isPresent()) {
      return ResponseEntity.ok(faction.get());
    } else {
      return ResponseEntity.badRequest()
          .body(
              new ErrorResponse(
                  "Cannot remove member - faction or wrestler not found, or wrestler not in"
                      + " faction"));
    }
  }

  @Operation(summary = "Change faction leader", description = "Change the leader of a faction")
  @PutMapping("/{id}/leader")
  public ResponseEntity<Object> changeLeader(
      @PathVariable Long id, @Valid @RequestBody ChangeLeaderRequest request) {
    Optional<Faction> faction = factionService.changeFactionLeader(id, request.newLeaderId());

    if (faction.isPresent()) {
      return ResponseEntity.ok(faction.get());
    } else {
      return ResponseEntity.badRequest()
          .body(
              new ErrorResponse(
                  "Cannot change leader - faction or wrestler not found, or wrestler not in"
                      + " faction"));
    }
  }

  @Operation(summary = "Disband faction", description = "Disband a faction")
  @DeleteMapping("/{id}")
  public ResponseEntity<Object> disbandFaction(
      @PathVariable Long id, @RequestParam(defaultValue = "Faction disbanded") String reason) {
    Optional<Faction> faction = factionService.disbandFaction(id, reason);

    if (faction.isPresent()) {
      return ResponseEntity.ok(faction.get());
    } else {
      return ResponseEntity.badRequest()
          .body(new ErrorResponse("Cannot disband faction - faction not found"));
    }
  }

  // ==================== REQUEST/RESPONSE RECORDS ====================

  public record CreateFactionRequest(String name, String description, Long leaderId) {}

  public record AddMemberRequest(Long wrestlerId) {}

  public record ChangeLeaderRequest(Long newLeaderId) {}

  public record ErrorResponse(String message) {}
}

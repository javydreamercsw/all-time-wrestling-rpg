/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.controller.drama;

import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing drama events in the ATW RPG system. Provides endpoints for creating,
 * processing, and querying drama events that affect storylines and wrestler development.
 */
@RestController
@RequestMapping("/api/drama-events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Drama Events", description = "Drama event management for storylines and narrative")
public class DramaEventController {

  private final DramaEventService dramaEventService;

  @Operation(
      summary = "Create a new drama event",
      description = "Creates a new drama event for one or two wrestlers")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Drama event created successfully"),
        @ApiResponse(responseCode = "404", description = "Wrestler not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  @PostMapping
  public ResponseEntity<Object> createDramaEvent(
      @Valid @RequestBody CreateDramaEventRequest request) {
    Optional<DramaEvent> event =
        dramaEventService.createDramaEvent(
            request.primaryWrestlerId(),
            request.secondaryWrestlerId(),
            request.eventType(),
            request.severity(),
            request.title(),
            request.description());

    return event
        .<ResponseEntity<Object>>map(dramaEvent -> ResponseEntity.status(201).body(dramaEvent))
        .orElseGet(
            () ->
                ResponseEntity.badRequest()
                    .body("Failed to create drama event - wrestler not found"));
  }

  @Operation(
      summary = "Generate random drama event",
      description = "Generates a random drama event for a wrestler")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Random drama event generated"),
        @ApiResponse(responseCode = "404", description = "Wrestler not found")
      })
  @PostMapping("/generate/{wrestlerId}")
  public ResponseEntity<Object> generateRandomDramaEvent(@PathVariable Long wrestlerId) {
    Optional<DramaEvent> event = dramaEventService.generateRandomDramaEvent(wrestlerId);

    return event
        .<ResponseEntity<Object>>map(dramaEvent -> ResponseEntity.status(201).body(dramaEvent))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Process unprocessed events",
      description = "Processes all unprocessed drama events and applies their impacts")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Events processed successfully")})
  @PostMapping("/process")
  public ResponseEntity<ProcessingResult> processUnprocessedEvents() {
    int processedCount = dramaEventService.processUnprocessedEvents();
    return ResponseEntity.ok(
        new ProcessingResult(
            processedCount, "Successfully processed " + processedCount + " drama events"));
  }

  @Operation(
      summary = "Get drama events for wrestler",
      description = "Gets all drama events involving a specific wrestler")
  @GetMapping("/wrestler/{wrestlerId}")
  public ResponseEntity<List<DramaEvent>> getEventsForWrestler(@PathVariable Long wrestlerId) {
    List<DramaEvent> events = dramaEventService.getEventsForWrestler(wrestlerId);
    return ResponseEntity.ok(events);
  }

  @Operation(
      summary = "Get drama events for wrestler (paginated)",
      description = "Gets drama events for a wrestler with pagination")
  @GetMapping("/wrestler/{wrestlerId}/paginated")
  public ResponseEntity<Page<DramaEvent>> getEventsForWrestlerPaginated(
      @PathVariable Long wrestlerId, Pageable pageable) {
    Page<DramaEvent> events = dramaEventService.getEventsForWrestler(wrestlerId, pageable);
    return ResponseEntity.ok(events);
  }

  @Operation(
      summary = "Get recent drama events",
      description = "Gets drama events from the last 30 days")
  @GetMapping("/recent")
  public ResponseEntity<List<DramaEvent>> getRecentEvents() {
    List<DramaEvent> events = dramaEventService.getRecentEvents();
    return ResponseEntity.ok(events);
  }

  @Operation(
      summary = "Get drama events between wrestlers",
      description = "Gets all drama events between two specific wrestlers")
  @GetMapping("/between/{wrestler1Id}/{wrestler2Id}")
  public ResponseEntity<List<DramaEvent>> getEventsBetweenWrestlers(
      @PathVariable Long wrestler1Id, @PathVariable Long wrestler2Id) {
    List<DramaEvent> events = dramaEventService.getEventsBetweenWrestlers(wrestler1Id, wrestler2Id);
    return ResponseEntity.ok(events);
  }

  @Operation(
      summary = "Get available event types",
      description = "Gets all available drama event types")
  @GetMapping("/types")
  public ResponseEntity<DramaEventType[]> getEventTypes() {
    return ResponseEntity.ok(DramaEventType.values());
  }

  @Operation(
      summary = "Get available severities",
      description = "Gets all available drama event severities")
  @GetMapping("/severities")
  public ResponseEntity<DramaEventSeverity[]> getSeverities() {
    return ResponseEntity.ok(DramaEventSeverity.values());
  }

  // ==================== REQUEST/RESPONSE DTOs ====================

  /** Request DTO for creating drama events. */
  public record CreateDramaEventRequest(
      @NotNull Long primaryWrestlerId,
      Long secondaryWrestlerId, // Optional
      @NotNull DramaEventType eventType,
      @NotNull DramaEventSeverity severity,
      @NotBlank String title,
      @NotBlank String description) {}

  /** Response DTO for processing results. */
  public record ProcessingResult(int processedCount, String message) {}
}

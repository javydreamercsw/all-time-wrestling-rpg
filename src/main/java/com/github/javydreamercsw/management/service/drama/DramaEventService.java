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
package com.github.javydreamercsw.management.service.drama;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.util.LogSanitizer;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.event.DramaEventCreatedEvent;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing drama events (backstage incidents, injuries, etc.) in the ATW RPG system.
 */
@Service
@Transactional
@Slf4j
public class DramaEventService {

  private final DramaEventRepository dramaEventRepository;
  private final WrestlerRepository wrestlerRepository;
  private final UniverseRepository universeRepository;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final WrestlerService wrestlerService;
  private final RivalryService rivalryService;
  private final InjuryService injuryService;
  private final SecurityUtils securityUtils;
  private final Clock clock;
  private final Random random;
  private final ApplicationEventPublisher eventPublisher;

  @Autowired
  public DramaEventService(
      final DramaEventRepository dramaEventRepository,
      final WrestlerRepository wrestlerRepository,
      final UniverseRepository universeRepository,
      final WrestlerStateRepository wrestlerStateRepository,
      final WrestlerService wrestlerService,
      final RivalryService rivalryService,
      final InjuryService injuryService,
      final SecurityUtils securityUtils,
      final Clock clock,
      final Random random,
      final ApplicationEventPublisher eventPublisher) {
    this.dramaEventRepository = dramaEventRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.universeRepository = universeRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
    this.wrestlerService = wrestlerService;
    this.rivalryService = rivalryService;
    this.injuryService = injuryService;
    this.securityUtils = securityUtils;
    this.clock = clock;
    this.random = random;
    this.eventPublisher = eventPublisher;
  }

  /** Create a manual drama event. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Optional<DramaEvent> createDramaEvent(
      @NonNull final Long wrestler1Id,
      final Long wrestler2Id,
      @NonNull final DramaEventType eventType,
      @NonNull final DramaEventSeverity severity,
      @NonNull final String title,
      @NonNull final String description,
      @NonNull final Long universeId) {

    Wrestler w1 =
        wrestlerRepository
            .findById(wrestler1Id)
            .orElseThrow(() -> new IllegalArgumentException("Wrestler not found: " + wrestler1Id));
    Universe universe =
        universeRepository
            .findById(universeId)
            .orElseThrow(() -> new IllegalArgumentException("Universe not found: " + universeId));

    DramaEvent event = new DramaEvent();
    event.setPrimaryWrestler(w1);
    if (wrestler2Id != null) {
      wrestlerRepository.findById(wrestler2Id).ifPresent(event::setSecondaryWrestler);
    }
    event.setUniverse(universe);
    event.setEventType(eventType);
    event.setSeverity(severity);
    event.setTitle(title);
    event.setDescription(description);
    event.setEventDate(Instant.now(clock));
    event.setIsProcessed(false);
    event.setCreationDate(Instant.now(clock));

    DramaEvent saved = dramaEventRepository.save(event);
    eventPublisher.publishEvent(new DramaEventCreatedEvent(this, saved));
    return Optional.of(saved);
  }

  /** Generate a random drama event for a wrestler. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Optional<DramaEvent> generateRandomDramaEvent(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    // Basic implementation for generating random events
    Wrestler wrestler =
        wrestlerRepository
            .findById(wrestlerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Wrestler not found with id: " + wrestlerId));

    // Choose random event type and severity
    DramaEventType[] types = DramaEventType.values();
    DramaEventType type = types[random.nextInt(types.length)];

    DramaEventSeverity[] severities = DramaEventSeverity.values();
    DramaEventSeverity severity = severities[random.nextInt(severities.length)];

    return createDramaEvent(
        wrestlerId,
        null,
        type,
        severity,
        "Random Event: " + type.getDisplayName(),
        "Auto-generated drama event for " + wrestler.getName(),
        universeId);
  }

  /** Process all unprocessed drama events. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void processUnprocessedEvents() {
    List<DramaEvent> unprocessed = dramaEventRepository.findByIsProcessedFalseOrderByEventDateAsc();
    for (DramaEvent event : unprocessed) {
      processEvent(event);
    }
  }

  /** Process a specific drama event, applying its effects. */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public int processEvent(@NonNull final DramaEvent event) {
    if (Boolean.TRUE.equals(event.getIsProcessed())) {
      return 0;
    }

    log.info(
        "Processing drama event: {} - {}",
        LogSanitizer.sanitize(event.getTitle()),
        event.getEventType());

    switch (event.getEventType()) {
      case BACKSTAGE_INCIDENT, PERSONAL_ISSUE, MEDIA_CONTROVERSY -> {
        if (event.getSecondaryWrestler() != null) {
          int heatImpact = event.getSeverity().getHeatImpactRange().getRandomValue(random);
          rivalryService.addHeatBetweenWrestlers(
              event.getPrimaryWrestler().getId(),
              event.getSecondaryWrestler().getId(),
              heatImpact,
              "From drama event: " + event.getTitle());
        }
      }
      case INJURY_INCIDENT -> {
        injuryService.createInjuryFromBumps(
            event.getPrimaryWrestler().getId(), event.getUniverse().getId());
      }
      case OUTCOME_MATRIX_RESULT -> {
        // Effects already applied eagerly in OutcomeMatrixService.applyEffects(); nothing to do.
        log.debug("OUTCOME_MATRIX_RESULT event {} was pre-processed; skipping.", event.getId());
      }
        // Add other cases as needed
    }

    event.setIsProcessed(true);
    event.setProcessedDate(Instant.now(clock));
    dramaEventRepository.save(event);
    return 1;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<DramaEvent> getEventsForWrestler(@NonNull final Long wrestlerId) {
    Wrestler wrestler =
        wrestlerRepository
            .findById(wrestlerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Wrestler not found with id: " + wrestlerId));
    return dramaEventRepository.findByWrestler(wrestler);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<DramaEvent> getEventsForWrestler(
      @NonNull final Long wrestlerId, @NonNull final Pageable pageable) {
    Wrestler wrestler =
        wrestlerRepository
            .findById(wrestlerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Wrestler not found with id: " + wrestlerId));
    return dramaEventRepository.findByWrestler(wrestler, pageable);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<DramaEvent> getRecentEvents() {
    return dramaEventRepository.findRecentEvents(Instant.now(clock).minus(30, ChronoUnit.DAYS));
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<DramaEvent> getEventsBetweenWrestlers(
      @NonNull final Long w1Id, @NonNull final Long w2Id) {
    Wrestler wrestler1 = wrestlerRepository.findById(w1Id).orElseThrow();
    Wrestler wrestler2 = wrestlerRepository.findById(w2Id).orElseThrow();
    return dramaEventRepository.findBetweenWrestlers(wrestler1, wrestler2);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public int getActiveInjuryCount(@NonNull final Long wrestlerId, @NonNull final Long universeId) {
    return injuryService.getActiveInjuriesForWrestler(wrestlerId, universeId).size();
  }

  /**
   * Create a drama event record for an outcome matrix roll result. The event is pre-marked as
   * processed because mechanical effects are applied eagerly by
   * OutcomeMatrixService.applyEffects().
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Optional<DramaEvent> createOutcomeMatrixEvent(
      @NonNull final Long primaryWrestlerId,
      final Long secondaryWrestlerId,
      final Long universeId,
      @NonNull final String title,
      @NonNull final String description,
      @NonNull final DramaEventSeverity severity,
      final Integer heatImpact,
      final boolean injuryCaused) {

    Wrestler primary =
        wrestlerRepository
            .findById(primaryWrestlerId)
            .orElseThrow(
                () -> new EntityNotFoundException("Wrestler not found: " + primaryWrestlerId));

    DramaEvent event = new DramaEvent();
    event.setPrimaryWrestler(primary);

    if (secondaryWrestlerId != null) {
      wrestlerRepository.findById(secondaryWrestlerId).ifPresent(event::setSecondaryWrestler);
    }
    if (universeId != null) {
      universeRepository.findById(universeId).ifPresent(event::setUniverse);
    }

    event.setEventType(DramaEventType.OUTCOME_MATRIX_RESULT);
    event.setSeverity(severity);
    event.setTitle(title);
    event.setDescription(description);
    event.setHeatImpact(heatImpact);
    event.setInjuryCaused(injuryCaused);
    event.setIsProcessed(true);
    event.setProcessedDate(Instant.now(clock));
    event.setProcessingNotes("Applied via OutcomeMatrixService.applyEffects()");
    event.setEventDate(Instant.now(clock));
    event.setCreationDate(Instant.now(clock));

    DramaEvent saved = dramaEventRepository.save(event);
    eventPublisher.publishEvent(new DramaEventCreatedEvent(this, saved));
    return Optional.of(saved);
  }

  /**
   * Delete processed events older than {@code processedRetentionDays} days and unprocessed events
   * older than {@code unprocessedRetentionDays} days.
   */
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SYSTEM')")
  public DramaEventCleanupResult purgeOldEvents(
      final int processedRetentionDays, final int unprocessedRetentionDays) {
    Instant processedCutoff = Instant.now(clock).minus(processedRetentionDays, ChronoUnit.DAYS);
    Instant unprocessedCutoff = Instant.now(clock).minus(unprocessedRetentionDays, ChronoUnit.DAYS);

    int processed = dramaEventRepository.deleteProcessedOlderThan(processedCutoff);
    int unprocessed = dramaEventRepository.deleteUnprocessedOlderThan(unprocessedCutoff);

    log.info(
        "DramaEvent cleanup: deleted {} processed (>{}d), {} unprocessed (>{}d)",
        processed,
        processedRetentionDays,
        unprocessed,
        unprocessedRetentionDays);
    return new DramaEventCleanupResult(processed, unprocessed);
  }

  public record DramaEventCleanupResult(int processedDeleted, int unprocessedDeleted) {}
}

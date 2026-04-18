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
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventRepository;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
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
      DramaEventRepository dramaEventRepository,
      WrestlerRepository wrestlerRepository,
      UniverseRepository universeRepository,
      WrestlerStateRepository wrestlerStateRepository,
      WrestlerService wrestlerService,
      RivalryService rivalryService,
      InjuryService injuryService,
      SecurityUtils securityUtils,
      Clock clock,
      Random random,
      ApplicationEventPublisher eventPublisher) {
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
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<DramaEvent> createDramaEvent(
      @NonNull Long wrestler1Id,
      Long wrestler2Id,
      @NonNull DramaEventType eventType,
      @NonNull DramaEventSeverity severity,
      @NonNull String title,
      @NonNull String description,
      @NonNull Long universeId) {

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

    return Optional.of(dramaEventRepository.save(event));
  }

  /** Generate a random drama event for a wrestler. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<DramaEvent> generateRandomDramaEvent(
      @NonNull Long wrestlerId, @NonNull Long universeId) {
    // Basic implementation for generating random events
    Wrestler wrestler = wrestlerRepository.findById(wrestlerId).orElseThrow();

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
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void processUnprocessedEvents() {
    List<DramaEvent> unprocessed = dramaEventRepository.findByIsProcessedFalseOrderByEventDateAsc();
    for (DramaEvent event : unprocessed) {
      processEvent(event);
    }
  }

  /** Process a specific drama event, applying its effects. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public int processEvent(@NonNull DramaEvent event) {
    if (Boolean.TRUE.equals(event.getIsProcessed())) {
      return 0;
    }

    log.info("Processing drama event: {} - {}", event.getTitle(), event.getEventType());

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
        // Add other cases as needed
    }

    event.setIsProcessed(true);
    event.setProcessedDate(Instant.now(clock));
    dramaEventRepository.save(event);
    return 1;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<DramaEvent> getEventsForWrestler(@NonNull Long wrestlerId) {
    Wrestler wrestler = wrestlerRepository.findById(wrestlerId).orElseThrow();
    return dramaEventRepository.findByWrestler(wrestler);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<DramaEvent> getEventsForWrestler(
      @NonNull Long wrestlerId, @NonNull Pageable pageable) {
    Wrestler wrestler = wrestlerRepository.findById(wrestlerId).orElseThrow();
    return dramaEventRepository.findByWrestler(wrestler, pageable);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<DramaEvent> getRecentEvents() {
    return dramaEventRepository.findRecentEvents(Instant.now(clock).minus(30, ChronoUnit.DAYS));
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<DramaEvent> getEventsBetweenWrestlers(@NonNull Long w1Id, @NonNull Long w2Id) {
    Wrestler wrestler1 = wrestlerRepository.findById(w1Id).orElseThrow();
    Wrestler wrestler2 = wrestlerRepository.findById(w2Id).orElseThrow();
    return dramaEventRepository.findBetweenWrestlers(wrestler1, wrestler2);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public int getActiveInjuryCount(@NonNull Long wrestlerId, @NonNull Long universeId) {
    return injuryService.getActiveInjuriesForWrestler(wrestlerId, universeId).size();
  }
}

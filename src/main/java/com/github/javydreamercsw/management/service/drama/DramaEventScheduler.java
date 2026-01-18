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

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.List;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Scheduler service for automatically generating drama events in the ATW RPG system. This service
 * runs periodically to create random drama events that keep storylines dynamic and engaging.
 *
 * <p>Can be enabled/disabled via application properties: drama.events.scheduler.enabled=true/false
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "drama.events.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class DramaEventScheduler {

  private final DramaEventService dramaEventService;
  private final WrestlerRepository wrestlerRepository;
  private final Random random = new Random();

  /**
   * Generate random drama events every 6 hours by default. This keeps the storylines dynamic by
   * introducing unexpected events that can affect rivalries, fan counts, and wrestler development.
   */
  @Scheduled(fixedRateString = "${drama.events.scheduler.rate:21600000}") // Every 6 hours default
  public void generateRandomDramaEvents() {
    try {
      setSystemAuthentication();
      log.debug("Starting scheduled drama event generation...");

      List<Wrestler> allWrestlers = wrestlerRepository.findAll();
      if (allWrestlers.isEmpty()) {
        log.debug("No wrestlers found, skipping drama event generation");
        return;
      }

      // Generate 0-2 drama events per run (weighted toward fewer events)
      int eventsToGenerate = getRandomEventCount();

      if (eventsToGenerate == 0) {
        log.debug("No drama events scheduled for this run");
        return;
      }

      log.info("Generating {} random drama events", eventsToGenerate);

      for (int i = 0; i < eventsToGenerate; i++) {
        generateSingleRandomEvent(allWrestlers);
      }

      log.info("Completed scheduled drama event generation");

    } catch (Exception e) {
      log.error("Error during scheduled drama event generation", e);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  /**
   * Process unprocessed drama events every 30 minutes. This ensures that drama events have their
   * impacts applied in a timely manner.
   */
  @Scheduled(fixedRate = 1_800_000) // Every 30 minutes (1,800,000 milliseconds)
  public void processUnprocessedEvents() {
    try {
      setSystemAuthentication();
      log.debug("Starting scheduled drama event processing...");

      int processedCount = dramaEventService.processUnprocessedEvents();

      if (processedCount > 0) {
        log.info("Processed {} drama events during scheduled run", processedCount);
      } else {
        log.debug("No unprocessed drama events found");
      }

    } catch (Exception e) {
      log.error("Error during scheduled drama event processing", e);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  /** Weekly drama event summary - logs statistics about drama events from the past week. */
  @Scheduled(cron = "0 0 9 * * MON") // Every Monday at 9 AM
  public void weeklyDramaEventSummary() {
    try {
      setSystemAuthentication();
      log.info("=== WEEKLY DRAMA EVENTS SUMMARY ===");

      List<com.github.javydreamercsw.management.domain.drama.DramaEvent> recentEvents =
          dramaEventService.getRecentEvents();

      if (recentEvents.isEmpty()) {
        log.info("No drama events occurred in the past week");
        return;
      }

      // Count events by type
      var eventTypeCounts =
          recentEvents.stream()
              .collect(
                  java.util.stream.Collectors.groupingBy(
                      com.github.javydreamercsw.management.domain.drama.DramaEvent::getEventType,
                      java.util.stream.Collectors.counting()));

      // Count events by severity
      var severityCounts =
          recentEvents.stream()
              .collect(
                  java.util.stream.Collectors.groupingBy(
                      com.github.javydreamercsw.management.domain.drama.DramaEvent::getSeverity,
                      java.util.stream.Collectors.counting()));

      log.info("Total drama events this week: {}", recentEvents.size());
      log.info("Events by type: {}", eventTypeCounts);
      log.info("Events by severity: {}", severityCounts);

      // Count events that created rivalries or caused injuries
      long rivalriesCreated =
          recentEvents.stream().mapToLong(e -> e.getRivalryCreated() ? 1 : 0).sum();

      long injuriesCaused = recentEvents.stream().mapToLong(e -> e.getInjuryCaused() ? 1 : 0).sum();

      if (rivalriesCreated > 0) {
        log.info("New rivalries created: {}", rivalriesCreated);
      }

      if (injuriesCaused > 0) {
        log.info("Injuries caused by drama: {}", injuriesCaused);
      }

      log.info("=== END WEEKLY SUMMARY ===");

    } catch (Exception e) {
      log.error("Error generating weekly drama event summary", e);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void setSystemAuthentication() {
    var auth =
        new UsernamePasswordAuthenticationToken(
            "system",
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_" + ADMIN_ROLE),
                new SimpleGrantedAuthority("ADMIN")));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  // ==================== PRIVATE HELPER METHODS ====================

  /** Get random number of events to generate (weighted toward fewer events). */
  private int getRandomEventCount() {
    double roll = random.nextDouble();

    // 50% chance of no events
    if (roll < 0.5) return 0;

    // 40% chance of 1 event
    if (roll < 0.9) return 1;

    // 10% chance of 2 events
    return 2;
  }

  /** Generate a single random drama event. */
  private void generateSingleRandomEvent(@NonNull List<Wrestler> allWrestlers) {
    try {
      // Select a random wrestler
      Wrestler randomWrestler = allWrestlers.get(random.nextInt(allWrestlers.size()));

      // Generate the event
      var eventOpt = dramaEventService.generateRandomDramaEvent(randomWrestler.getId());

      if (eventOpt.isPresent()) {
        var event = eventOpt.get();
        log.info("Generated drama event: {} ({})", event.getTitle(), event.getSeverity());
      } else {
        log.warn("Failed to generate drama event for wrestler: {}", randomWrestler.getName());
      }

    } catch (Exception e) {
      log.error("Error generating single drama event", e);
    }
  }
}

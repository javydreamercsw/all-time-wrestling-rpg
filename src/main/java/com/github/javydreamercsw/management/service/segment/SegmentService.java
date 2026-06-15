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
package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.SegmentDTO;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class SegmentService {

  private final SegmentRepository segmentRepository;
  private final TitleRepository titleRepository;
  private final GameSettingService gameSettingService;
  private final LeagueRepository leagueRepository;
  private final LeagueRosterRepository leagueRosterRepository;
  private final MatchFulfillmentRepository matchFulfillmentRepository;
  private final InboxService inboxService;
  private final NewsGenerationService newsGenerationService;
  private final InboxEventType matchRequestEventType;

  @PersistenceContext private EntityManager entityManager;

  @Autowired
  public SegmentService(
      final SegmentRepository segmentRepository,
      final TitleRepository titleRepository,
      final GameSettingService gameSettingService,
      final LeagueRepository leagueRepository,
      final LeagueRosterRepository leagueRosterRepository,
      final MatchFulfillmentRepository matchFulfillmentRepository,
      final InboxService inboxService,
      final NewsGenerationService newsGenerationService,
      @Qualifier("MATCH_REQUEST") final InboxEventType matchRequestEventType) {
    this.segmentRepository = segmentRepository;
    this.titleRepository = titleRepository;
    this.gameSettingService = gameSettingService;
    this.leagueRepository = leagueRepository;
    this.leagueRosterRepository = leagueRosterRepository;
    this.matchFulfillmentRepository = matchFulfillmentRepository;
    this.inboxService = inboxService;
    this.newsGenerationService = newsGenerationService;
    this.matchRequestEventType = matchRequestEventType;
  }

  /**
   * Creates a new match.
   *
   * @param show The show where the match took place
   * @param matchType The type of match
   * @param matchDate The date/time of the match
   * @return The created Segment
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Segment createSegment(
      @NonNull final Show show,
      @NonNull final SegmentType matchType,
      @NonNull final Instant matchDate) {
    return createSegment(show, matchType, matchDate, new HashSet<>());
  }

  /**
   * Creates a new match.
   *
   * @param show The show where the match took place
   * @param matchType The type of match
   * @param matchDate The date/time of the match
   * @param titles The titles contested in this segment
   * @return The created Segment
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Segment createSegment(
      @NonNull final Show show,
      @NonNull final SegmentType matchType,
      @NonNull final Instant matchDate,
      @NonNull final Set<Title> titles) {

    Segment match = new Segment();
    match.setShow(show);
    match.setSegmentType(matchType);
    match.setSegmentDate(matchDate);
    match.setIsTitleSegment(!titles.isEmpty());
    match.setTitles(titles);

    Segment saved = segmentRepository.save(match);
    log.info("Created match with ID: {} for show: {}", saved.getId(), show.getName());
    return saved;
  }

  /**
   * Updates an existing segment based on DTO information.
   *
   * @param id The ID of the segment to update.
   * @param dto The SegmentDTO containing updated information.
   * @return The updated Segment.
   * @throws IllegalArgumentException if the segment with the given ID is not found.
   */
  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or (isAuthenticated() and\
       @segmentService.canUserUpdateSegment(#id))\
      """)
  public Segment updateSegment(@NonNull final Long id, @NonNull final SegmentDTO dto) {
    return segmentRepository
        .findById(id)
        .map(
            existingSegment -> {
              existingSegment.setNarration(dto.getNarration());
              existingSegment.setSegmentDate(dto.getSegmentDate());
              existingSegment.setIsTitleSegment(!dto.getTitleIds().isEmpty());

              // Update titles
              Set<Title> newTitles = new java.util.HashSet<>();
              if (dto.getTitleIds() != null) {
                for (Long titleId : dto.getTitleIds()) {
                  titleRepository.findById(titleId).ifPresent(newTitles::add);
                }
              }
              existingSegment.setTitles(newTitles);

              return segmentRepository.save(existingSegment);
            })
        .orElseThrow(() -> new IllegalArgumentException("Segment not found with ID: " + id));
  }

  /**
   * Updates an existing segment.
   *
   * @param segment The segment to update
   * @return The updated Segment
   */
  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or (isAuthenticated() and\
       @segmentService.canUserUpdateSegment(#segment.id))\
      """)
  public Segment updateSegment(@NonNull final Segment segment) {
    return segmentRepository.save(segment);
  }

  /**
   * Finds a match by ID.
   *
   * @param id The match ID
   * @return Optional containing the Segment if found
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Segment> findById(@NonNull final Long id) {
    return segmentRepository.findById(id);
  }

  /**
   * Finds a match by ID with all details eagerly fetched.
   *
   * @param id The match ID
   * @return Optional containing the Segment if found
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Segment> findByIdWithDetails(@NonNull final Long id) {
    Optional<Segment> result = segmentRepository.findByIdWithDetails(id);
    result.ifPresent(s -> s.getParticipants().forEach(p -> p.getWrestler().getAlignments().size()));
    return result;
  }

  /**
   * Gets all matches with pagination.
   *
   * @param pageable Pagination information
   * @return Page of Segment objects
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Segment> getAllSegments(@NonNull final Pageable pageable) {
    return segmentRepository.findAllBy(pageable);
  }

  /**
   * Gets all matches for a specific show.
   *
   * @param show The show to get matches for
   * @return List of Segment objects for the show
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Segment> getSegmentsByShow(@NonNull final Show show) {
    return segmentRepository.findByShow(show);
  }

  /**
   * Gets all matches where a wrestler participated.
   *
   * @param wrestler The wrestler to search for
   * @return List of Segment objects where the wrestler participated
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Segment> getSegmentsByWrestlerParticipation(
      @NonNull final Wrestler wrestler, @NonNull final Pageable pageable) {
    return segmentRepository.findByWrestlerParticipation(wrestler, pageable);
  }

  /**
   * Gets matches between two specific wrestlers.
   *
   * @param wrestler1 First wrestler
   * @param wrestler2 Second wrestler
   * @return List of Segment objects between the two wrestlers
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Segment> getSegmentsBetween(
      @NonNull final Wrestler wrestler1, @NonNull final Wrestler wrestler2) {
    return segmentRepository.findSegmentsBetween(wrestler1, wrestler2);
  }

  /**
   * Gets all NPC-generated matches.
   *
   * @return List of NPC-generated Segment objects
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Segment> getNpcGeneratedSegments() {
    return segmentRepository.findByIsNpcGeneratedTrue();
  }

  /**
   * Gets all title matches.
   *
   * @return List of title Segment objects
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Segment> getTitleSegments() {
    return segmentRepository.findByIsTitleSegmentTrue();
  }

  /**
   * Gets matches after a specific date.
   *
   * @param date The date to search after
   * @return List of Segment objects after the specified date
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Segment> getSegmentsAfter(@NonNull final Instant date) {
    return segmentRepository.findBySegmentDateAfter(date);
  }

  /**
   * Gets all matches where a wrestler participated within a specific season.
   *
   * @param wrestler The wrestler to search for
   * @param season The season to filter by
   * @return List of Segment objects where the wrestler participated in the given season
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Page<Segment> getSegmentsByWrestlerParticipationAndSeason(
      @NonNull final Wrestler wrestler,
      @NonNull final Season season,
      @NonNull final Pageable pageable) {
    return segmentRepository.findByWrestlerParticipationAndSeason(wrestler, season, pageable);
  }

  /**
   * Counts wins for a wrestler in a specific universe.
   *
   * @param wrestler The wrestler to count wins for
   * @param universeId The universe ID
   * @return Number of wins
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long countWinsByWrestler(
      @NonNull final Wrestler wrestler, @NonNull final Long universeId) {
    return segmentRepository.countWinsByWrestler(wrestler, universeId);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long countLossesByWrestler(
      @NonNull final Wrestler wrestler, @NonNull final Long universeId) {
    return segmentRepository.countLossesByWrestler(wrestler, universeId);
  }

  /**
   * Counts wins for a wrestler.
   *
   * @param wrestler The wrestler to count wins for
   * @return Number of wins
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long countWinsByWrestler(@NonNull final Wrestler wrestler) {
    return segmentRepository.countWinsByWrestler(wrestler);
  }

  @PreAuthorize("isAuthenticated()")
  public long countSegmentsByWrestler(final Wrestler wrestler) {
    return segmentRepository.countSegmentsByWrestler(wrestler);
  }

  @PreAuthorize("isAuthenticated()")
  public long countSegmentsByWrestlerAndSeason(final Wrestler wrestler, final Season season) {
    return segmentRepository.countByWrestlerParticipationAndSeason(wrestler, season);
  }

  /**
   * Deletes a match.
   *
   * @param id The ID of the match to delete
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public void deleteSegment(@NonNull final Long id) {
    segmentRepository.deleteById(id);
    log.info("Deleted match with ID: {}", id);
  }

  /**
   * Checks if a match exists by external ID.
   *
   * @param externalId The external ID to check
   * @return true if a match with the external ID exists
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public boolean existsByExternalId(@NonNull final String externalId) {
    return segmentRepository.existsByExternalId(externalId);
  }

  /**
   * Finds a match by external ID.
   *
   * @param externalId The external ID to search for
   * @return Optional containing the Segment if found
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Segment> findByExternalId(@NonNull final String externalId) {
    return segmentRepository.findByExternalId(externalId);
  }

  /**
   * Gets all external IDs of all matches.
   *
   * @return List of all external IDs.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<String> getAllExternalIds() {
    return segmentRepository.findAllExternalIds();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Segment> getUpcomingSegmentsForWrestler(
      @NonNull final Wrestler wrestler, final int limit) {
    LocalDate referenceDate = gameSettingService.getCurrentGameDate();
    Pageable pageable = PageRequest.of(0, limit);
    return segmentRepository.findUpcomingSegmentsForWrestler(wrestler, referenceDate, pageable);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Segment saveSegment(@NonNull final Segment segment) {
    boolean isNew = segment.getId() == null;
    Segment saved = segmentRepository.save(segment);

    if (isNew) {
      // Check for league match and send notifications
      checkAndNotifyLeagueMatch(saved);
    } else if (saved.getAdjudicationStatus()
        == com.github.javydreamercsw.management.domain.AdjudicationStatus.ADJUDICATED) {
      // Generate news for completed matches
      newsGenerationService.generateNewsForSegment(saved);
    }
    return saved;
  }

  private void checkAndNotifyLeagueMatch(final Segment segment) {
    if (segment.getShow() == null) {
      return;
    }

    // Reload show within this transaction to guarantee lazy associations are accessible;
    // fall back to the in-memory instance when the show has not been persisted yet (e.g. tests).
    Show show = segment.getShow();
    if (show.getId() != null) {
      Show reloaded = entityManager.find(Show.class, show.getId());
      if (reloaded != null) {
        show = reloaded;
      }
    }

    League league = show.getLeague();
    if (league == null && show.getUniverse() != null) {
      league = leagueRepository.findByUniverse(show.getUniverse()).orElse(null);
    }

    if (league != null) {
      for (Wrestler wrestler : segment.getWrestlers()) {
        notifyLeagueParticipant(segment, show, wrestler, league);
      }
    }
  }

  private void notifyLeagueParticipant(
      final Segment segment, final Show show, final Wrestler wrestler, final League league) {
    leagueRosterRepository
        .findByLeagueAndWrestler(league, wrestler)
        .ifPresent(
            roster -> {
              // If wrestler is owned by a player (not commissioner), track fulfillment
              if (!roster.getOwner().equals(league.getCommissioner())) {
                MatchFulfillment fulfillment =
                    matchFulfillmentRepository
                        .findBySegment(segment)
                        .orElse(new MatchFulfillment());

                if (fulfillment.getId() == null) {
                  fulfillment.setSegment(segment);
                  fulfillment.setLeague(league);
                  fulfillment.setStatus(MatchFulfillment.FulfillmentStatus.PENDING_RESULTS);
                  matchFulfillmentRepository.save(fulfillment);

                  // Send Notification to the owner
                  InboxItem inboxItem =
                      inboxService.createInboxItem(
                          matchRequestEventType,
                          "Pending match on show: "
                              + show.getName()
                              + " for wrestler: "
                              + wrestler.getName(),
                          List.of(
                              new InboxService.TargetInfo(
                                  roster.getOwner().getId().toString(),
                                  InboxItemTarget.TargetType.ACCOUNT),
                              new InboxService.TargetInfo(
                                  fulfillment.getId().toString(),
                                  InboxItemTarget.TargetType.MATCH_FULFILLMENT),
                              new InboxService.TargetInfo(
                                  wrestler.getId().toString(),
                                  InboxItemTarget.TargetType.WRESTLER)));
                  inboxItem.setActionType("MATCH_REPORT");
                  inboxItem.setActionPayload("{\"fulfillmentId\":\"" + fulfillment.getId() + "\"}");
                  inboxService.save(inboxItem);
                }
              }
            });
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public void addParticipant(@NonNull final Segment segment, @NonNull final Wrestler wrestler) {
    segment.addParticipant(wrestler);
    segmentRepository.save(segment);

    Show show = segment.getShow();
    if (show != null && show.getUniverse() != null) {
      checkAndNotifyLeagueMatch(segment);
    }
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public void setWinner(@NonNull final Segment segment, @NonNull final Wrestler winner) {
    segment.setWinners(List.of(winner));
    segmentRepository.save(segment);
  }

  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public void setAdjudicationStatus(
      @NonNull final Segment segment,
      @NonNull final com.github.javydreamercsw.management.domain.AdjudicationStatus status) {
    segment.setAdjudicationStatus(status);
    segmentRepository.save(segment);
  }
}

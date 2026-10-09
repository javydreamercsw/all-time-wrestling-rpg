/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityRepository;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.BriefcaseCashedInEvent;
import com.github.javydreamercsw.management.event.BriefcaseGrantedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Awards and redeems Money in the Bank-style briefcases (ATW-8p72). A briefcase is a {@link
 * com.github.javydreamercsw.management.domain.title.TitleOpportunity}: a held, cashable title shot
 * earned by winning a briefcase-deciding tournament (e.g. "Time Vault"). The holder may cash it in
 * exactly once, at any time, for a title match against the reigning champion(s) of any active
 * championship — tier is deliberately not checked (the briefcase is the credential); universe,
 * gender, and champion-existence are.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TitleOpportunityService {

  private final TitleOpportunityRepository opportunityRepository;
  private final TournamentRepository tournamentRepository;
  private final WrestlerRepository wrestlerRepository;
  private final TitleRepository titleRepository;
  private final TitleReignRepository titleReignRepository;
  private final ShowRepository showRepository;
  private final SegmentTypeRepository segmentTypeRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final NPCSegmentResolutionService segmentResolutionService;
  private final GameSettingService gameSettingService;
  private final Clock clock;
  private final ApplicationEventPublisher eventPublisher;

  // ── Grant ────────────────────────────────────────────────────────────────

  /**
   * Awards a briefcase to a tournament winner. Idempotent: at most one opportunity per awarding
   * tournament, and a wrestler already holding one keeps the older case (a second live win is not
   * granted).
   *
   * @param tournament the briefcase-deciding tournament that just completed
   * @param winner the tournament winner
   * @return the created opportunity, or empty when the grant was skipped (already granted, or the
   *     winner already holds one)
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Optional<TitleOpportunity> grantFromTournament(Tournament tournament, Wrestler winner) {
    if (tournament.getId() != null
        && opportunityRepository.existsByEarnedFromTournamentId(tournament.getId())) {
      log.info(
          "Tournament '{}' already granted a briefcase — grant skipped (idempotent)",
          tournament.getName());
      return Optional.empty();
    }
    if (winner.getId() != null
        && opportunityRepository
            .findFirstByWrestlerIdAndStatus(winner.getId(), TitleOpportunityStatus.HELD)
            .isPresent()) {
      log.info(
          "Wrestler '{}' already holds a briefcase — grant from '{}' skipped",
          winner.getName(),
          tournament.getName());
      return Optional.empty();
    }
    LocalDate earnedAt = tournament.getEndDate() != null ? tournament.getEndDate() : gameDate();
    TitleOpportunity opportunity = new TitleOpportunity();
    opportunity.setName(tournament.getName() + " briefcase");
    opportunity.setStatus(TitleOpportunityStatus.HELD);
    opportunity.setWrestler(winner);
    opportunity.setUniverse(tournament.getUniverse());
    opportunity.setGender(tournament.getGender());
    opportunity.setEarnedAt(earnedAt);
    opportunity.setEarnedFromTournament(tournament);
    opportunity.setExpiryDate(earnedAt.plusDays(gameSettingService.getBriefcaseExpiryDays()));
    opportunity = opportunityRepository.save(opportunity);
    log.info(
        "Granted '{}' to {} (earned {}, expires {})",
        opportunity.getName(),
        winner.getName(),
        earnedAt,
        opportunity.getExpiryDate());
    eventPublisher.publishEvent(new BriefcaseGrantedEvent(this, opportunity));
    return Optional.of(opportunity);
  }

  // ── Cash in ──────────────────────────────────────────────────────────────

  /**
   * Cashes a held briefcase in against the reigning champion(s) of the chosen championship: books a
   * title match on the chosen show and spends the case immediately (the WWE rule — a failed cash-in
   * still spends it; the match outcome decides the championship, not the case).
   *
   * @param opportunityId the held briefcase
   * @param titleId the championship to challenge (must have a reigning champion)
   * @param showId the show the cash-in match books onto
   * @return the booked cash-in segment
   */
  @Transactional
  @PreAuthorize(
      """
      hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or\
       @permissionService.isOwner(#opportunityId, 'TitleOpportunity')\
      """)
  public Segment cashIn(long opportunityId, long titleId, long showId) {
    TitleOpportunity opportunity =
        opportunityRepository
            .findById(opportunityId)
            .orElseThrow(
                () -> new IllegalArgumentException("Briefcase not found: " + opportunityId));
    if (!opportunity.isHeld()) {
      throw new IllegalStateException(
          "This briefcase is not cashable — its status is " + opportunity.getStatus());
    }
    LocalDate gameDate = gameDate();
    if (opportunity.isExpired(gameDate)) {
      opportunity.markExpired();
      opportunityRepository.save(opportunity);
      throw new IllegalStateException(
          "This briefcase expired on "
              + opportunity.getExpiryDate()
              + " and can no longer be"
              + " cashed in");
    }
    Wrestler holder = opportunity.getWrestler();
    if (holder.getId() == null || !Boolean.TRUE.equals(holder.getActive())) {
      throw new IllegalStateException(
          holder.getName()
              + " is no longer an active wrestler — the briefcase cannot be cashed"
              + " in");
    }
    Title title =
        titleRepository
            .findById(titleId)
            .orElseThrow(() -> new IllegalArgumentException("Title not found: " + titleId));
    if (!Boolean.TRUE.equals(title.getIsActive())) {
      throw new IllegalStateException(
          "'" + title.getName() + "' is retired and cannot be challenged");
    }
    if (title.getUniverse() != null
        && opportunity.getUniverse() != null
        && !title.getUniverse().getId().equals(opportunity.getUniverse().getId())) {
      throw new IllegalStateException(
          "'" + title.getName() + "' belongs to a different universe than the briefcase");
    }
    if (title.getGender() != null
        && holder.getGender() != null
        && title.getGender() != holder.getGender()) {
      throw new IllegalStateException(
          holder.getName()
              + " cannot cash in for '"
              + title.getName()
              + "' — it is a "
              + title.getGender()
              + " championship");
    }
    // A gendered briefcase is a division credential: it challenges its own division's titles
    // (or an ungendered title), never the other division's (ATW-hq8d).
    if (opportunity.getGender() != null
        && title.getGender() != null
        && opportunity.getGender() != title.getGender()) {
      throw new IllegalStateException(
          "'"
              + opportunity.getName()
              + "' is a "
              + opportunity.getGender()
              + " division briefcase and cannot be cashed in for '"
              + title.getName()
              + "' — it is a "
              + title.getGender()
              + " championship");
    }
    // The champion check reads the reign table, not the detached title's in-memory champion list
    // (the same lazy-collection trap TournamentService.currentChampionsOf avoids).
    List<Wrestler> champions =
        titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()).stream()
            .flatMap(reign -> reign.getChampions().stream())
            .distinct()
            .toList();
    if (champions.isEmpty()) {
      throw new IllegalStateException(
          "'"
              + title.getName()
              + "' is vacant — a briefcase needs a reigning champion to cash in"
              + " against");
    }
    Show show =
        showRepository
            .findById(showId)
            .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));

    // Book the cash-in match: champion(s) vs holder, title on the line — the same shape as the
    // tournament champion showcase. Adjudication's applyTitleChange handles win/loss from here.
    SegmentType segmentType =
        segmentTypeRepository
            .findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "One on One segment type is missing — cannot book a cash-in"));
    SegmentRule noRule = null; // stipulation resolved as "Standard Match" by the resolver
    Segment segment =
        segmentResolutionService.resolveTeamSegment(
            new SegmentTeam(champions, title.getName() + " Champion"),
            new SegmentTeam(holder),
            segmentType,
            show,
            stipulationOf(noRule));
    segment.setIsTitleSegment(true);
    segment.getTitles().add(title);
    segment.setNarration(
        "Briefcase cash-in: "
            + holder.getName()
            + " cashes in '"
            + opportunity.getName()
            + "' for a "
            + title.getName()
            + " opportunity against the reigning champion"
            + (champions.size() > 1 ? "s" : "")
            + ". The match was booked on the spot — narrate the surprise arrival and the"
            + " champion's reaction.");

    opportunity.markCashedIn(title, segment, gameDate);
    opportunityRepository.save(opportunity);
    eventPublisher.publishEvent(new BriefcaseCashedInEvent(this, opportunity));
    log.info(
        "Cashed in '{}' on show '{}': {} vs {} for {}",
        opportunity.getName(),
        show.getName(),
        champions.stream().map(Wrestler::getName).collect(Collectors.joining(" & ")),
        holder.getName(),
        title.getName());
    return segment;
  }

  // ── Expiry ───────────────────────────────────────────────────────────────

  /**
   * Flips every held briefcase past its expiry date to EXPIRED. Called from a daily schedule; cash
   * in also re-checks expiry lazily so correctness never depends on the scheduler firing.
   *
   * @return how many opportunities were expired
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public int expireOverdue() {
    List<TitleOpportunity> overdue =
        opportunityRepository.findByStatusAndExpiryDateBefore(
            TitleOpportunityStatus.HELD, gameDate());
    overdue.forEach(
        opportunity -> {
          opportunity.markExpired();
          opportunityRepository.save(opportunity);
          log.info("Briefcase '{}' expired unspent", opportunity.getName());
        });
    return overdue.size();
  }

  // ── Reads ────────────────────────────────────────────────────────────────

  /** The holder's briefcase history, newest first (career view). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<TitleOpportunity> findByWrestler(Long wrestlerId) {
    return opportunityRepository.findByWrestlerIdOrderByEarnedAtDesc(wrestlerId);
  }

  /** The wrestler's current held briefcase, if any. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<TitleOpportunity> findHeldByWrestler(Long wrestlerId) {
    return opportunityRepository.findFirstByWrestlerIdAndStatus(
        wrestlerId, TitleOpportunityStatus.HELD);
  }

  /** Every currently HELD briefcase, oldest first (booker dashboard panel, ATW-3fhh). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<TitleOpportunity> findHeld() {
    return opportunityRepository.findByStatus(TitleOpportunityStatus.HELD);
  }

  private LocalDate gameDate() {
    return gameSettingService.getCurrentGameDate();
  }

  private String stipulationOf(@Nullable SegmentRule rule) {
    return rule != null ? rule.getName() : "";
  }
}

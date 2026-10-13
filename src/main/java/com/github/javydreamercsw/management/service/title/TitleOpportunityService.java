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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.title.BriefcaseCashInMode;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityRepository;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.event.BriefcaseCashedInEvent;
import com.github.javydreamercsw.management.event.BriefcaseGrantedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
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
  private final SegmentRepository segmentRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final NPCSegmentResolutionService segmentResolutionService;
  private final WrestlerService wrestlerService;
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

  // ── Admin CRUD (ATW-jpki) ────────────────────────────────────────────────

  /**
   * Manually creates a HELD briefcase (booker compensation case, custom prizes). Validates the same
   * invariants {@link #grantFromTournament} enforces: one HELD case per wrestler, active holder.
   * Earned-at may not be in the kayfabe future (after the game's current date) — in-universe
   * fiction is anchored to the game clock, not the wall clock.
   *
   * @param expiryDate explicit cashable-until date, or null to default to earnedAt + the configured
   *     expiry window
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TitleOpportunity adminCreate(
      @NonNull String name,
      @NonNull Wrestler holder,
      @Nullable Universe universe,
      @Nullable Gender gender,
      @NonNull LocalDate earnedAt,
      @Nullable LocalDate expiryDate,
      @Nullable String imageUrl) {
    if (holder.getId() == null || !Boolean.TRUE.equals(holder.getActive())) {
      throw new IllegalArgumentException("Briefcase holder must be an active wrestler");
    }
    if (opportunityRepository
        .findFirstByWrestlerIdAndStatus(holder.getId(), TitleOpportunityStatus.HELD)
        .isPresent()) {
      throw new IllegalArgumentException(
          holder.getName() + " already holds a briefcase — only one HELD case per wrestler");
    }
    if (earnedAt.isAfter(gameDate())) {
      throw new IllegalArgumentException("Earned-at date cannot be in the future: " + earnedAt);
    }
    TitleOpportunity opportunity = new TitleOpportunity();
    opportunity.setName(name);
    opportunity.setStatus(TitleOpportunityStatus.HELD);
    opportunity.setWrestler(holder);
    opportunity.setUniverse(universe);
    opportunity.setGender(gender);
    opportunity.setEarnedAt(earnedAt);
    opportunity.setExpiryDate(expiryDate != null ? expiryDate : earnedAt.plusDays(expiryDays()));
    opportunity.setImageUrl(imageUrl);
    opportunity = opportunityRepository.save(opportunity);
    log.info(
        "Admin created briefcase '{}' for {} (earned {}, expires {})",
        name,
        holder.getName(),
        earnedAt,
        opportunity.getExpiryDate());
    return opportunity;
  }

  /**
   * Edits a live (HELD) case's details: name, expiry override, image, earned-at correction.
   * CASHED_IN/EXPIRED/VOIDED rows are history and stay immutable.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TitleOpportunity adminUpdate(
      @NonNull Long id,
      @NonNull String name,
      @Nullable LocalDate expiryDate,
      @Nullable String imageUrl,
      @Nullable LocalDate earnedAt) {
    TitleOpportunity opportunity = loadEditable(id);
    opportunity.setName(name);
    if (expiryDate != null) {
      opportunity.setExpiryDate(expiryDate);
    }
    opportunity.setImageUrl(imageUrl);
    if (earnedAt != null) {
      if (earnedAt.isAfter(gameDate())) {
        throw new IllegalArgumentException("Earned-at date cannot be in the future: " + earnedAt);
      }
      opportunity.setEarnedAt(earnedAt);
    }
    TitleOpportunity saved = opportunityRepository.save(opportunity);
    log.info("Admin updated briefcase {}: '{}'", id, saved.getName());
    return saved;
  }

  /**
   * Reassigns a HELD case to a different holder (the ATW-jpki dialog's wrestler picker). Enforces
   * the same invariants as {@link #adminCreate}: active wrestler, one HELD case per wrestler.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TitleOpportunity adminUpdateHolder(@NonNull Long id, @NonNull Long newHolderId) {
    TitleOpportunity opportunity = loadEditable(id);
    if (opportunity.getWrestler() != null
        && newHolderId.equals(opportunity.getWrestler().getId())) {
      return opportunity; // same holder — nothing to do
    }
    Wrestler newHolder =
        wrestlerRepository
            .findById(newHolderId)
            .orElseThrow(() -> new IllegalArgumentException("Wrestler not found: " + newHolderId));
    if (!Boolean.TRUE.equals(newHolder.getActive())) {
      throw new IllegalArgumentException("Briefcase holder must be an active wrestler");
    }
    opportunityRepository
        .findFirstByWrestlerIdAndStatus(newHolderId, TitleOpportunityStatus.HELD)
        .ifPresent(
            existing -> {
              throw new IllegalArgumentException(
                  newHolder.getName()
                      + " already holds a briefcase — only one HELD case per"
                      + " wrestler");
            });
    opportunity.setWrestler(newHolder);
    TitleOpportunity saved = opportunityRepository.save(opportunity);
    log.info("Admin reassigned briefcase {} to {}", id, newHolder.getName());
    return saved;
  }

  /** Changes the division (gender filter) of a HELD case — which titles it can cash in against. */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TitleOpportunity adminUpdateDivision(@NonNull Long id, @Nullable Gender gender) {
    TitleOpportunity opportunity = loadEditable(id);
    if (opportunity.getGender() == gender) {
      return opportunity; // unchanged — don't save (null means "leave as-is" for the dialog)
    }
    opportunity.setGender(gender);
    TitleOpportunity saved = opportunityRepository.save(opportunity);
    log.info("Admin changed briefcase {} division to {}", id, gender);
    return saved;
  }

  /**
   * Manually cancels a HELD case (mistaken grant, unwanted prize) — HELD → VOIDED only. History
   * rows are never edited and nothing is ever deleted.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void adminVoid(@NonNull Long id) {
    TitleOpportunity opportunity = loadEditable(id);
    opportunity.setStatus(TitleOpportunityStatus.VOIDED);
    opportunityRepository.save(opportunity);
    log.info("Admin voided briefcase {} ('{}')", id, opportunity.getName());
  }

  /** Loads a live case for editing — only HELD rows are mutable. */
  private TitleOpportunity loadEditable(Long id) {
    TitleOpportunity opportunity =
        opportunityRepository
            .findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Briefcase not found: " + id));
    if (!opportunity.isHeld()) {
      throw new IllegalArgumentException(
          "Briefcase "
              + id
              + " is "
              + opportunity.getStatus()
              + " — only HELD cases can be edited or voided");
    }
    return opportunity;
  }

  private int expiryDays() {
    return gameSettingService.getBriefcaseExpiryDays();
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
    Title title = validateCashIn(opportunity, titleId);
    // The champion check reads the reign table, not the detached title's in-memory champion list
    // (the same lazy-collection trap TournamentService.currentChampionsOf avoids).
    List<Wrestler> champions =
        titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()).stream()
            .flatMap(reign -> reign.getChampions().stream())
            .distinct()
            .toList();
    Show show =
        showRepository
            .findById(showId)
            .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));

    // Book the cash-in match: champion(s) vs holder, title on the line — the same shape as the
    // tournament champion showcase. Adjudication's applyTitleChange handles win/loss from here.
    SegmentTeam championTeam = new SegmentTeam(champions, title.getName() + " Champion");
    Segment segment =
        bookCashIn(
            opportunity,
            title,
            show,
            championTeam,
            champions,
            " The match was booked on the spot — narrate the surprise arrival and the champion's"
                + " reaction.");
    opportunity.setCashInMode(BriefcaseCashInMode.PLANNED);
    opportunityRepository.save(opportunity);
    return segment;
  }

  /**
   * Advisor-path ambush booking (ATW-2b9u): books the holder's cash-in as a segment placed right
   * after the champion's match on the SAME card, before either has been adjudicated. No wear is
   * applied here — nothing is adjudicated yet; {@link #applyAmbushWearIfApplicable} carries the
   * champion's wear over later, when this segment itself adjudicates. All standard cash-in
   * validations apply (HELD, expiry, holder, title eligibility, reigning champion).
   *
   * @param opportunityId the held briefcase
   * @param titleId the championship to challenge
   * @param showId the show being approved
   * @param championSegment the champion's (still pending) match on this card
   * @return the booked ambush segment
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Segment bookAmbush(
      long opportunityId, long titleId, long showId, @NonNull Segment championSegment) {
    Show show =
        showRepository
            .findById(showId)
            .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));
    if (championSegment.getShow() == null
        || !championSegment.getShow().getId().equals(show.getId())) {
      throw new IllegalArgumentException(
          "The champion's segment is not on the same show — an ambush targets a match"
              + " on the same card");
    }
    if (WellKnownSegmentType.PROMO.matches(championSegment.getSegmentType())) {
      throw new IllegalStateException(
          "The champion's earlier segment is a promo — no ambush after a promo");
    }
    TitleOpportunity opportunity =
        opportunityRepository
            .findById(opportunityId)
            .orElseThrow(
                () -> new IllegalArgumentException("Briefcase not found: " + opportunityId));
    Title title = validateCashIn(opportunity, titleId);
    List<Wrestler> champions =
        titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()).stream()
            .flatMap(reign -> reign.getChampions().stream())
            .distinct()
            .toList();
    if (champions.isEmpty()) {
      throw new IllegalStateException(
          "'" + title.getName() + "' is vacant — no champion to ambush");
    }

    SegmentTeam championTeam = new SegmentTeam(champions, title.getName() + " Champion");
    Segment segment =
        bookCashIn(
            opportunity,
            title,
            show,
            championTeam,
            champions,
            " The ambush was booked on the card right after the champion's match — narrate the"
                + " chaos when it plays out.");
    opportunity.setCashInMode(BriefcaseCashInMode.AMBUSH);
    opportunityRepository.save(opportunity);
    segment.setNarration(
        "Briefcase ambush: "
            + opportunity.getWrestler().getName()
            + " blindsides the "
            + title.getName()
            + " champion"
            + (champions.size() > 1 ? "s" : "")
            + " right after their match, cashing in '"
            + opportunity.getName()
            + "'. The champion is worn down from their earlier fight — narrate the chaos.");
    return segment;
  }

  /** Flat breather the incumbents recover when a run-in interrupts their match (ATW-qsm9). */
  static final int RUN_IN_BREATHER = 3;

  /**
   * Mid-match run-in cash-in (ATW-qsm9): the holder interrupts a title match IN PROGRESS — the
   * encounter becomes a 3-team free-for-all (champion / challenger-if-any / fresh holder). The
   * incumbents recover a flat breather from where the encounter left off (their recorded {@code
   * finalHealth} minus {@link #RUN_IN_BREATHER} feeds each incumbent team's wear penalty); momentum
   * is per-match so nothing resets. The winner takes the title regardless of who they pin —
   * adjudication's applyTitleChange already handles a winners list with no champion.
   *
   * <p>Trigger: a wrestler reached HALF their starting HP in the interrupted encounter (the
   * tabletop check, mirrored by MatchView's prompt). All standard cash-in validations apply; the
   * case is spent at booking as usual.
   *
   * @param opportunityId the held briefcase
   * @param titleId the championship on the line in the interrupted match
   * @param showId the show the interrupted match belongs to
   * @param interruptedSegment the in-progress title match (final health already recorded)
   * @return the booked run-in segment (3-team FFA, title on the line)
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Segment bookRunIn(
      long opportunityId, long titleId, long showId, @NonNull Segment interruptedSegment) {
    if (WellKnownSegmentType.PROMO.matches(interruptedSegment.getSegmentType())) {
      throw new IllegalStateException("A run-in interrupts a match — not a promo");
    }
    Show show =
        showRepository
            .findById(showId)
            .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));
    if (interruptedSegment.getShow() == null
        || !interruptedSegment.getShow().getId().equals(show.getId())) {
      throw new IllegalArgumentException(
          "The interrupted match is not on the same show — a run-in targets a match on"
              + " this card");
    }
    TitleOpportunity opportunity =
        opportunityRepository
            .findById(opportunityId)
            .orElseThrow(
                () -> new IllegalArgumentException("Briefcase not found: " + opportunityId));
    Title title = validateCashIn(opportunity, titleId);

    // The interrupted match's participants split into incumbent teams: the champion side and the
    // challenger side (any participant who is not a current champion). The champion side must be
    // present — a run-in targets the title holder's fight.
    List<Wrestler> champions =
        titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()).stream()
            .flatMap(reign -> reign.getChampions().stream())
            .distinct()
            .toList();
    if (champions.isEmpty()) {
      throw new IllegalStateException(
          "'" + title.getName() + "' is vacant — no match to run in on");
    }
    Set<Long> championIds = champions.stream().map(Wrestler::getId).collect(Collectors.toSet());
    List<Wrestler> incumbentChampions =
        interruptedSegment.getWrestlers().stream()
            .filter(w -> w.getId() != null && championIds.contains(w.getId()))
            .toList();
    if (incumbentChampions.isEmpty()) {
      throw new IllegalStateException(
          "The interrupted match does not involve the reigning "
              + title.getName()
              + " champion — nothing to run in on");
    }
    List<Wrestler> challengers =
        interruptedSegment.getWrestlers().stream()
            .filter(w -> w.getId() != null && !championIds.contains(w.getId()))
            .toList();
    Wrestler holder = opportunity.getWrestler();

    // The incumbents recover a flat breather from where the encounter left off: their recorded
    // final health (+3) feeds each incumbent team's wear penalty. The holder enters fresh.
    SegmentTeam championTeam = new SegmentTeam(incumbentChampions, title.getName() + " Champion");
    championTeam.setExtraWearPenalty(incumbentWear(incumbentChampions, interruptedSegment));
    List<SegmentTeam> teams = new ArrayList<>();
    teams.add(championTeam);
    if (!challengers.isEmpty()) {
      SegmentTeam challengerTeam = new SegmentTeam(challengers, "Challenger");
      challengerTeam.setExtraWearPenalty(incumbentWear(challengers, interruptedSegment));
      teams.add(challengerTeam);
    }
    teams.add(new SegmentTeam(holder));

    SegmentType segmentType =
        segmentTypeRepository
            .findByCode(WellKnownSegmentType.FREE_FOR_ALL.getCode())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Free-for-All segment type is missing — cannot book a run-in"
                            + " cash-in"));
    SegmentRule noRule = null; // stipulation resolved as "Standard Match" by the resolver
    Segment segment =
        segmentResolutionService.resolveMultiTeamSegment(
            teams, segmentType, show, stipulationOf(noRule));
    segment.setIsTitleSegment(true);
    segment.getTitles().add(title);
    segment.setNarration(
        "Briefcase run-in: "
            + holder.getName()
            + " storms the ring mid-match and cashes in '"
            + opportunity.getName()
            + "', turning the "
            + title.getName()
            + " title fight into a free-for-all. The incumbent"
            + (incumbentChampions.size() > 1 ? "s have" : " has")
            + " already been through a war — narrate the chaos and who survives it.");
    opportunity.setCashInMode(BriefcaseCashInMode.RUN_IN);
    opportunity.markCashedIn(title, segment, gameDate());
    opportunityRepository.save(opportunity);
    eventPublisher.publishEvent(new BriefcaseCashedInEvent(this, opportunity));
    log.info(
        "Run-in cash-in of '{}' booked on show '{}': {} interrupts {} for the {}",
        opportunity.getName(),
        show.getName(),
        holder.getName(),
        interruptedSegment.getWrestlers().stream()
            .map(Wrestler::getName)
            .collect(Collectors.joining(" & ")),
        title.getName());
    return segment;
  }

  /**
   * The incumbents' wear penalty from the interrupted encounter: percent of starting health lost
   * from their recorded final health, minus the flat run-in breather, floored at 0. Wrestlers with
   * no recorded health (NPC) contribute nothing.
   */
  private int incumbentWear(List<Wrestler> incumbents, Segment interruptedSegment) {
    int wear = 0;
    for (Wrestler incumbent : incumbents) {
      Integer finalHealth =
          interruptedSegment.getParticipants().stream()
              .filter(
                  p -> p.getWrestler() != null && incumbent.getId().equals(p.getWrestler().getId()))
              .map(SegmentParticipant::getFinalHealth)
              .filter(Objects::nonNull)
              .findFirst()
              .orElse(null);
      if (finalHealth == null) {
        continue;
      }
      int startingHealth = Math.max(1, incumbent.getStartingHealth());
      int lossPercent = Math.max(0, (startingHealth - finalHealth) * 100 / startingHealth);
      wear += Math.max(0, lossPercent / 10 - RUN_IN_BREATHER);
    }
    return wear;
  }

  /**
   * Shared booking tail for the standard cash-in and the post-match ambush (ATW-p8ij): books the
   * title segment with the (possibly wear-penalized) champion team, spends the case, and publishes
   * the event. Callers have already run every validation — a failure past this point is a bug.
   */
  private Segment bookCashIn(
      TitleOpportunity opportunity,
      Title title,
      Show show,
      SegmentTeam championTeam,
      List<Wrestler> champions,
      String narrationTail) {
    Wrestler holder = opportunity.getWrestler();
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
            championTeam, new SegmentTeam(holder), segmentType, show, stipulationOf(noRule));
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
            + "."
            + narrationTail);

    opportunity.markCashedIn(title, segment, gameDate());
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

  // ── Post-match ambush (ATW-p8ij) ─────────────────────────────────────────

  /** Flat breather the champion recovers between their match and the ambush (ATW-p8ij). */
  static final int AMBUSH_BREATHER = 2;

  /**
   * Carries the champion's same-show wear into an advisor-booked ambush at resolution time
   * (ATW-2b9u). The ambush was booked before either segment was adjudicated, so the wear math
   * (champion's final health → extra team penalty, breather subtracted) runs here, when the ambush
   * segment itself adjudicates — by then the champion's earlier match is ADJUDICATED (the batch
   * loop runs in segment order). No-op for non-ambush segments; logs and skips when the champion's
   * match has no wear to carry (NPC matches report no final health) or was never adjudicated.
   */
  public void applyAmbushWearIfApplicable(@NonNull Segment segment) {
    if (segment.getId() == null) {
      return;
    }
    TitleOpportunity opportunity =
        opportunityRepository.findByCashedAtSegmentId(segment.getId()).stream()
            .filter(opp -> opp.getCashInMode() == BriefcaseCashInMode.AMBUSH)
            .findFirst()
            .orElse(null);
    if (opportunity == null) {
      return;
    }
    Title title = opportunity.getCashedAgainstTitle();
    if (title == null) {
      return;
    }
    try {
      List<Wrestler> champions =
          titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()).stream()
              .flatMap(reign -> reign.getChampions().stream())
              .distinct()
              .toList();
      Segment championSegment =
          segmentRepository.findByShowOrderBySegmentOrderAsc(segment.getShow()).stream()
              .filter(
                  candidate ->
                      candidate.getId() != null
                          && !candidate.getId().equals(segment.getId())
                          && candidate.getSegmentOrder() < segment.getSegmentOrder()
                          && !WellKnownSegmentType.PROMO.matches(candidate.getSegmentType())
                          && candidate.getAdjudicationStatus() == AdjudicationStatus.ADJUDICATED
                          && candidate.getWrestlers().stream()
                              .anyMatch(
                                  w ->
                                      champions.stream()
                                          .anyMatch(c -> c.getId().equals(w.getId()))))
              .reduce((first, second) -> second) // highest order
              .orElse(null);
      if (championSegment == null) {
        log.info(
            "Ambush segment {}: the champion's earlier match is not adjudicated yet — wear"
                + " carry-over skipped",
            segment.getId());
        return;
      }
      int wearPenalty = computeChampionWear(champions, championSegment);
      if (wearPenalty <= 0) {
        log.info(
            "Ambush segment {}: the champion showed no wear — no ambush advantage applied",
            segment.getId());
        return;
      }
      // Apply as extra wear-and-tear on the champions' condition: the same channel every
      // match uses, scaled like the player-reported path (10% HP loss ≈ 1 condition point).
      // The state is managed inside the caller's adjudication transaction — no explicit save
      // (the same pattern applyWearAndTear relies on).
      Long universeId =
          segment.getShow().getUniverse() != null ? segment.getShow().getUniverse().getId() : 1L;
      for (Wrestler champion : champions) {
        WrestlerState state = wrestlerService.getOrCreateState(champion.getId(), universeId);
        int newCondition = Math.max(0, state.getPhysicalCondition() - wearPenalty);
        state.setPhysicalCondition(newCondition);
        log.info(
            "Ambush wear applied to {}: -{} condition points (new condition {})",
            champion.getName(),
            wearPenalty,
            newCondition);
      }
    } catch (Exception e) {
      log.error(
          "Ambush wear carry-over failed for segment {} — adjudication continues",
          segment.getId(),
          e);
    }
  }

  /** The champions' wear percentage from their earlier match, breather subtracted (ATW-p8ij). */
  private int computeChampionWear(List<Wrestler> champions, Segment championSegment) {
    int wearPenalty = 0;
    for (Wrestler champion : champions) {
      Integer finalHealth =
          championSegment.getParticipants().stream()
              .filter(
                  p -> p.getWrestler() != null && champion.getId().equals(p.getWrestler().getId()))
              .map(SegmentParticipant::getFinalHealth)
              .filter(Objects::nonNull)
              .findFirst()
              .orElse(null);
      if (finalHealth == null) {
        continue; // wrestled but no reported health (NPC) — no specific carry-over number
      }
      // Percent of starting health lost, scaled the same way adjudication scales wear, minus
      // the breather. Floor at 0 (a breathered champion is effectively fresh).
      int startingHealth = Math.max(1, champion.getStartingHealth());
      int lossPercent = Math.max(0, (startingHealth - finalHealth) * 100 / startingHealth);
      wearPenalty += Math.max(0, lossPercent / 10 - AMBUSH_BREATHER);
    }
    return wearPenalty;
  }

  /**
   * The classic WWE ambush: the holder cashes in immediately AFTER the champion has already
   * wrestled on the same show, entering fresh against a worn-down champion. The champion's
   * same-show wear (their adjudicated final health/stamina) is carried over as an extra penalty on
   * the champion's team weight — minus a small between-segments breather.
   *
   * <p>All cash-in validations apply (HELD, not expired, active holder, eligible title, reigning
   * champion). On top: the champion(s) must have wrestled earlier on this show, their earlier match
   * must still belong to them (a title change voids the ambush — the case is not spent), and they
   * must not be in a promo (no wear).
   *
   * @param opportunityId the held briefcase
   * @param titleId the championship to challenge
   * @param showId the show where both the earlier match and the ambush book
   * @param championSegmentId the champion's earlier segment on this show
   * @return the booked ambush segment
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Segment cashInAmbush(
      long opportunityId, long titleId, long showId, long championSegmentId) {
    Show show =
        showRepository
            .findById(showId)
            .orElseThrow(() -> new IllegalArgumentException("Show not found: " + showId));
    Segment championSegment =
        segmentRepository
            .findById(championSegmentId)
            .orElseThrow(
                () -> new IllegalArgumentException("Segment not found: " + championSegmentId));
    if (championSegment.getShow() == null
        || !championSegment.getShow().getId().equals(show.getId())) {
      throw new IllegalArgumentException(
          "The champion's segment is not on the same show — an ambush targets a match"
              + " earlier on the same card");
    }
    if (WellKnownSegmentType.PROMO.matches(championSegment.getSegmentType())) {
      throw new IllegalStateException(
          "The champion's earlier segment is a promo — no wear to exploit, no ambush");
    }
    if (championSegment.getAdjudicationStatus() != AdjudicationStatus.ADJUDICATED) {
      throw new IllegalStateException(
          "The champion's earlier match has not been adjudicated yet — there is no final"
              + " wear to carry over");
    }
    // A champion who LOST a title match earlier no longer reigns — the reign-table read below
    // returns the new champion, who did not wrestle this segment, and the wear loop rejects the
    // ambush (the case is not spent on a stolen-credential setup).

    // Resolve the current champions BEFORE booking (needed for the wear penalty).
    Title title =
        titleRepository
            .findById(titleId)
            .orElseThrow(() -> new IllegalArgumentException("Title not found: " + titleId));
    List<Wrestler> champions =
        titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()).stream()
            .flatMap(reign -> reign.getChampions().stream())
            .distinct()
            .toList();
    if (champions.isEmpty()) {
      throw new IllegalStateException(
          "'" + title.getName() + "' is vacant — no champion to ambush");
    }

    // Same-show wear carry-over: the champions' adjudicated final health/stamina from their
    // earlier match feeds an extra team penalty (breather subtracted). A champion who didn't
    // wrestle on this card gets no ambush (nothing to exploit).
    int wearPenalty = 0;
    for (Wrestler champion : champions) {
      Integer finalHealth =
          championSegment.getParticipants().stream()
              .filter(
                  p -> p.getWrestler() != null && champion.getId().equals(p.getWrestler().getId()))
              .map(SegmentParticipant::getFinalHealth)
              .filter(Objects::nonNull)
              .findFirst()
              .orElse(null);
      if (finalHealth == null) {
        boolean wrestledHere =
            championSegment.getWrestlers().stream()
                .anyMatch(w -> champion.getId().equals(w.getId()));
        if (!wrestledHere) {
          throw new IllegalStateException(
              champion.getName()
                  + " has not wrestled on this show yet — the ambush needs a worn-down"
                  + " champion");
        }
        continue; // wrestled but no reported health (NPC) — no specific carry-over number
      }
      // Percent of starting health lost, scaled the same way adjudication scales wear, minus
      // the breather. Floor at 0 (a breathered champion is effectively fresh).
      int startingHealth = Math.max(1, champion.getStartingHealth());
      int lossPercent = Math.max(0, (startingHealth - finalHealth) * 100 / startingHealth);
      wearPenalty += Math.max(0, lossPercent / 10 - AMBUSH_BREATHER);
    }
    if (wearPenalty <= 0) {
      throw new IllegalStateException(
          "The champion showed no wear in their earlier match — no ambush advantage");
    }

    // Run every standard cash-in validation first (HELD, expiry, holder, title eligibility,
    // reigning champion) WITHOUT booking — a validation failure must not spend the case.
    TitleOpportunity opportunity =
        opportunityRepository
            .findById(opportunityId)
            .orElseThrow(
                () -> new IllegalArgumentException("Briefcase not found: " + opportunityId));
    validateCashIn(opportunity, titleId);

    // Book with the wear-penalized champion team and spend (clamped here — the setter is plain).
    SegmentTeam championTeam = new SegmentTeam(champions, title.getName() + " Champion");
    championTeam.setExtraWearPenalty(Math.max(0, wearPenalty));
    Segment segment =
        bookCashIn(
            opportunity,
            title,
            show,
            championTeam,
            champions,
            " The ambush came moments after the champion's match — narrate the chaos and the"
                + " exhausted champion's fate.");
    segment.setNarration(
        "Briefcase ambush: "
            + opportunity.getWrestler().getName()
            + " blindsides the exhausted "
            + title.getName()
            + " champion"
            + (champions.size() > 1 ? "s" : "")
            + " after their match, cashing in '"
            + opportunity.getName()
            + "'. The champion is worn down from their earlier fight — narrate the chaos.");
    return segment;
  }

  /**
   * The validation block of {@link #cashIn(long, long, long)} (everything up to booking), shared
   * with the ambush path so both surfaces enforce identical rules. Loads and returns the title —
   * the holder checks run before the lookup to preserve cash-in's validation order.
   */
  private Title validateCashIn(TitleOpportunity opportunity, long titleId) {
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
    return title;
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

  /** Every briefcase with holder resolved — the CRUD grid (ATW-jpki). */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TitleOpportunity> findAllWithDetails() {
    // The fetch-joined query — the plain findAllByOrderByEarnedAtDesc leaves the holder a lazy
    // proxy and the grid's value providers run with no session (LazyInitializationException on
    // the briefcase list, seen in the sandbox 2026-10-10).
    return opportunityRepository.findAllWithDetails();
  }

  /** One briefcase with holder resolved — the CRUD edit dialog (ATW-jpki). */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Optional<TitleOpportunity> findByIdWithDetails(Long id) {
    return opportunityRepository.findByIdWithDetails(id);
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

  /**
   * Narration-context line for a wrestler's held briefcase (ATW-brrz): "Time Vault briefcase —
   * cashable against any reigning champion until 2027-10-08", or null when they hold none. The AI
   * narration should introduce a holder the same way it introduces a champion.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  @Nullable public String heldBriefcaseContextOf(Long wrestlerId) {
    return findHeldByWrestler(wrestlerId)
        .map(
            opportunity ->
                opportunity.getName()
                    + " — cashable against any reigning champion"
                    + (opportunity.getExpiryDate() != null
                        ? " until " + opportunity.getExpiryDate()
                        : ""))
        .orElse(null);
  }

  /**
   * Records that the booker declined (deleted) an advisor proposal for this briefcase (ATW-2b9u).
   * Drives the advisor's re-proposal cooldown window.
   */
  @Transactional
  public void recordDecline(long opportunityId) {
    TitleOpportunity opportunity =
        opportunityRepository
            .findById(opportunityId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Briefcase opportunity not found: " + opportunityId));
    opportunity.setLastDeclinedAt(gameDate());
    opportunityRepository.save(opportunity);
  }

  private LocalDate gameDate() {
    return gameSettingService.getCurrentGameDate();
  }

  private String stipulationOf(@Nullable SegmentRule rule) {
    return rule != null ? rule.getName() : "";
  }
}

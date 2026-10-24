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
package com.github.javydreamercsw.management.service.tournament;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationPurpose;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamMember;
import com.github.javydreamercsw.management.domain.team.TeamOrigin;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRecurrence;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.tournament.TrophyReign;
import com.github.javydreamercsw.management.domain.tournament.TrophyReignRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowBookingService;
import com.github.javydreamercsw.management.service.show.ShowSegmentReservationService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("bracketTournamentService")
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

  private final TournamentRepository tournamentRepository;
  private final TournamentEntryRepository entryRepository;
  private final TournamentRoundRepository roundRepository;
  private final TournamentMatchRepository matchRepository;
  private final WrestlerRepository wrestlerRepository;
  private final TeamRepository teamRepository;
  private final UniverseRepository universeRepository;
  private final ShowRepository showRepository;
  private final ShowBookingService showBookingService;
  private final ShowSegmentReservationService reservationService;
  private final TitleReignRepository titleReignRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final TitleOpportunityService titleOpportunityService;
  private final TrophyReignRepository trophyReignRepository;
  private final TrophyReignService trophyReignService;
  private final List<TournamentFormat> formats;

  private TournamentFormatContext formatContext() {
    return new TournamentFormatContext(roundRepository, matchRepository);
  }

  // ── Format registry ──────────────────────────────────────────────────────

  public List<TournamentFormat> getAvailableFormats() {
    return List.copyOf(formats);
  }

  public Optional<TournamentFormat> findFormat(@NonNull String formatId) {
    return formats.stream().filter(f -> f.getFormatId().equals(formatId)).findFirst();
  }

  // ── CRUD ──────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Tournament> findAll() {
    return tournamentRepository.findAll();
  }

  /**
   * Session-safe entrant count for detached grid rows — the entries collection is lazy and the list
   * view renders outside a transaction (TournamentListView).
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long countEntries(@NonNull Tournament tournament) {
    return entryRepository.countByTournamentId(tournament.getId());
  }

  /**
   * Whether the tournament has any persisted entrants — asked via a count query rather than {@code
   * getEntries().isEmpty()}, because inside an active transaction that call initializes the lazy
   * collection empty and an initialized collection never re-queries (a caller that then seeds would
   * still see "no entrants" for the rest of the transaction, ATW-oahn).
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public boolean hasEntries(@NonNull Long tournamentId) {
    return entryRepository.countByTournamentId(tournamentId) > 0;
  }

  /** Total tournament count — the seed sync's skip-if-not-empty gate (ATW-vg16). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public long count() {
    return tournamentRepository.count();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Tournament> findByUniverse(@NonNull Universe universe) {
    return tournamentRepository.findByUniverseIdOrderByStartDateDesc(universe.getId());
  }

  /**
   * Persist edits to an existing tournament (e.g. the creation wizard setting the edition cadence
   * after {@link #createTournament}, ATW-o4ad).
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament save(@NonNull Tournament tournament) {
    return tournamentRepository.save(tournament);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Tournament> findById(@NonNull Long id) {
    return tournamentRepository.findById(id);
  }

  /**
   * One-time tournaments whose payoff (final or champion showcase) books on the given show
   * (ATW-xbn4) — the "Host Show" binding from the tournament wizard.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Tournament> findByPayoffShowId(@NonNull Long showId) {
    return tournamentRepository.findByPayoffShowId(showId);
  }

  /** Like {@link #findById} but initializes all lazy collections for use outside a transaction. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Tournament> findByIdWithDetails(Long id) {
    return tournamentRepository.findById(id).map(this::initializeGraph);
  }

  private Tournament initializeGraph(@NonNull Tournament t) {
    // entrantName() touches whichever side the entry carries (team name or wrestler name) so the
    // detached detail view reads it without a lazy-initialization exception.
    t.getEntries().forEach(e -> e.entrantName());
    t.getAllowedRules().forEach(SegmentRule::getName);
    // The detail view reads the linked championship's name outside the session.
    if (t.getLinkedTitle() != null) {
      t.getLinkedTitle().getName();
    }
    // The one-time host show (ATW-xbn4) — defensive touch in case the fetch type ever changes.
    if (t.getPayoffShow() != null) {
      t.getPayoffShow().getName();
    }
    t.getRounds()
        .forEach(
            r -> {
              if (r.getFixedRule() != null) {
                r.getFixedRule().getName();
              }
              r.getMatches()
                  .forEach(
                      m -> {
                        m.getEntrant1().entrantName();
                        m.getEntrant2().entrantName();
                        if (m.getWinner() != null) {
                          m.getWinner().entrantName();
                        }
                        // The bracket label reads the booked segment's applied rule (and the
                        // segment link itself) outside the session — touch both eagerly.
                        if (m.getSegment() != null) {
                          m.getSegment().getSegmentRules().forEach(SegmentRule::getName);
                        }
                        if (r.getShow() != null) {
                          r.getShow().getName();
                        }
                      });
            });
    return t;
  }

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament createTournament(
      String name,
      String formatId,
      Universe universe,
      Title linkedTitle,
      LocalDate startDate,
      List<SegmentRule> allowedRules) {
    return createTournament(
        name, formatId, universe, linkedTitle, startDate, allowedRules, null, null, null, null);
  }

  /**
   * Create with the one-time host-show binding (ATW-xbn4): the payoff books on {@code payoffShow}
   * exactly once and rounds pace automatically onto the weekly shows before it. A null payoffShow
   * creates a free-running (or recurring template-paired) tournament. The optional gender filter
   * narrows the eligible entrant pool on top of the linked title's own constraint.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament createTournament(
      String name,
      String formatId,
      Universe universe,
      Title linkedTitle,
      LocalDate startDate,
      List<SegmentRule> allowedRules,
      Show payoffShow,
      SegmentType payoffSegmentType,
      SegmentRule payoffSegmentRule) {
    return createTournament(
        name,
        formatId,
        universe,
        linkedTitle,
        startDate,
        allowedRules,
        payoffShow,
        payoffSegmentType,
        payoffSegmentRule,
        null);
  }

  /** Full overload including the optional entrant gender filter. */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament createTournament(
      String name,
      String formatId,
      Universe universe,
      Title linkedTitle,
      LocalDate startDate,
      List<SegmentRule> allowedRules,
      Show payoffShow,
      SegmentType payoffSegmentType,
      SegmentRule payoffSegmentRule,
      Gender gender) {
    return createTournament(
        name,
        formatId,
        universe,
        linkedTitle,
        startDate,
        allowedRules,
        payoffShow,
        payoffSegmentType,
        payoffSegmentRule,
        gender,
        false);
  }

  /**
   * Full overload including the contender-deciding mode (ATW-ewrp). {@code contenderDeciding}
   * requires a linked title — the winner becomes that title's #1 contender, so without a title the
   * mode is meaningless and creation is rejected.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament createTournament(
      String name,
      String formatId,
      Universe universe,
      Title linkedTitle,
      LocalDate startDate,
      List<SegmentRule> allowedRules,
      Show payoffShow,
      SegmentType payoffSegmentType,
      SegmentRule payoffSegmentRule,
      Gender gender,
      boolean contenderDeciding) {
    return createTournament(
        name,
        formatId,
        universe,
        linkedTitle,
        startDate,
        allowedRules,
        payoffShow,
        payoffSegmentType,
        payoffSegmentRule,
        gender,
        contenderDeciding,
        false);
  }

  /**
   * Full overload including the payoff mode (ATW-ewrp contender / ATW-8p72 briefcase). The modes
   * are mutually exclusive: {@code contenderDeciding} requires a linked title (the winner becomes
   * its #1 contender); {@code briefcaseDeciding} forbids one (the winner earns a cashable shot at
   * ANY championship — a linked title would be meaningless).
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament createTournament(
      String name,
      String formatId,
      Universe universe,
      Title linkedTitle,
      LocalDate startDate,
      List<SegmentRule> allowedRules,
      Show payoffShow,
      SegmentType payoffSegmentType,
      SegmentRule payoffSegmentRule,
      Gender gender,
      boolean contenderDeciding,
      boolean briefcaseDeciding) {
    return createTournament(
        name,
        formatId,
        universe,
        linkedTitle,
        startDate,
        allowedRules,
        payoffShow,
        payoffSegmentType,
        payoffSegmentRule,
        gender,
        contenderDeciding,
        briefcaseDeciding,
        null,
        null);
  }

  /**
   * Full overload including the entrant type: {@code teamSize} null keeps the singles semantics; a
   * non-null value creates a team tournament taking team entrants of exactly that declared size.
   * Team tournaments cannot decide contenders or briefcases (both stay singles-only — the winner is
   * a team, and a team neither challenges as an individual nor holds a cashable briefcase), and
   * when a TEAM title is linked its {@code Title.getTeamSize()} must match.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament createTournament(
      String name,
      String formatId,
      Universe universe,
      Title linkedTitle,
      LocalDate startDate,
      List<SegmentRule> allowedRules,
      Show payoffShow,
      SegmentType payoffSegmentType,
      SegmentRule payoffSegmentRule,
      Gender gender,
      boolean contenderDeciding,
      boolean briefcaseDeciding,
      TeamSize teamSize,
      @Nullable String trophyName) {
    if (teamSize != null) {
      if (contenderDeciding || briefcaseDeciding) {
        throw new IllegalArgumentException(
            "Team tournaments are inaugural-champion tournaments only — contender- and"
                + " briefcase-deciding payoffs stay singles");
      }
      if (linkedTitle != null) {
        if (linkedTitle.getChampionshipType() != ChampionshipType.TEAM) {
          throw new IllegalArgumentException(
              "A team tournament must link a TEAM championship — '"
                  + linkedTitle.getName()
                  + "' is a singles title");
        }
        if (linkedTitle.getTeamSize() != null && linkedTitle.getTeamSize() != teamSize) {
          throw new IllegalArgumentException(
              "Tournament team size "
                  + teamSize
                  + " does not match the linked championship's team size "
                  + linkedTitle.getTeamSize());
        }
      }
    }
    if (contenderDeciding && briefcaseDeciding) {
      throw new IllegalArgumentException(
          "Choose one payoff mode — a tournament cannot be both contender-deciding and"
              + " briefcase-deciding");
    }
    if (contenderDeciding && linkedTitle == null) {
      throw new IllegalArgumentException(
          "A contender-deciding tournament needs a linked title — the winner becomes its"
              + " #1 contender");
    }
    if (briefcaseDeciding && linkedTitle != null) {
      throw new IllegalArgumentException(
          "A briefcase-deciding tournament must not link a title — the winner can cash in against"
              + " any champion");
    }
    validateTrophyName(trophyName, linkedTitle, contenderDeciding, briefcaseDeciding);
    validatePayoffShow(universe, payoffShow);
    findFormat(formatId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown format: " + formatId));
    Tournament t = new Tournament();
    t.setName(name);
    t.setFormatId(formatId);
    t.setUniverse(universe);
    t.setLinkedTitle(briefcaseDeciding ? null : linkedTitle);
    t.setPayoffShow(payoffShow);
    t.setPayoffSegmentType(payoffSegmentType);
    t.setPayoffSegmentRule(payoffSegmentRule);
    t.setGender(gender);
    t.setContenderDeciding(contenderDeciding);
    t.setBriefcaseDeciding(briefcaseDeciding);
    t.setTeamSize(teamSize);
    t.setTrophyName(trophyName != null ? trophyName.trim() : null);
    t.setStartDate(startDate);
    t.setStatus(TournamentStatus.SCHEDULED);
    t.setEntries(new ArrayList<>());
    t.setRounds(new ArrayList<>());
    t.setAllowedRules(allowedRules != null ? new ArrayList<>(allowedRules) : new ArrayList<>());
    return tournamentRepository.save(t);
  }

  /**
   * Trophy payoff validation: a named trophy is its own payoff kind — it can never share a
   * tournament with a linked title or the contender/briefcase payoffs (the winner lifts the trophy;
   * nothing else changes hands). Legal for team tournaments. A blank name is a UI slip, not a
   * trophy tournament.
   */
  private void validateTrophyName(
      @Nullable String trophyName,
      @Nullable Title linkedTitle,
      boolean contenderDeciding,
      boolean briefcaseDeciding) {
    if (trophyName == null) {
      return;
    }
    if (trophyName.isBlank()) {
      throw new IllegalArgumentException("Trophy name is required when the winner earns a trophy");
    }
    if (linkedTitle != null || contenderDeciding || briefcaseDeciding) {
      throw new IllegalArgumentException(
          "Choose one payoff mode — the trophy goes to the winner alone, with no title, contender"
              + " slot, or briefcase attached");
    }
  }

  /**
   * Create the next edition of a recurring tournament (ATW-o4ad): copies format, allowed rules,
   * linked title, universe, default entrant count, and the payoff match type + rule (tournament
   * identity, ATW-lj3d); names it with the base name (any trailing Roman numeral stripped) plus the
   * successor ordinal in Roman numerals ("Time Vault" → "Time Vault II"); sets parent + ordinal;
   * SCHEDULED, no host show (next year's PLE is scheduled separately), no entries/rounds.
   * Recurrence carries over so the chain continues. Idempotent: returns empty when a successor of
   * {@code completed} already exists (re-approval of the same payoff must not mint duplicate
   * editions).
   *
   * @param completed the edition whose payoff just booked
   * @return the new edition, or empty when one already exists or the tournament is not recurring
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Optional<Tournament> createNextEdition(@NonNull Tournament completed) {
    if (completed.getRecurrence() != TournamentRecurrence.ANNUAL) {
      return Optional.empty();
    }
    if (completed.getId() != null
        && tournamentRepository.findByParentId(completed.getId()).isPresent()) {
      return Optional.empty(); // successor already minted — re-approval is a no-op
    }
    int nextOrdinal =
        (completed.getEditionOrdinal() != null ? completed.getEditionOrdinal() : 1) + 1;
    Tournament next = new Tournament();
    next.setName(editionName(baseEditionName(completed.getName()), nextOrdinal));
    next.setFormatId(completed.getFormatId());
    next.setUniverse(completed.getUniverse());
    next.setLinkedTitle(completed.getLinkedTitle());
    next.setGender(completed.getGender());
    // The declared team size is format identity too — a recurring team tournament's successor
    // stays a team tournament of the same size (ATW-01g2 review).
    next.setTeamSize(completed.getTeamSize());
    next.setDefaultEntrantCount(completed.getDefaultEntrantCount());
    next.setQualifierGroupSize(completed.getQualifierGroupSize());
    next.setContenderDeciding(completed.isContenderDeciding());
    next.setBriefcaseDeciding(completed.isBriefcaseDeciding());
    // The payoff match type and stipulation are tournament identity — the successor's PLE books
    // the same kind of payoff match (ATW-lj3d). The host SHOW is not copied: next year's PLE is
    // scheduled separately, and the template pairing re-pointed to this edition carries its show.
    next.setPayoffSegmentType(completed.getPayoffSegmentType());
    next.setPayoffSegmentRule(completed.getPayoffSegmentRule());
    next.setParent(completed);
    next.setEditionOrdinal(nextOrdinal);
    next.setRecurrence(TournamentRecurrence.ANNUAL);
    next.setStartDate(completed.getStartDate());
    next.setStatus(TournamentStatus.SCHEDULED);
    next.setEntries(new ArrayList<>());
    next.setRounds(new ArrayList<>());
    next.setAllowedRules(new ArrayList<>(completed.getAllowedRules()));
    return Optional.of(tournamentRepository.save(next));
  }

  /**
   * "Time Vault" + 3 → "Time Vault III". Ordinal 1 → the base name unchanged. A base name already
   * carrying a Roman-numeral edition suffix has it stripped first ("Crown Cup XLII" + 43 → "Crown
   * Cup XLIII", never a stacked suffix).
   */
  public static String editionName(@NonNull String baseName, int ordinal) {
    return ordinal <= 1 ? baseName : baseEditionName(baseName) + " " + romanNumeral(ordinal);
  }

  /**
   * Strips a trailing Roman-numeral edition suffix ("Time Vault II" → "Time Vault"; a plain name
   * passes through) so the next edition builds from the shared base.
   */
  private static String baseEditionName(@NonNull String name) {
    return name.replaceFirst(" (?:[IVXLCDM]+)$", "");
  }

  /**
   * Auto-attach an unattached annual edition to a newly scheduled PLE (ATW-cpca): when a PLE is
   * created in a tournament's universe, the earliest edition in a recurring chain that still has no
   * host show claims it as its payoff show. Keeps the successor playable without a manual edit —
   * seasons end before next year's PLE is scheduled, so nothing else can attach it in time.
   *
   * <p>Rules: the show must be a PLE with a date; the edition must be SCHEDULED or IN_PROGRESS
   * (COMPLETE/CANCELLED ones never book a payoff); the show date must be after the edition's start
   * date (a PLE before the tournament begins is not its payoff); and the earliest edition wins, so
   * a chain with two unattached editions fills the older slot first — the next scheduled PLE then
   * claims the second. Manual attach via the edit dialog always wins (an attached edition is never
   * stolen). Never throws: scheduling a show must not fail over an advisory attach.
   *
   * @return the edition attached to this show, or empty when no candidate matched
   */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  public Optional<Tournament> autoAttachPayoffShow(@NonNull Show ple) {
    if (!ple.isPremiumLiveEvent()
        || ple.getShowDate() == null
        || ple.getUniverse() == null
        || ple.getUniverse().getId() == null) {
      return Optional.empty();
    }
    List<Tournament> candidates =
        tournamentRepository
            .findByUniverseIdAndPayoffShowIsNullAndRecurrenceOrderByEditionOrdinalAsc(
                ple.getUniverse().getId(), TournamentRecurrence.ANNUAL);
    for (Tournament candidate : candidates) {
      if (candidate.getStatus() != TournamentStatus.SCHEDULED
          && candidate.getStatus() != TournamentStatus.IN_PROGRESS) {
        continue;
      }
      // A PLE before the tournament starts cannot host its payoff (the bracket is not underway).
      if (candidate.getStartDate() != null
          && !ple.getShowDate().isAfter(candidate.getStartDate())) {
        continue;
      }
      candidate.setPayoffShow(ple);
      tournamentRepository.save(candidate);
      log.info(
          "Auto-attached PLE '{}' ({} ) as payoff show for annual edition '{}' (#{} — no host"
              + " show was set; manual edit can still re-attach, ATW-cpca)",
          ple.getName(),
          ple.getShowDate(),
          candidate.getName(),
          candidate.getEditionOrdinal());
      return Optional.of(candidate);
    }
    return Optional.empty();
  }

  private static String romanNumeral(int value) {
    if (value <= 0) {
      throw new IllegalArgumentException("Edition ordinals start at 1: " + value);
    }
    final int[] numbers = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    final String[] symbols = {
      "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"
    };
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < numbers.length; i++) {
      while (value >= numbers[i]) {
        sb.append(symbols[i]);
        value -= numbers[i];
      }
    }
    return sb.toString();
  }

  /**
   * Seed-catalog upsert (tournaments.json / {@code TournamentSync}, ATW-vg16): find by the stable
   * {@code code}, create when absent, otherwise update the editable metadata. Lifecycle state
   * (status, entries, rounds, payoff fields) is NEVER touched on update — re-syncing cannot
   * resurrect a consumed or completed tournament. Rules resolve by NAME via {@code
   * SegmentRuleRepository.findByName} (SegmentRule is name-keyed); unknown names are skipped with a
   * warning.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament createOrUpdateTournament(
      @NonNull String code,
      @NonNull String name,
      @NonNull String formatId,
      @Nullable Integer defaultEntrantCount,
      @Nullable List<String> allowedRuleNames) {
    findFormat(formatId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown format: " + formatId));
    List<SegmentRule> rules = resolveRuleNames(allowedRuleNames);
    Tournament t = tournamentRepository.findByCode(code).orElseGet(Tournament::new);
    boolean isNew = t.getId() == null;
    t.setCode(code);
    t.setName(name);
    if (isNew || !hasEntries(t.getId())) {
      // The format is locked once a bracket exists — keep the running tournament's format.
      t.setFormatId(formatId);
    }
    t.setDefaultEntrantCount(defaultEntrantCount);
    t.setAllowedRules(rules);
    if (isNew) {
      t.setUniverse(null);
      t.setStartDate(null);
      t.setStatus(TournamentStatus.SCHEDULED);
      t.setEntries(new ArrayList<>());
      t.setRounds(new ArrayList<>());
    }
    return tournamentRepository.save(t);
  }

  /** Resolve rule names to entities; unknown names are skipped with a warning (seed tolerance). */
  private List<SegmentRule> resolveRuleNames(@Nullable List<String> allowedRuleNames) {
    if (allowedRuleNames == null || allowedRuleNames.isEmpty()) {
      return new ArrayList<>();
    }
    return allowedRuleNames.stream()
        .map(
            name -> {
              SegmentRule rule = segmentRuleRepository.findByName(name).orElse(null);
              if (rule == null) {
                log.warn("Seed rule '{}' not found — skipping it in the tournament catalog", name);
              }
              return rule;
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /** The host show must live in the tournament's universe — validated at creation/edit time. */
  private void validatePayoffShow(Universe universe, Show payoffShow) {
    if (payoffShow != null
        && universe != null
        && payoffShow.getUniverse() != null
        && !payoffShow.getUniverse().getId().equals(universe.getId())) {
      throw new IllegalArgumentException("Host show must belong to the tournament's universe");
    }
  }

  /**
   * Update a tournament's editable metadata. Only SCHEDULED tournaments can be edited — once a
   * bracket is underway its structure (format, entrants) is locked.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament updateTournament(
      @NonNull final Long id,
      @NonNull final String name,
      final String formatId,
      final Title linkedTitle,
      final LocalDate startDate,
      final List<SegmentRule> allowedRules) {
    return updateTournament(
        id, name, formatId, linkedTitle, startDate, allowedRules, null, null, null, false);
  }

  /**
   * Update with the one-time host-show binding (ATW-xbn4). {@code setPayoffFields} false (the
   * compat delegate) leaves the existing payoff fields untouched; the wizard's save passes true.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament updateTournament(
      @NonNull final Long id,
      @NonNull final String name,
      final String formatId,
      final Title linkedTitle,
      final LocalDate startDate,
      final List<SegmentRule> allowedRules,
      final Show payoffShow,
      final SegmentType payoffSegmentType,
      final SegmentRule payoffSegmentRule,
      final boolean setPayoffFields) {
    return updateTournament(
        id,
        name,
        formatId,
        linkedTitle,
        startDate,
        allowedRules,
        payoffShow,
        payoffSegmentType,
        payoffSegmentRule,
        setPayoffFields,
        null,
        null,
        null);
  }

  /**
   * Full update including the payoff modes (ATW-ewrp contender / ATW-8p72 briefcase). {@code
   * contenderDeciding}/{@code briefcaseDeciding} null leaves the existing flag untouched (compat);
   * a non-null value overwrites it. Contender-deciding requires a linked title when true;
   * briefcase-deciding forbids one.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament updateTournament(
      @NonNull final Long id,
      @NonNull final String name,
      final String formatId,
      final Title linkedTitle,
      final LocalDate startDate,
      final List<SegmentRule> allowedRules,
      final Show payoffShow,
      final SegmentType payoffSegmentType,
      final SegmentRule payoffSegmentRule,
      final boolean setPayoffFields,
      final Boolean contenderDeciding) {
    return updateTournament(
        id,
        name,
        formatId,
        linkedTitle,
        startDate,
        allowedRules,
        payoffShow,
        payoffSegmentType,
        payoffSegmentRule,
        setPayoffFields,
        contenderDeciding,
        null,
        null);
  }

  /**
   * Full update including both payoff-mode flags and the trophy payoff. {@code
   * briefcaseDeciding}/{@code trophyName} null leaves the existing value untouched (compat); a
   * non-null value overwrites it. The trophy name follows the Boolean-flag pattern: passing a name
   * arms the trophy payoff, passing {@code ""} clears it, passing {@code null} keeps whatever the
   * tournament already carries.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament updateTournament(
      @NonNull final Long id,
      @NonNull final String name,
      final String formatId,
      final Title linkedTitle,
      final LocalDate startDate,
      final List<SegmentRule> allowedRules,
      final Show payoffShow,
      final SegmentType payoffSegmentType,
      final SegmentRule payoffSegmentRule,
      final boolean setPayoffFields,
      final Boolean contenderDeciding,
      final Boolean briefcaseDeciding,
      final String trophyName) {
    Tournament t =
        tournamentRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + id));
    if (t.getStatus() != TournamentStatus.SCHEDULED) {
      throw new IllegalStateException(
          "Only SCHEDULED tournaments can be edited — '" + t.getName() + "' is " + t.getStatus());
    }
    if (setPayoffFields) {
      validatePayoffShow(t.getUniverse(), payoffShow);
    }
    boolean effectiveContenderDeciding =
        contenderDeciding != null ? contenderDeciding : t.isContenderDeciding();
    boolean effectiveBriefcaseDeciding =
        briefcaseDeciding != null ? briefcaseDeciding : t.isBriefcaseDeciding();
    Title effectiveTitle = linkedTitle != null ? linkedTitle : t.getLinkedTitle();
    String effectiveTrophyName =
        trophyName != null ? (trophyName.isBlank() ? null : trophyName.trim()) : t.getTrophyName();
    if (effectiveContenderDeciding && effectiveBriefcaseDeciding) {
      throw new IllegalArgumentException(
          "Choose one payoff mode — a tournament cannot be both contender-deciding and"
              + " briefcase-deciding");
    }
    if (effectiveContenderDeciding && effectiveTitle == null) {
      throw new IllegalArgumentException(
          "A contender-deciding tournament needs a linked title — the winner becomes its"
              + " #1 contender");
    }
    if (effectiveBriefcaseDeciding && effectiveTitle != null) {
      throw new IllegalArgumentException(
          "A briefcase-deciding tournament must not link a title — the winner can cash in against"
              + " any champion");
    }
    if (effectiveTrophyName != null
        && (effectiveTitle != null || effectiveContenderDeciding || effectiveBriefcaseDeciding)) {
      throw new IllegalArgumentException(
          "Choose one payoff mode — the trophy goes to the winner alone, with no title, contender"
              + " slot, or briefcase attached");
    }
    t.setName(name);
    if (formatId != null && !formatId.equals(t.getFormatId())) {
      if (!t.getEntries().isEmpty()) {
        throw new IllegalStateException(
            "Cannot change the format of a tournament that already has entrants");
      }
      findFormat(formatId)
          .orElseThrow(() -> new IllegalArgumentException("Unknown format: " + formatId));
      t.setFormatId(formatId);
    }
    t.setLinkedTitle(linkedTitle);
    t.setStartDate(startDate);
    t.setAllowedRules(allowedRules != null ? new ArrayList<>(allowedRules) : new ArrayList<>());
    if (contenderDeciding != null) {
      t.setContenderDeciding(contenderDeciding);
    }
    if (briefcaseDeciding != null) {
      t.setBriefcaseDeciding(briefcaseDeciding);
    }
    if (trophyName != null) {
      t.setTrophyName(trophyName.isBlank() ? null : trophyName.trim());
    }
    if (setPayoffFields) {
      t.setPayoffShow(payoffShow);
      t.setPayoffSegmentType(payoffSegmentType);
      t.setPayoffSegmentRule(payoffSegmentRule);
    }
    return tournamentRepository.save(t);
  }

  /**
   * Detach a one-time tournament from its host show so the payoff cannot fire twice (ATW-xbn4). The
   * booking service calls this once the payoff segment exists.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void clearPayoffShow(@NonNull Tournament tournament) {
    tournament.setPayoffShow(null);
    tournamentRepository.save(tournament);
  }

  /**
   * Delete a tournament. Only SCHEDULED ones (no bracket in flight, no booked segments to orphan);
   * deletes entries, rounds, and their matches first since match → round is a plain FK with no JPA
   * cascade.
   *
   * @return true when deleted, false when the tournament does not exist
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public boolean deleteTournament(@NonNull final Long id) {
    Tournament t = tournamentRepository.findById(id).orElse(null);
    if (t == null) {
      return false;
    }
    if (t.getStatus() != TournamentStatus.SCHEDULED) {
      throw new IllegalStateException(
          "Only SCHEDULED tournaments can be deleted — '"
              + t.getName()
              + "' is "
              + t.getStatus()
              + ". Start a new one instead.");
    }
    t.getRounds()
        .forEach(round -> matchRepository.deleteAll(matchRepository.findByRoundId(round.getId())));
    roundRepository.deleteAll(roundRepository.findByTournamentIdOrderByRoundNumberAsc(id));
    entryRepository.deleteAll(entryRepository.findByTournamentIdOrderBySeedAsc(id));
    tournamentRepository.delete(t);
    return true;
  }

  // ── Entry management ──────────────────────────────────────────────────────

  /**
   * Guard for the exactly-one-entrant-shape invariant the DB check constraint {@code
   * chk_entry_entrant_type} backstops: an entry holds either a wrestler or a team, never both,
   * never neither. Called on every entry this service builds.
   */
  static void validateEntrantShape(TournamentEntry entry) {
    if ((entry.getWrestler() == null) == (entry.getTeam() == null)) {
      throw new IllegalArgumentException(
          "A tournament entry holds exactly one entrant — either a wrestler or a team, got"
              + " wrestler="
              + (entry.getWrestler() != null)
              + ", team="
              + (entry.getTeam() != null));
    }
  }

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TournamentEntry addEntry(Tournament tournament, Wrestler wrestler, int seed) {
    if (tournament.getTeamSize() != null) {
      throw new IllegalArgumentException(
          "'"
              + tournament.getName()
              + "' is a team tournament — entrants are teams of "
              + tournament.getTeamSize()
              + ", not individual wrestlers");
    }
    if (entryRepository.existsByTournamentIdAndWrestlerId(tournament.getId(), wrestler.getId())) {
      throw new IllegalStateException(
          wrestler.getName() + " is already entered in this tournament");
    }
    TournamentEntry entry =
        TournamentEntry.builder().tournament(tournament).wrestler(wrestler).seed(seed).build();
    validateEntrantShape(entry);
    return entryRepository.save(entry);
  }

  /**
   * Enter a {@link Team} into a team tournament ({@code Tournament.getTeamSize() != null}).
   * Validates the tournament takes teams, the declared size matches, every member satisfies the
   * effective gender restriction (the tournament's filter, else the linked TEAM championship's own
   * constraint — the same fallback the eligibility pool applies), and no member is already entered
   * in the tournament through any other entry (team or individual) — a wrestler may appear in only
   * one entry per tournament.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TournamentEntry addEntry(Tournament tournament, Team team, int seed) {
    if (tournament.getTeamSize() == null) {
      throw new IllegalArgumentException(
          "'"
              + tournament.getName()
              + "' is a singles tournament — entrants are individual wrestlers, not teams");
    }
    if (team.getTeamSize() != tournament.getTeamSize()) {
      throw new IllegalArgumentException(
          "Team '"
              + team.getName()
              + "' is a "
              + team.getTeamSize()
              + " team — '"
              + tournament.getName()
              + "' takes "
              + tournament.getTeamSize()
              + " teams");
    }
    Gender gender = effectiveTeamGenderOf(tournament.getGender(), tournament.getLinkedTitle());
    if (gender != null) {
      for (TeamMember member : team.getMembers()) {
        if (member.getWrestler().getGender() != gender) {
          throw new IllegalArgumentException(
              "Team '"
                  + team.getName()
                  + "' cannot enter: member "
                  + member.getWrestler().getName()
                  + " does not satisfy the tournament's "
                  + gender
                  + " gender restriction");
        }
      }
    }
    Set<Long> memberIds =
        team.getMembers().stream()
            .map(TeamMember::getWrestler)
            .map(Wrestler::getId)
            .collect(Collectors.toSet());
    for (TournamentEntry existing :
        entryRepository.findByTournamentIdOrderBySeedAsc(tournament.getId())) {
      validateEntrantShape(existing);
      if (existing.getTeam() != null) {
        Set<Long> existingIds =
            existing.getTeam().getMembers().stream()
                .map(TeamMember::getWrestler)
                .map(Wrestler::getId)
                .collect(Collectors.toSet());
        Set<Long> overlap = new HashSet<>(memberIds);
        overlap.retainAll(existingIds);
        if (!overlap.isEmpty()) {
          throw new IllegalStateException(
              "Wrestler is already entered in this tournament (via team '"
                  + existing.getTeam().getName()
                  + "')");
        }
      } else if (memberIds.contains(existing.getWrestler().getId())) {
        throw new IllegalStateException(
            existing.getWrestler().getName() + " is already entered in this tournament");
      }
    }
    TournamentEntry entry =
        TournamentEntry.builder().tournament(tournament).team(team).seed(seed).build();
    validateEntrantShape(entry);
    return entryRepository.save(entry);
  }

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentEntry> seedAuto(Tournament tournament, int count, Long universeId) {
    TournamentFormat fmt =
        findFormat(tournament.getFormatId())
            .orElseThrow(() -> new IllegalStateException("Format not found"));
    if (tournament.getTeamSize() != null) {
      List<Team> teamPool = findEligibleTeamsSortedByFans(tournament);
      if (teamPool.size() < fmt.getMinEntrants()) {
        throw new IllegalStateException(
            "Not enough eligible teams to seed '"
                + tournament.getName()
                + "': "
                + teamPool.size()
                + " available, the format needs at least "
                + fmt.getMinEntrants()
                + eligibilityNote(
                    tournament.getLinkedTitle(),
                    effectiveTeamGenderOf(tournament.getGender(), tournament.getLinkedTitle())));
      }
      int effectiveTeams = Math.min(count, teamPool.size());
      if (effectiveTeams < count) {
        log.info(
            "Seeding '{}' with {} teams ({} requested, the eligible registry holds only that"
                + " many)",
            tournament.getName(),
            effectiveTeams,
            count);
      }
      // The format's bracket cannot generate beyond its maximum — the same range check the
      // wrestler path enforces through seedWith, applied to the team count (ATW-01g2 review).
      if (effectiveTeams > fmt.getMaxEntrants()) {
        throw new IllegalArgumentException(
            "Team entrant count "
                + effectiveTeams
                + " out of range ["
                + fmt.getMinEntrants()
                + ", "
                + fmt.getMaxEntrants()
                + "]");
      }
      List<TournamentEntry> teamEntries = new ArrayList<>();
      for (int i = 0; i < effectiveTeams; i++) {
        teamEntries.add(addEntry(tournament, teamPool.get(i), i + 1));
      }
      return teamEntries;
    }
    List<Wrestler> pool =
        findEligibleWrestlersSortedByFans(
            tournament.getLinkedTitle(), tournament.getGender(), universeId);
    if (pool.size() < fmt.getMinEntrants()) {
      throw new IllegalStateException(
          "Not enough eligible wrestlers to seed '"
              + tournament.getName()
              + "': "
              + pool.size()
              + " available, the format needs at least "
              + fmt.getMinEntrants()
              + eligibilityNote(tournament.getLinkedTitle(), tournament.getGender()));
    }
    int effective = Math.min(count, pool.size());
    if (effective < count) {
      // Requested more than the eligible roster holds — seed everyone available instead of
      // failing (the wizard caps its field at the roster size; API callers may not).
      log.info(
          "Seeding '{}' with {} entrants ({} requested, roster holds only that many)",
          tournament.getName(),
          effective,
          count);
    }
    List<Wrestler> active = pool.stream().limit(effective).toList();
    return seedWith(tournament, active);
  }

  /**
   * The active REGISTRY teams of the declared size eligible to seed a team tournament with the
   * given gender filter, sorted by the team's own fan counter (most fans first) — the team-level
   * mirror of {@link #findEligibleWrestlersSortedByFans}. The gender restriction applies per
   * member: every member must be active with that gender (the same shape the TEAM ranking filter
   * uses). When the tournament sets no gender, the linked TEAM championship's own constraint
   * applies instead (the same effective-gender rule the singles path uses). Champion exclusion does
   * not apply — an inaugural-champion tournament links a vacant (or not-yet-created) title, so
   * there is no champion to exclude. Scoped to the tournament's universe when one is set (teams are
   * universe-scoped; the same scoping the singles path gets from the universe-bound fan states).
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Team> findEligibleTeamsSortedByFans(Tournament tournament) {
    Gender gender = effectiveTeamGenderOf(tournament.getGender(), tournament.getLinkedTitle());
    Long universeId = tournament.getUniverse() != null ? tournament.getUniverse().getId() : null;
    return teamRepository.findAll().stream()
        .filter(
            team ->
                team.getTeamSize() == tournament.getTeamSize()
                    && team.getOrigin() == TeamOrigin.REGISTRY
                    && team.isActive()
                    && sameUniverse(team, universeId)
                    // Non-empty: an empty husk team (last member deleted) would pass allMatch
                    // vacuously and land in the wizard's registry picker without any members.
                    && !team.getMembers().isEmpty()
                    && team.getMembers().stream()
                        .map(TeamMember::getWrestler)
                        .allMatch(
                            w ->
                                Boolean.TRUE.equals(w.getActive())
                                    && (gender == null || w.getGender() == gender)))
        .sorted(Comparator.comparingLong(Team::getFans).reversed())
        .toList();
  }

  /**
   * The gender a team tournament's pool is narrowed by: the tournament's own filter when set, else
   * the linked championship's constraint (ATW-01g2 review) — the same fallback the singles path's
   * {@link #findEligibleWrestlersSortedByFans} applies, so a linked women's tag-title tournament
   * takes women's teams even with no explicit division filter.
   */
  private static Gender effectiveTeamGenderOf(Gender gender, Title linkedTitle) {
    return gender != null ? gender : linkedTitle != null ? linkedTitle.getGender() : null;
  }

  /**
   * Universe scoping for the eligibility pool: when the tournament has a universe, only teams of
   * that universe are eligible; when it does not (no universe selected), teams from any universe
   * pass — matching the singles path, whose fan-state lookup also widens when no universe is given.
   */
  private static boolean sameUniverse(@NonNull Team team, Long universeId) {
    if (universeId == null) {
      return true;
    }
    return team.getUniverse() != null && universeId.equals(team.getUniverse().getId());
  }

  /** Human-readable note appended to eligibility errors when a constraint narrowed the pool. */
  private String eligibilityNote(Title linkedTitle, Gender gender) {
    if (linkedTitle == null && gender == null) {
      return "";
    }
    StringBuilder note = new StringBuilder(" (eligibility limited");
    if (linkedTitle != null) {
      note.append(" by the linked championship's gender constraint and current champion");
    }
    if (gender != null) {
      note.append(linkedTitle != null ? "," : " by").append(" the tournament gender filter");
    }
    return note.append(")").toString();
  }

  /**
   * How many entrants are eligible to seed a tournament with the given linked title and gender
   * filter — the active roster, narrowed by both gender constraints and minus the current
   * champion(s) (see {@link #findEligibleWrestlersSortedByFans}). For a TEAM title the count is
   * team-based instead: the active REGISTRY teams of the title's declared size whose every member
   * satisfies the gender (see {@link #findEligibleTeamsSortedByFans}; a TEAM title with no declared
   * size counts its backfilled DUO default). Scoped to the title's universe when it has one. The
   * creation wizard caps its entrant-count field at this value.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public int countEligibleEntrants(Title linkedTitle, Gender gender) {
    if (linkedTitle != null && linkedTitle.getChampionshipType() == ChampionshipType.TEAM) {
      Tournament probe = new Tournament();
      probe.setTeamSize(
          linkedTitle.getTeamSize() != null ? linkedTitle.getTeamSize() : TeamSize.DUO);
      probe.setGender(gender);
      probe.setUniverse(linkedTitle.getUniverse());
      return findEligibleTeamsSortedByFans(probe).size();
    }
    return findEligibleWrestlersSortedByFans(linkedTitle, gender, null).size();
  }

  /**
   * How many teams are eligible to seed a team tournament of the given size with the given gender
   * filter — the team mirror of {@link #countEligibleEntrants} (active REGISTRY teams whose every
   * member is active with the gender; see {@link #findEligibleTeamsSortedByFans}). The linked
   * championship's own gender constraint applies when the tournament sets no filter (ATW-01g2
   * review) — the same effective-gender rule the eligibility pool and the singles count apply.
   * Scoped to the given universe when non-null. The creation wizard caps its team-entrant-count
   * field at this value.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public int countEligibleTeamEntrants(
      TeamSize teamSize, Gender gender, Title linkedTitle, Long universeId) {
    Tournament probe = new Tournament();
    probe.setTeamSize(teamSize);
    probe.setGender(gender);
    probe.setLinkedTitle(linkedTitle);
    if (universeId != null) {
      probe.setUniverse(universeRepository.findById(universeId).orElse(null));
    }
    return findEligibleTeamsSortedByFans(probe).size();
  }

  /**
   * Seed from an explicit ordered wrestler list (seed i+1 = list position i). Shared by {@link
   * #seedAuto} and the seeding editor. Validates against the format's range so a re-seed cannot
   * produce a bracket the format cannot generate.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentEntry> seedWith(Tournament tournament, List<Wrestler> orderedWrestlers) {
    TournamentFormat fmt =
        findFormat(tournament.getFormatId())
            .orElseThrow(() -> new IllegalStateException("Format not found"));
    if (orderedWrestlers.size() < fmt.getMinEntrants()
        || orderedWrestlers.size() > fmt.getMaxEntrants()) {
      throw new IllegalArgumentException(
          "Entrant count "
              + orderedWrestlers.size()
              + " out of range ["
              + fmt.getMinEntrants()
              + ", "
              + fmt.getMaxEntrants()
              + "]");
    }
    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 0; i < orderedWrestlers.size(); i++) {
      entries.add(addEntry(tournament, orderedWrestlers.get(i), i + 1));
    }
    return entries;
  }

  /**
   * The active wrestlers eligible to seed a tournament with the given linked title and gender
   * filter, sorted by fans (most fans first) — the same pool {@link #seedAuto} uses. Eligibility:
   * active, narrowed by the title's gender constraint and the tournament's own gender filter when
   * either is set, and (title-linked tournaments) the current champion(s) are excluded — they hold
   * the belt the tournament awards, so they cannot win it from themselves. Transactional so
   * detached UI callers can read fan counts (they walk the lazy wrestlerStates collection) and
   * render the seeding preview without LazyInitializationException.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> findEligibleWrestlersSortedByFans(
      Title linkedTitle, Gender gender, Long universeId) {
    Gender effectiveGender =
        gender != null ? gender : linkedTitle != null ? linkedTitle.getGender() : null;
    List<Wrestler> pool =
        (effectiveGender != null
                ? wrestlerRepository.findAllByGenderAndActive(effectiveGender, true)
                : wrestlerRepository.findAllByActiveTrue())
            .stream()
                .sorted(Comparator.comparingLong((Wrestler w) -> w.getFans(universeId)).reversed())
                .toList();
    if (linkedTitle == null) {
      return pool;
    }
    List<Long> championIds = currentChampionIds(linkedTitle);
    if (championIds.isEmpty()) {
      return pool;
    }
    List<Wrestler> withoutChampions =
        pool.stream().filter(w -> !championIds.contains(w.getId())).toList();
    if (withoutChampions.size() < pool.size()) {
      log.debug(
          "Excluded current champion(s) of '{}' from tournament eligibility",
          linkedTitle.getName());
    }
    return withoutChampions;
  }

  /** Ids of the wrestlers currently holding the given title (empty when vacant). */
  private List<Long> currentChampionIds(Title linkedTitle) {
    // A lazy collection already initialized inside the current transaction never re-queries —
    // read the champions from the reign table, not the detached title's in-memory list.
    return titleReignRepository.findByTitleIdAndEndDateIsNull(linkedTitle.getId()).stream()
        .flatMap(reign -> reign.getChampions().stream())
        .map(Wrestler::getId)
        .distinct()
        .toList();
  }

  /**
   * Whether the given championship is vacant (no active reign). Read from the reign table, not the
   * detached title's in-memory list — the same lazy-collection trap {@link #currentChampionIds}
   * avoids (ATW-z963 payoff semantics).
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public boolean isTitleVacant(@NonNull Title title) {
    return titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()).isEmpty();
  }

  /**
   * The wrestlers currently holding the given title (empty when vacant). Read from the reign table
   * — the detached title's in-memory champions list may be an uninitialized lazy collection.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<Wrestler> currentChampionsOf(@Nullable Title title) {
    if (title == null) {
      return List.of();
    }
    return titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()).stream()
        .flatMap(reign -> reign.getChampions().stream())
        .collect(
            Collectors.collectingAndThen(
                Collectors.toMap(Wrestler::getId, w -> w, (a, b) -> a),
                m -> List.copyOf(m.values())));
  }

  // ── Bracket lifecycle ─────────────────────────────────────────────────────

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Tournament startTournament(Tournament tournament) {
    // Re-resolve id-bearing callers to the managed graph (found in live tour testing): a
    // detached instance whose entries collection was initialized before seedAuto ran fails the
    // entrants check with a stale-empty collection — an initialized lazy collection never
    // re-queries.
    // Managed callers (template-fed approval) get the same instance back from the persistence
    // context, so seeding inside the caller's transaction stays visible exactly as before.
    Tournament managed =
        tournament.getId() == null
            ? tournament
            : tournamentRepository
                .findById(tournament.getId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Tournament not found: " + tournament.getId()));
    if (managed.getStatus() != TournamentStatus.SCHEDULED) {
      throw new IllegalStateException("Tournament is not in SCHEDULED state");
    }
    if (managed.getEntries().isEmpty()) {
      throw new IllegalStateException("No entrants — seed the tournament before starting");
    }
    TournamentFormat fmt =
        findFormat(managed.getFormatId())
            .orElseThrow(() -> new IllegalStateException("Format not found"));
    List<TournamentRound> generated = fmt.generateBracket(managed, formatContext());
    // The format persists the bracket through the owning side; attach it to the in-memory
    // collection so callers holding this instance (template-fed approval, ATW-oahn) can scan
    // round 1 without re-fetching — a lazy collection already initialized inside the current
    // transaction never re-queries.
    attachGenerated(managed, generated.stream().flatMap(r -> r.getMatches().stream()).toList());
    managed.setStatus(TournamentStatus.IN_PROGRESS);
    return tournamentRepository.save(managed);
  }

  /**
   * Make freshly generated bracket content visible on the in-memory tournament. A lazy collection
   * that was already initialized within the current transaction never re-queries, so rounds the
   * format persisted through the owning side would otherwise be invisible to in-memory scans (e.g.
   * the template-fed booking path's open-match lookup).
   */
  private void attachGenerated(Tournament tournament, List<TournamentMatch> generated) {
    for (TournamentMatch match : generated) {
      TournamentRound round = match.getRound();
      TournamentRound attached =
          tournament.getRounds().stream()
              .filter(r -> r.getId() != null && r.getId().equals(round.getId()))
              .findFirst()
              .orElseGet(
                  () -> {
                    tournament.getRounds().add(round);
                    return round;
                  });
      if (attached.getMatches().stream()
          .noneMatch(m -> m.getId() != null && m.getId().equals(match.getId()))) {
        attached.getMatches().add(match);
      }
    }
  }

  /**
   * Mark a PENDING round IN_PROGRESS when one of its matches is booked through any path. The manual
   * flow sets this in {@link #bookRoundOnShow}; template-fed booking (ATW-oahn) books matches one
   * at a time outside it, so it calls this to keep the UI's "Book Round on Show" button coherent —
   * a round with a booked match is no longer offerable for booking.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void markRoundInProgress(TournamentRound round) {
    if (round.getStatus() == TournamentRoundStatus.PENDING) {
      round.setStatus(TournamentRoundStatus.IN_PROGRESS);
      roundRepository.save(round);
    }
  }

  /**
   * Book all pending matches in the current round onto a show, creating segment reservations.
   * Team-aware (ATW-01g2 review): team entries book through the same team-segment construction the
   * template-fed path uses (sides = the entry team's members in slot order), never through the
   * solo-wrestler path whose @NonNull params an entrant with no wrestler would trip.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void bookRoundOnShow(Tournament tournament, TournamentRound round, Show show) {
    List<TournamentMatch> unbooked =
        matchRepository.findByRoundIdAndWinnerIsNull(round.getId()).stream()
            .filter(m -> m.getSegment() == null)
            .toList();

    for (TournamentMatch match : unbooked) {
      String label =
          tournament.getName()
              + " — "
              + round.getRoundName()
              + ": "
              + match.getEntrant1().entrantName()
              + " vs "
              + match.getEntrant2().entrantName();

      String stipulation = resolveStipulation(tournament, round, label);

      // Reserve a slot on the show
      reservationService.reserveSlot(
          show, ShowSegmentReservationPurpose.TOURNAMENT_ROUND, match.getId(), label);

      // Book the actual segment with the resolved stipulation — team sides when either entrant
      // is a team, solo wrestlers otherwise.
      Optional<Segment> booked =
          teamSidesOf(match)
              .map(
                  sides ->
                      showBookingService.bookSpecificTeamMatch(
                          show, sides.get(0), sides.get(1), label, stipulation))
              .orElseGet(
                  () ->
                      showBookingService.bookSpecificMatch(
                          show,
                          match.getEntrant1().getWrestler(),
                          match.getEntrant2().getWrestler(),
                          label,
                          stipulation));
      booked.ifPresent(
          segment -> {
            match.setSegment(segment);
            matchRepository.save(match);
          });
    }

    round.setShow(show);
    round.setStatus(TournamentRoundStatus.IN_PROGRESS);
    roundRepository.save(round);
  }

  /**
   * The match's two sides as member lists when EITHER entrant is a team (slot-ordered members),
   * empty when both are solo wrestlers (the classic path books those). Sized sides keep mixed
   * team/solo pairings bookable — the resolution treats a 1-wrestler side as its own team.
   */
  private Optional<List<List<Wrestler>>> teamSidesOf(TournamentMatch match) {
    if (match.getEntrant1().getTeam() == null && match.getEntrant2().getTeam() == null) {
      return Optional.empty();
    }
    return Optional.of(List.of(membersOf(match.getEntrant1()), membersOf(match.getEntrant2())));
  }

  /** The entry's side: the team's members in slot order, or the solo wrestler alone. */
  private static List<Wrestler> membersOf(TournamentEntry entry) {
    if (entry.getTeam() != null) {
      return entry.getTeam().getMembers().stream()
          .map(TeamMember::getWrestler)
          .collect(Collectors.toList());
    }
    return List.of(entry.getWrestler());
  }

  /**
   * Assign a fixed segment rule to a round. Set {@code null} to clear and revert to the
   * tournament's allowed-rules pool.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TournamentRound setRoundFixedRule(TournamentRound round, SegmentRule rule) {
    round.setFixedRule(rule);
    return roundRepository.save(round);
  }

  // ── Seeding edits (SCHEDULED + no bracket yet) ─────────────────────────────

  /**
   * Reorder the tournament's seeds: the given entry ids' order becomes the new seed order (seed 1 =
   * first id in the list). Only allowed while SCHEDULED with no generated bracket — once the
   * bracket exists the pairings are locked.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentEntry> reorderSeeds(
      @NonNull final Long tournamentId, @NonNull final List<Long> orderedEntryIds) {
    Tournament t = requireEditableForSeeding(tournamentId);
    List<TournamentEntry> entries = entryRepository.findByTournamentIdOrderBySeedAsc(tournamentId);
    if (orderedEntryIds.size() != entries.size()) {
      throw new IllegalArgumentException(
          "Expected all "
              + entries.size()
              + " entrants in the new order, got "
              + orderedEntryIds.size());
    }
    Set<Long> entryIds = entries.stream().map(TournamentEntry::getId).collect(Collectors.toSet());
    for (Long entryId : orderedEntryIds) {
      if (!entryIds.contains(entryId)) {
        throw new IllegalArgumentException("Entry " + entryId + " is not in this tournament");
      }
    }
    for (int i = 0; i < orderedEntryIds.size(); i++) {
      final Long entryId = orderedEntryIds.get(i);
      final int seed = i + 1;
      TournamentEntry entry =
          entries.stream().filter(e -> e.getId().equals(entryId)).findFirst().orElseThrow();
      entry.setSeed(seed);
      entryRepository.save(entry);
    }
    return entryRepository.findByTournamentIdOrderBySeedAsc(tournamentId);
  }

  /**
   * Shuffle the tournament's seeds uniformly at random — the unbiased draw next to the manual
   * reorder controls. Only allowed while SCHEDULED with no generated bracket (the same lock the
   * manual reordering obeys): once the bracket exists the pairings are locked.
   *
   * @return the reshuffled entries, seeds 1..n
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentEntry> randomizeSeeds(@NonNull final Long tournamentId) {
    requireEditableForSeeding(tournamentId);
    List<Long> shuffledIds =
        entryRepository.findByTournamentIdOrderBySeedAsc(tournamentId).stream()
            .map(TournamentEntry::getId)
            .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(shuffledIds);
    return reorderSeeds(tournamentId, shuffledIds);
  }

  /**
   * Replace one entrant with a different wrestler, keeping the vacated seed. Only allowed while
   * SCHEDULED with no generated bracket. Team entries are not replaceable this way — swapping one
   * member of a team entry would corrupt the entrant shape (wrestler + team on one entry); team
   * rosters are managed on the team itself.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public TournamentEntry replaceEntrant(
      @NonNull final Long tournamentId,
      @NonNull final Long entryId,
      @NonNull final Long newWrestlerId) {
    Tournament t = requireEditableForSeeding(tournamentId);
    TournamentEntry entry =
        entryRepository
            .findById(entryId)
            .filter(e -> e.getTournament().getId().equals(tournamentId))
            .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));
    if (entry.getTeam() != null) {
      throw new IllegalStateException(
          "Team entrant '"
              + entry.entrantName()
              + "' cannot be replaced with an individual wrestler — edit the team roster instead");
    }
    Wrestler replacement = wrestlerRepository.findById(newWrestlerId).orElse(null);
    if (replacement == null) {
      throw new IllegalArgumentException("Wrestler not found: " + newWrestlerId);
    }
    if (entryRepository.existsByTournamentIdAndWrestlerId(tournamentId, newWrestlerId)) {
      throw new IllegalStateException(
          replacement.getName() + " is already entered in this tournament");
    }
    entry.setWrestler(replacement);
    entryRepository.save(entry);
    return entry;
  }

  /** Load the tournament and gate seed edits: SCHEDULED with no generated rounds. */
  private Tournament requireEditableForSeeding(Long tournamentId) {
    Tournament t =
        tournamentRepository
            .findById(tournamentId)
            .orElseThrow(
                () -> new IllegalArgumentException("Tournament not found: " + tournamentId));
    if (t.getStatus() != TournamentStatus.SCHEDULED) {
      throw new IllegalStateException(
          "Seeds are locked once the tournament starts — '"
              + t.getName()
              + "' is "
              + t.getStatus());
    }
    if (!t.getRounds().isEmpty()) {
      throw new IllegalStateException(
          "Seeds are locked once the bracket is generated — '" + t.getName() + "' has rounds");
    }
    return t;
  }

  /**
   * Resolve the segment rule stipulation for a match:
   *
   * <ol>
   *   <li>Round's fixedRule (if set)
   *   <li>Random pick from tournament's allowedRules pool (if non-empty)
   *   <li>Fallback string (preserves legacy behaviour)
   * </ol>
   */
  private String resolveStipulation(Tournament tournament, TournamentRound round, String fallback) {
    if (round.getFixedRule() != null) {
      return round.getFixedRule().getName();
    }
    List<SegmentRule> pool = tournament.getAllowedRules();
    if (!pool.isEmpty()) {
      return pool.get(ThreadLocalRandom.current().nextInt(pool.size())).getName();
    }
    return fallback;
  }

  /**
   * Stipulation for a template-booked round match (ATW-etws). Precedence:
   *
   * <ol>
   *   <li>Round's fixedRule (booker-set, or the spec final rule stamped at the bracket final)
   *   <li>The template row's own rule (e.g. a type+rule AUTO_ATTACH pairing)
   *   <li>Random pick from the tournament's allowedRules pool
   *   <li>Fallback string
   * </ol>
   *
   * The template path diverges from the manual path ({@link #resolveStipulation}) by tier 2: a row
   * rule outranks the pool so an existing pairing keeps today's behavior.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public String resolveRoundStipulation(
      @NonNull Tournament tournament,
      @NonNull TournamentRound round,
      @Nullable SegmentRule rowRule,
      @NonNull String fallback) {
    if (round.getFixedRule() != null) {
      return round.getFixedRule().getName();
    }
    if (rowRule != null) {
      return rowRule.getName();
    }
    List<SegmentRule> pool = tournament.getAllowedRules();
    if (!pool.isEmpty()) {
      return pool.get(ThreadLocalRandom.current().nextInt(pool.size())).getName();
    }
    return fallback;
  }

  /** Record the winner of a match and, if the round is now fully decided, mark it complete. */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void recordMatchResult(TournamentMatch match, TournamentEntry winner) {
    // Every non-winning entrant is eliminated — for the classic two-entrant shape that is the
    // single loser; for multi-entrant matches (ATW-oloa) everyone else in the Free-for-All is
    // out too.
    for (TournamentEntry entrant : match.entrants()) {
      if (!entrant.equals(winner)) {
        entrant.setStatus(TournamentEntryStatus.ELIMINATED);
        entryRepository.save(entrant);
      }
    }

    match.setWinner(winner);
    matchRepository.save(match);

    // Check if round is complete
    TournamentRound round = match.getRound();
    boolean roundComplete = matchRepository.findByRoundIdAndWinnerIsNull(round.getId()).isEmpty();
    if (roundComplete) {
      round.setStatus(TournamentRoundStatus.COMPLETE);
      roundRepository.save(round);
    }
  }

  /**
   * Advance to the next round (generates matches for single-elimination). Generated matches are
   * attached to the in-memory rounds collection so callers holding this instance (template-fed
   * booking re-scans it right after advancing, ATW-yoo5) see them without re-fetching — the same
   * contract {@link #startTournament} documents for round 1.
   */
  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public List<TournamentMatch> advanceToNextRound(Tournament tournament) {
    TournamentFormat fmt =
        findFormat(tournament.getFormatId())
            .orElseThrow(() -> new IllegalStateException("Format not found"));

    if (fmt.isComplete(tournament)) {
      markWinner(tournament);
      return List.of();
    }
    List<TournamentMatch> generated = fmt.advanceRound(tournament, formatContext());
    attachGenerated(tournament, generated);
    return generated;
  }

  private void markWinner(Tournament tournament) {
    tournament.setStatus(TournamentStatus.COMPLETE);
    tournament.setEndDate(LocalDate.now());
    tournamentRepository.save(tournament);
    entryRepository
        .findByTournamentIdAndStatus(tournament.getId(), TournamentEntryStatus.ACTIVE)
        .stream()
        .findFirst()
        .ifPresent(
            entry -> {
              entry.setStatus(TournamentEntryStatus.WINNER);
              entryRepository.save(entry);
              // Briefcase-deciding tournaments are singles-only (creation validation rejects
              // teamSize), so this entry always carries a wrestler — the guard keeps a
              // shapeless/legacy row from NPEing rather than changing the semantics.
              if (entry.getWrestler() != null) {
                grantBriefcaseOnManualCompletion(tournament, entry.getWrestler());
              }
              recordTrophyWin(tournament, entry);
            });
  }

  /**
   * Records the trophy lineage row for a completed trophy tournament. Runs on every completion path
   * — markWinner is the single choke point (manual results in the detail view and bracket-played
   * ones both funnel through advanceToNextRound). Best-effort and idempotent: a failed stamp is
   * logged, never thrown (the tournament completion itself must not break), and a re-run cannot
   * duplicate the reign (unique tournament FK). The winner comes off the WINNER entry — a team
   * tournament stores the team FK and expands members at read time.
   */
  private void recordTrophyWin(Tournament tournament, TournamentEntry winnerEntry) {
    if (tournament.getTrophyName() == null) {
      return;
    }
    try {
      if (trophyReignRepository.existsByTournamentId(tournament.getId())) {
        log.debug(
            "Trophy '{}' already recorded for tournament {} — skipping (idempotent)",
            tournament.getTrophyName(),
            tournament.getId());
        return;
      }
      TrophyReign reign = new TrophyReign();
      reign.setTournament(tournament);
      reign.setTrophyName(tournament.getTrophyName());
      LocalDate wonOn = tournament.getEndDate() != null ? tournament.getEndDate() : LocalDate.now();
      reign.setWonOn(wonOn);
      reign.setYearWon(wonOn.getYear());
      reign.setWinnerWrestler(winnerEntry.getWrestler());
      reign.setWinnerTeam(winnerEntry.getTeam());
      reign.setUniverse(tournament.getUniverse());
      trophyReignRepository.save(reign);
      log.info(
          "Recorded trophy win: '{}' ({}), winner: {}",
          tournament.getTrophyName(),
          reign.getYearWon(),
          reign.getWinnerWrestler() != null
              ? reign.getWinnerWrestler().getName()
              : reign.getWinnerTeam().getName());
    } catch (Exception ex) {
      log.error(
          "Failed to record trophy win for tournament {} — the completion is unaffected",
          tournament.getId(),
          ex);
    }
  }

  /**
   * Manual-completion briefcase grant (ATW-8p72): when a briefcase-deciding tournament completes
   * without the final's segment ever being booked (results recorded by hand in the detail view),
   * adjudication will never fire for it — the grant happens here instead. The booking path is
   * untouched: its final has a segment and the grant flows through adjudication. Best-effort — a
   * failed grant is logged, never thrown (the tournament completion itself must not break).
   */
  private void grantBriefcaseOnManualCompletion(Tournament tournament, Wrestler winner) {
    if (!tournament.isBriefcaseDeciding()) {
      return;
    }
    boolean anySegmentBooked =
        tournament.getRounds().stream()
            .flatMap(r -> r.getMatches().stream())
            .anyMatch(m -> m.getSegment() != null);
    if (anySegmentBooked) {
      return; // bracket was played on shows — adjudication grants the briefcase
    }
    try {
      titleOpportunityService
          .grantFromTournament(tournament, winner)
          .ifPresentOrElse(
              opportunity ->
                  log.info(
                      "Granted briefcase '{}' to {} on manual completion of '{}'",
                      opportunity.getName(),
                      winner.getName(),
                      tournament.getName()),
              () ->
                  log.info(
                      "Briefcase grant for '{}' skipped (already granted or holder already holds"
                          + " one)",
                      tournament.getName()));
    } catch (Exception e) {
      log.error("Manual-completion briefcase grant failed for '{}'", tournament.getName(), e);
    }
  }

  /** Find the next upcoming show after today for use in the creation wizard. */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<Show> findNextShow(Universe universe) {
    List<Show> upcoming =
        showRepository.findByShowDateGreaterThanEqualOrderByShowDate(
            LocalDate.now(), PageRequest.of(0, 5));
    return upcoming.stream()
        .filter(s -> s.getUniverse() == null || s.getUniverse().equals(universe))
        .findFirst();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<TournamentRound> getCurrentRound(Tournament tournament) {
    return roundRepository.findFirstByTournamentIdAndStatusOrderByRoundNumberAsc(
        tournament.getId(), TournamentRoundStatus.IN_PROGRESS);
  }

  /**
   * Detail-view lineage for a trophy: every edition of the named trophy, newest first. Delegates to
   * {@link TrophyReignService} — the read side of what {@code markWinner} stamps.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<TrophyReign> trophyLineage(String trophyName) {
    return trophyReignService.lineageOf(trophyName);
  }

  /** The reign stamped for one tournament edition, if any (detail view winner line). */
  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public Optional<TrophyReign> trophyReignOf(Long tournamentId) {
    return Optional.ofNullable(trophyReignService.reignOfTournament(tournamentId));
  }
}

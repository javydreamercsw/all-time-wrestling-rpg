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

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamMember;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Shared booking core for every path that turns a bracket match into a resolved segment: the
 * template-fed booking funnel, the standalone tournament runner, and any future booking entry
 * point. Owns the open-match scan, segment creation through the ATW RPG match mechanics ({@link
 * NPCSegmentResolutionService}), the winner mapping back onto the bracket, and the stipulation and
 * segment-type resolution rules — all extracted verbatim from the template booking funnel so both
 * paths stay in lockstep by construction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class BracketMatchResolver {

  private final TournamentService tournamentService;
  private final NPCSegmentResolutionService segmentResolutionService;
  private final SegmentTypeService segmentTypeService;

  /**
   * The earliest round holding an open (winnerless) match — round 1 on a fresh bracket, later
   * rounds as earlier ones complete. Skips rounds already booked onto other shows (every match
   * linked to a segment).
   */
  Optional<TournamentMatch> nextBookableMatch(final Tournament tournament) {
    return tournament.getRounds().stream()
        .filter(r -> r.getStatus() != TournamentRoundStatus.COMPLETE)
        .flatMap(r -> r.getMatches().stream())
        .filter(m -> m.getWinner() == null && m.getSegment() == null)
        .min(Comparator.comparingInt(m -> m.getRound().getRoundNumber()));
  }

  /**
   * Whether booking {@code match} completes the bracket — the pacing math (total estimate minus
   * booked matches) says exactly one match remains.
   */
  boolean isBracketFinal(Tournament tournament, TournamentMatch match) {
    return tournamentService
        .findFormat(tournament.getFormatId())
        .map(fmt -> fmt.estimateTotalMatches(tournament))
        .map(total -> total - countBooked(tournament) == 1 && match.getWinner() == null)
        .orElse(false);
  }

  private int countBooked(Tournament tournament) {
    return (int)
        tournament.getRounds().stream()
            .flatMap(r -> r.getMatches().stream())
            .filter(m -> m.getSegment() != null || m.getWinner() != null)
            .count();
  }

  /**
   * Create the match segment for a bracket match. Title payoffs are flagged as title segments
   * carrying the linked championship so adjudication awards or defends it at finalization.
   *
   * <p>Narration carries the tournament context (tournament, round, stakes) so AI narration knows
   * what this match is and why it matters — the Narrate dialog generates from the narration field.
   *
   * <p>Team tournaments (ATW-01g2): sides are the entry teams' members in slot order for team
   * entries (the solo wrestler otherwise), and the segment type resolves from {@link
   * Tournament#getTeamSize()} through {@link #teamSegmentTypeOf(Tournament)} rather than the
   * format's round type.
   */
  Segment resolveSegment(
      final TournamentMatch match,
      final Tournament tournament,
      final SegmentType segmentType,
      final Show show,
      final String stipulation,
      final boolean titleOnTheLine) {
    Segment segment;
    // Team tournaments (ATW-01g2): the teamSize-resolved type (tag_team / trios) overrides the
    // format's round type whenever the tournament declares a team size — for multi-entrant team
    // fields and team-vs-team matches alike. Singles behavior is untouched.
    SegmentType teamType = teamSegmentTypeOf(tournament);
    SegmentType effectiveType = teamType != null ? teamType : segmentType;
    if (match.isMultiEntrant()) {
      // Multi-entrant match (Free-for-All qualifier, multi-man final): one team per entrant,
      // resolved by the multi-team path so every participant lands in their own slot.
      List<SegmentTeam> teams = match.entrants().stream().map(this::segmentTeamOf).toList();
      segment =
          segmentResolutionService.resolveMultiTeamSegment(teams, effectiveType, show, stipulation);
    } else {
      segment =
          segmentResolutionService.resolveTeamSegment(
              segmentTeamOf(match.getEntrant1()),
              segmentTeamOf(match.getEntrant2()),
              effectiveType,
              show,
              stipulation);
    }
    segment.setNarration(tournamentNotesOf(tournament, match, titleOnTheLine));
    if (titleOnTheLine && tournament.getLinkedTitle() != null) {
      segment.setIsTitleSegment(true);
      segment.getTitles().add(tournament.getLinkedTitle());
    }
    return segment;
  }

  /**
   * The segment team for a bracket entry: the entry's team members in slot order for a team entry,
   * the solo wrestler for a singles entry. Team names label the side for resolution logging and
   * narration.
   */
  SegmentTeam segmentTeamOf(TournamentEntry entry) {
    if (entry.getTeam() != null) {
      Team team = entry.getTeam();
      List<Wrestler> members =
          team.getMembers().stream().map(TeamMember::getWrestler).collect(Collectors.toList());
      return new SegmentTeam(members, team.getName());
    }
    return new SegmentTeam(entry.getWrestler());
  }

  /**
   * Resolve a team tournament's match segment type from the declared team size (ATW-01g2) — the
   * single place the teamSize → segment-type mapping lives. {@code DUO} books as the tag-team type
   * and {@code TRIO} as the trios type, both resolved against the seeded catalog through {@link
   * SegmentTypeService}; every booking path (weekly rounds, PLE payoff, host-show payoff,
   * standalone runner) funnels through {@link #resolveSegment}, so no per-format override is
   * needed.
   *
   * <p>Returns empty-mapped {@code null} (with a warning) when the tournament is not a team
   * tournament or the catalog lacks the type — the caller then falls back to the type the booking
   * path already carries, never failing the booking because of a missing catalog row.
   *
   * @return the resolved team segment type, or {@code null} to fall back to the caller's type
   */
  @Nullable SegmentType teamSegmentTypeOf(Tournament tournament) {
    TeamSize teamSize = tournament.getTeamSize();
    if (teamSize == null) {
      return null;
    }
    String code =
        switch (teamSize) {
          case DUO -> WellKnownSegmentType.TAG_TEAM.getCode();
          case TRIO -> WellKnownSegmentType.TRIOS.getCode();
        };
    Optional<SegmentType> resolved = segmentTypeService.findByCode(code);
    if (resolved.isEmpty()) {
      log.warn(
          "Team tournament '{}' books as '{}' — the '{}' segment type is missing from the catalog;"
              + " falling back to the assigned type",
          tournament.getName(),
          teamSize,
          code);
      return null;
    }
    return resolved.get();
  }

  /**
   * The segment type this tournament's non-final rounds book as: the format's round type code
   * resolved against the catalog, falling back to the standard one-on-one type. Empty (with a
   * warning) when neither exists — pacing skips the show rather than booking a wrong type.
   */
  Optional<SegmentType> roundSegmentTypeOf(Tournament tournament) {
    String code =
        tournamentService
            .findFormat(tournament.getFormatId())
            .map(TournamentFormat::getRoundSegmentTypeCode)
            .orElse(WellKnownSegmentType.ONE_ON_ONE.getCode());
    Optional<SegmentType> resolved = segmentTypeService.findByCode(code);
    if (resolved.isEmpty()) {
      log.warn(
          "Cannot book tournament rounds on show '{}': the '{}' segment type is missing",
          tournament.getFormatId(),
          code);
    }
    return resolved;
  }

  /**
   * The format's default round rule, resolved to a catalog rule name. The rule catalog seeds "No
   * DQ" (no_dq=1); when the row is missing the raw name still lands in the stipulation string
   * (narration-only at worst), and "Normal" stays the fallback when the format declares no default.
   */
  String defaultRoundRuleOf(Tournament tournament) {
    return tournamentService
        .findFormat(tournament.getFormatId())
        .map(TournamentFormat::getDefaultRoundRuleName)
        .filter(name -> !name.isEmpty())
        .orElse("Normal");
  }

  /** Booking narration describing the tournament stakes for AI narration. */
  String tournamentNotesOf(
      final Tournament tournament, final TournamentMatch match, final boolean titleOnTheLine) {
    StringBuilder notes = new StringBuilder();
    notes
        .append("Tournament match: ")
        .append(tournament.getName())
        .append(" — ")
        .append(roundNameOf(match))
        .append(". ");
    if (titleOnTheLine && tournament.getLinkedTitle() != null) {
      notes
          .append("The winner advances in the tournament; the ")
          .append(tournament.getLinkedTitle().getName())
          .append(" is on the line. ");
    } else {
      notes.append("The winner advances in the tournament. ");
    }
    if (tournament.isBriefcaseDeciding()) {
      notes
          .append("The winner earns the ")
          .append(tournament.getName())
          .append(" briefcase: a cashable opportunity for a title match against any reigning")
          .append(" champion. ");
    } else if (tournament.getTrophyName() != null) {
      notes
          .append("The winner lifts the ")
          .append(tournament.getTrophyName())
          .append(" — the tournament's named trophy. No championship changes hands; the trophy")
          .append(" and the legacy of its past winners are what the bracket is fought for. ");
    } else if (tournament.getLinkedTitle() == null
        && !tournament.isContenderDeciding()
        && !titleOnTheLine) {
      notes
          .append("There is no championship at stake — the win itself, and the bragging rights")
          .append(" that come with outlasting the field, are the prize. ");
    } else if (tournament.getLinkedTitle() != null && !titleOnTheLine) {
      notes
          .append("The tournament winner earns a ")
          .append(tournament.getLinkedTitle().getName())
          .append(" opportunity at the tournament's payoff show. ");
    }
    notes.append("Narrate the tournament stakes and the crowd's investment in the bracket.");
    return notes.toString();
  }

  /**
   * Derive the bracket winner from the booked segment's actual winners so the tournament state
   * matches the match result shown on the card.
   *
   * <p>Segment winners are wrestlers; {@code entrantId()} is a team id on a team entry — those live
   * in disjoint id spaces (and a wrestler/team id collision would misattribute the win). Team
   * entries therefore match by MEMBER wrestler ids: any segment winner being one of the entry
   * team's members makes that entry the winner. Package-private for direct testing — the full
   * booking path cannot materialize team segments until Task 6 builds sides from the entry team's
   * members (until then booking a team tournament fails earlier, in segment resolution).
   */
  TournamentEntry pickBracketWinner(final TournamentMatch match, final Segment segment) {
    // First segment winner that is one of the match's entrants — works for both the classic
    // two-entrant shape and multi-entrant matches (any entrant can win a Free-for-All).
    List<TournamentEntry> entrants = match.entrants();
    return segment.getWinners().stream()
        .flatMap(winner -> entrants.stream().filter(entry -> isEntryWinner(entry, winner)))
        .findFirst()
        .orElse(entrants.get(0));
  }

  /**
   * Whether {@code segmentWinner} wins {@code entry}: the wrestler himself for a singles entry, any
   * member of the entry's team for a team entry.
   */
  private boolean isEntryWinner(TournamentEntry entry, Wrestler segmentWinner) {
    if (entry.getTeam() != null) {
      return entry.getTeam().getMembers().stream()
          .anyMatch(member -> member.getWrestler().getId().equals(segmentWinner.getId()));
    }
    return entry.getWrestler() != null && entry.getWrestler().getId().equals(segmentWinner.getId());
  }

  String roundNameOf(final TournamentMatch match) {
    return match.getRound().getRoundName();
  }
}

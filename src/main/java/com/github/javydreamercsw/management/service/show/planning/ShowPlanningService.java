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
package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.SegmentsApprovedEvent;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningDtoMapper;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowPlanningService {
  private final SegmentRepository segmentRepository;
  private final RivalryService rivalryService;
  private final ShowPlanningDtoMapper mapper;
  private final Clock clock;
  private final TitleService titleService;
  private final ShowService showService;
  private final com.github.javydreamercsw.management.service.segment.SegmentService segmentService;
  private final com.github.javydreamercsw.management.service.segment.SegmentSummaryService
      segmentSummaryService;
  private final SegmentTypeService segmentTypeService;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final FactionService factionService;
  private final SegmentRuleRepository segmentRuleRepository;
  private final com.github.javydreamercsw.management.service.npc.NpcService npcService;
  private final ApplicationEventPublisher eventPublisher;
  private final TitleReignRepository titleReignRepository;

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public ShowPlanningContextDTO getShowPlanningContext(@NonNull final Show show) {
    if (show.getShowDate() == null) {
      throw new IllegalStateException(
          "Show '"
              + show.getName()
              + "' (id="
              + show.getId()
              + ") has no showDate set. "
              + "Set a scheduled date before opening Show Planning.");
    }
    if (show.getUniverse() == null) {
      throw new IllegalStateException(
          "Show '" + show.getName() + "' (id=" + show.getId() + ") has no universe assigned.");
    }

    ShowPlanningContext context = new ShowPlanningContext();

    // Get segments from the last 7 days
    Instant showDate = show.getShowDate().atStartOfDay(clock.getZone()).toInstant();
    context.setShowDate(showDate);
    context.setPremiumLiveEvent(show.isPremiumLiveEvent());
    Instant lastWeek = showDate.minus(7, ChronoUnit.DAYS);
    log.debug("Getting segments between {} and {}", lastWeek, showDate);
    List<Segment> lastWeekSegments = segmentRepository.findBySegmentDateBetween(lastWeek, showDate);
    log.debug("Found {} segments", lastWeekSegments.size());

    // New logic to generate summaries
    lastWeekSegments.forEach(
        segment -> {
          if ((segment.getSummary() == null || segment.getSummary().isEmpty())
              && (segment.getNarration() != null && !segment.getNarration().isEmpty())) {
            try {
              segmentSummaryService.summarizeSegment(segment.getId());
              // After summarizing, reload the segment to get the updated summary
              segmentService
                  .findById(segment.getId())
                  .ifPresent(
                      updatedSegment -> {
                        segment.setSummary(updatedSegment.getSummary());
                      });
            } catch (Exception e) {
              log.error("Failed to generate summary for segment: {}", segment.getId(), e);
            }
          }
        });

    context.setRecentSegments(lastWeekSegments);

    // Get current rivalries (full list; heat filtering happens in ShowPlanningDtoMapper)
    List<Rivalry> currentRivalries = rivalryService.getActiveRivalries();
    log.debug("Found {} active rivalries", currentRivalries.size());
    context.setCurrentRivalries(currentRivalries);

    // Get show template
    ShowTemplate template = new ShowTemplate();
    template.setShowName(show.getName());
    template.setDescription(show.getDescription());

    // Use template's expected values if set, otherwise fallback to show type defaults
    if (show.getTemplate() != null && show.getTemplate().getExpectedMatches() != null) {
      template.setExpectedMatches(show.getTemplate().getExpectedMatches());
    } else {
      template.setExpectedMatches(show.getType().getExpectedMatches());
    }

    if (show.getTemplate() != null && show.getTemplate().getExpectedPromos() != null) {
      template.setExpectedPromos(show.getTemplate().getExpectedPromos());
    } else {
      template.setExpectedPromos(show.getType().getExpectedPromos());
    }
    if (show.getTemplate() != null) {
      template.setGenderConstraint(show.getTemplate().getGenderConstraint());
    }
    context.setShowTemplate(template);

    // Get championships
    List<ShowPlanningChampionship> championships = new ArrayList<>();
    List<Title> activeTitles = titleService.getActiveTitles();

    Gender genderConstraint =
        show.getTemplate() != null ? show.getTemplate().getGenderConstraint() : null;

    log.debug(
        "Found {} active titles, filtering by gender: {}", activeTitles.size(), genderConstraint);
    for (Title title : activeTitles) {
      // Skip titles that don't match the gender constraint
      if (genderConstraint != null
          && title.getGender() != null
          && title.getGender() != genderConstraint) {
        continue;
      }

      ShowPlanningChampionship championship = new ShowPlanningChampionship();
      championship.setTitle(title);
      if (!title.getCurrentChampions().isEmpty()) {
        championship.getChampions().addAll(title.getCurrentChampions());
      }
      // Use only the current #1 contender(s) for this title
      List<Wrestler> numberOneContenders = title.getChallengers();
      if (numberOneContenders != null && !numberOneContenders.isEmpty()) {
        championship.getContenders().addAll(numberOneContenders);
      }

      // Calculate days since last defense.
      // Query directly instead of title.getCurrentReign() — getActiveTitles() may return
      // cached detached entities whose lazy titleReigns collection cannot be initialized
      // outside the original session (causes LazyInitializationException in async context).
      Instant lastDefense =
          titleReignRepository.findByTitleAndEndDateIsNull(title).stream()
              .map(com.github.javydreamercsw.management.domain.title.TitleReign::getStartDate)
              .findFirst()
              .orElse(null);

      // Check if there are any title matches after the start of the reign.
      // Query directly instead of title.getSegments() — same lazy-collection issue as above.
      Optional<Instant> lastMatch =
          segmentRepository.findByTitle(title).stream()
              .map(com.github.javydreamercsw.management.domain.show.segment.Segment::getSegmentDate)
              .max(Comparator.naturalOrder());

      if (lastMatch.isPresent() && (lastDefense == null || lastMatch.get().isAfter(lastDefense))) {
        lastDefense = lastMatch.get();
      }

      if (lastDefense != null) {
        championship.setDaysSinceLastDefense(ChronoUnit.DAYS.between(lastDefense, showDate));
      }

      championships.add(championship);
    }
    context.setChampionships(championships);

    // Get all wrestlers
    List<Wrestler> allWrestlers =
        wrestlerService.findAllFiltered(
            null, genderConstraint, show.getUniverse().getId(), null, null);

    log.debug("Found {} wrestlers in the roster", allWrestlers.size());
    context.setFullRoster(allWrestlers);

    // Get all factions, filtered by gender constraint
    List<Faction> allFactions =
        factionService.findAll().stream()
            .filter(
                faction ->
                    genderConstraint == null
                        || faction.getMembers().stream()
                            .anyMatch(m -> m.getGender() == genderConstraint))
            .collect(Collectors.toList());
    log.debug("Found {} factions after filtering", allFactions.size());
    context.setFactions(allFactions);

    // Get next PLE
    Optional<Show> nextPle =
        showService.getUpcomingShows(10).stream().filter(Show::isPremiumLiveEvent).findFirst();
    if (nextPle.isPresent()) {
      ShowPlanningPle ple = new ShowPlanningPle();
      ple.setPle(nextPle.get());
      context.setNextPle(ple);
    }

    return mapper.toDto(context);
  }

  /**
   * Returns active rivalries (heat ≥ 10) not covered by the given segments, sorted by heat
   * descending. Useful for surfacing the highest-priority unbooked feuds in UI hints and dialogs.
   */
  public List<Rivalry> getUnbookedRivalriesByHeat(
      @NonNull final List<ProposedSegment> proposedSegments) {
    return getUnbookedRivalriesByHeat(proposedSegments, rivalryService.getActiveRivalries());
  }

  /**
   * Returns rivalries (heat ≥ 10) not covered by the given segments from the supplied rivalry list,
   * sorted by heat descending.
   */
  public List<Rivalry> getUnbookedRivalriesByHeat(
      @NonNull final List<ProposedSegment> proposedSegments,
      @NonNull final List<Rivalry> activeRivalries) {
    return activeRivalries.stream()
        .filter(r -> r.getHeat() >= 10)
        .filter(r -> !isRivalryCovered(r, proposedSegments))
        .sorted(Comparator.comparingInt(Rivalry::getHeat).reversed())
        .collect(Collectors.toList());
  }

  /**
   * Validates the proposed card against active rivalries.
   *
   * <ul>
   *   <li>Warnings — MUST_BOOK (heat ≥ 10): rivalry not on the card. Advisory only; the booker may
   *       acknowledge and proceed when a large roster makes full coverage impossible.
   *   <li>Errors — STIPULATION_REQUIRED (heat ≥ 30): rivalry is on the card but has no match rule.
   *       Must be fixed before approval.
   * </ul>
   */
  public CardValidationResult validateCard(
      @NonNull final List<ProposedSegment> proposedSegments,
      @NonNull final List<Rivalry> activeRivalries) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    List<Rivalry> requiredRivalries =
        activeRivalries.stream().filter(r -> r.getHeat() >= 10).toList();

    for (Rivalry rivalry : requiredRivalries) {
      String w1 = rivalry.getWrestler1().getName();
      String w2 = rivalry.getWrestler2().getName();

      if (!isRivalryCovered(rivalry, proposedSegments)) {
        warnings.add(
            "MUST_BOOK rivalry not on card: "
                + w1
                + " vs "
                + w2
                + " (heat="
                + rivalry.getHeat()
                + ")");
      } else if (rivalry.getHeat() >= 30) {
        Optional<ProposedSegment> matchingSegment = findCoveringSegment(rivalry, proposedSegments);
        matchingSegment.ifPresent(
            seg -> {
              boolean hasStipulation = seg.getRules() != null && !seg.getRules().isEmpty();
              if (!hasStipulation) {
                errors.add(
                    "STIPULATION_REQUIRED rivalry booked without a stipulation: "
                        + w1
                        + " vs "
                        + w2
                        + " (heat="
                        + rivalry.getHeat()
                        + ")");
              }
            });
      }
    }

    return new CardValidationResult(errors, warnings);
  }

  /** Convenience overload — validates against live active rivalries from the database. */
  public CardValidationResult validateCard(@NonNull final List<ProposedSegment> proposedSegments) {
    return validateCard(proposedSegments, rivalryService.getActiveRivalries());
  }

  private boolean isRivalryCovered(
      final Rivalry rivalry, final List<ProposedSegment> proposedSegments) {
    return findCoveringSegment(rivalry, proposedSegments).isPresent();
  }

  private Optional<ProposedSegment> findCoveringSegment(
      final Rivalry rivalry, final List<ProposedSegment> proposedSegments) {
    String w1 = rivalry.getWrestler1().getName();
    String w2 = rivalry.getWrestler2().getName();
    return proposedSegments.stream()
        .filter(
            s ->
                (rivalry.getId() != null && rivalry.getId().equals(s.getRivalryId()))
                    || (s.getParticipants() != null
                        && s.getParticipants().contains(w1)
                        && s.getParticipants().contains(w2)))
        .findFirst();
  }

  @Transactional
  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public void approveSegments(
      @NonNull final Show show, @NonNull final List<ProposedSegment> proposedSegments) {
    if (show.getShowDate() == null) {
      throw new IllegalStateException(
          "Cannot approve segments for show '"
              + show.getName()
              + "' (id="
              + show.getId()
              + ") "
              + "because showDate is not set.");
    }

    CardValidationResult validation =
        validateCard(proposedSegments, rivalryService.getActiveRivalries());
    if (!validation.isValid()) {
      throw new IllegalStateException(
          "Show card validation failed for '"
              + show.getName()
              + "':\n"
              + String.join("\n", validation.getErrors()));
    }
    if (validation.hasWarnings()) {
      log.warn(
          "Approving card for '{}' with {} unbooked rivalry warnings",
          show.getName(),
          validation.getWarnings().size());
    }

    List<Segment> segmentsToSave = new ArrayList<>();
    int currentSegmentCount = segmentRepository.findByShow(show).size();
    for (int i = 0; i < proposedSegments.size(); i++) {
      ProposedSegment proposedSegment = proposedSegments.get(i);
      log.debug("Processing segment: {}", proposedSegment);
      Segment segment = new Segment();
      segment.setShow(show);
      Optional<com.github.javydreamercsw.management.domain.show.segment.type.SegmentType>
          segmentTypeOpt = segmentTypeService.findByName(proposedSegment.getType());
      if (segmentTypeOpt.isEmpty()) {
        log.warn("Segment type not found: {}. Skipping segment.", proposedSegment.getType());
        continue;
      }
      segment.setSegmentType(segmentTypeOpt.get());
      segment.setSegmentDate(show.getShowDate().atStartOfDay(clock.getZone()).toInstant());
      segment.setNarration(proposedSegment.getNarration());
      segment.setSummary(proposedSegment.getSummary());
      segment.setNotes(proposedSegment.getNotes());
      segment.setRivalryId(proposedSegment.getRivalryId());
      segment.setSegmentOrder(currentSegmentCount + i + 1);
      segment.setIsTitleSegment(proposedSegment.getIsTitleSegment());
      if (proposedSegment.getTitles() != null && !proposedSegment.getTitles().isEmpty()) {
        segment.setTitles(proposedSegment.getTitles());
      }

      if (proposedSegment.getRefereeName() != null) {
        segment.setReferee(npcService.findByName(proposedSegment.getRefereeName()));
      }

      // First, clear existing participants if any, to re-sync
      segment.getParticipants().clear();

      // Add participants with team assignments when available.
      // Prefer ID-based lookup; fall back to name if IDs are absent (e.g. manual/UI-created).
      List<Wrestler> actualParticipants = new ArrayList<>();
      if (proposedSegment.getTeamIds() != null && !proposedSegment.getTeamIds().isEmpty()) {
        List<List<Long>> teamIds = proposedSegment.getTeamIds();
        for (int teamIndex = 0; teamIndex < teamIds.size(); teamIndex++) {
          int teamNumber = teamIndex + 1;
          for (Long wrestlerId : teamIds.get(teamIndex)) {
            wrestlerRepository
                .findById(wrestlerId)
                .ifPresent(
                    wrestler -> {
                      segment.addParticipant(wrestler, teamNumber);
                      actualParticipants.add(wrestler);
                    });
          }
        }
      } else if (proposedSegment.getTeams() != null && !proposedSegment.getTeams().isEmpty()) {
        List<List<String>> teams = proposedSegment.getTeams();
        for (int teamIndex = 0; teamIndex < teams.size(); teamIndex++) {
          int teamNumber = teamIndex + 1;
          for (String participantName : teams.get(teamIndex)) {
            wrestlerRepository
                .findByName(participantName)
                .ifPresent(
                    wrestler -> {
                      segment.addParticipant(wrestler, teamNumber);
                      actualParticipants.add(wrestler);
                    });
          }
        }
      } else if (proposedSegment.getParticipantIds() != null
          && !proposedSegment.getParticipantIds().isEmpty()) {
        for (Long wrestlerId : proposedSegment.getParticipantIds()) {
          wrestlerRepository
              .findById(wrestlerId)
              .ifPresent(
                  wrestler -> {
                    segment.addParticipant(wrestler);
                    actualParticipants.add(wrestler);
                  });
        }
      } else if (proposedSegment.getParticipants() != null) {
        for (String participantName : proposedSegment.getParticipants()) {
          wrestlerRepository
              .findByName(participantName)
              .ifPresent(
                  wrestler -> {
                    segment.addParticipant(wrestler);
                    actualParticipants.add(wrestler);
                  });
        }
      }

      // Set winners — prefer ID-based lookup, fall back to name
      List<Wrestler> actualWinners = new ArrayList<>();
      if (proposedSegment.getWinners() != null && !proposedSegment.getWinners().isEmpty()) {
        for (String winnerName : proposedSegment.getWinners()) {
          wrestlerRepository.findByName(winnerName).ifPresent(actualWinners::add);
        }
      }
      segment.setWinners(actualWinners);

      if (proposedSegment.getRules() != null && !proposedSegment.getRules().isEmpty()) {
        List<com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule>
            newSegmentRules =
                proposedSegment.getRules().stream()
                    .map(ruleName -> segmentRuleRepository.findByName(ruleName))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        segment.syncSegmentRules(newSegmentRules);
      }
      segmentsToSave.add(segment);
    }
    segmentRepository.saveAll(segmentsToSave);
    log.debug("Approved and saved {} segments for show: {}", segmentsToSave.size(), show.getName());
    eventPublisher.publishEvent(new SegmentsApprovedEvent(this, show));
  }
}

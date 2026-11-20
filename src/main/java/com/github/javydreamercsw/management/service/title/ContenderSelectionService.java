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

import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldown;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldownRepository;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.event.ContenderDesignatedEvent;
import com.github.javydreamercsw.management.event.ContenderTieDetectedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automates #1 contender selection for titles. After a title match (or a resolved feud) the next
 * contender is picked from the rankings; when the top-ranked wrestlers are too close to call, a
 * tie-breaker match is suggested instead. All thresholds are configurable via {@link
 * GameSettingService}.
 *
 * <p>Two designation paths: {@link #designateAsContender} for implicit picks from the rankings
 * (tier-gated at the sink), and {@link #designateEarnedContender} for shots earned in the ring —
 * contender-deciding wins and champion-slayer wins — which bypass the tier gate but still respect
 * the failed-challenge cooldown.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContenderSelectionService {

  private final TitleService titleService;
  private final RankingService rankingService;
  private final GameSettingService gameSettingService;
  private final WrestlerRepository wrestlerRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final WrestlerTitleCooldownRepository cooldownRepository;
  private final WrestlerService wrestlerService;

  /**
   * Designates the given wrestler as the #1 contender for the title and publishes a {@link
   * ContenderDesignatedEvent} on success.
   *
   * @param title the title being contended for
   * @param wrestler the new #1 contender
   * @return true when the wrestler was registered as a challenger
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public boolean designateAsContender(
      @NonNull final Title title, @NonNull final Wrestler wrestler) {
    // The #1 contender spot is exclusive — replace the challenger list instead of appending,
    // otherwise the previous contender lingers alongside the new one.
    TitleService.ChallengeResult result =
        titleService.setSoleChallenger(title.getId(), wrestler.getId());
    if (result.success()) {
      log.info("{} designated as #1 contender for {}", wrestler.getName(), title.getName());
      eventPublisher.publishEvent(new ContenderDesignatedEvent(this, title, wrestler));
    } else {
      log.warn(
          "Could not designate {} as contender for {}: {}",
          wrestler.getName(),
          title.getName(),
          result.message());
    }
    return result.success();
  }

  /**
   * Designates the winner of an earned shot (contender-deciding win) as the title's #1 contender,
   * bypassing the tier percentile gate but still respecting the failed-challenge cooldown. A
   * wrestler on cooldown for this title cannot be designated even by winning.
   *
   * @param title the title being contended for
   * @param wrestler the winner of the contender-deciding match
   * @return true when the wrestler was registered as the #1 contender
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public boolean designateEarnedContender(
      @NonNull final Title title, @NonNull final Wrestler wrestler) {
    if (isOnCooldown(title, wrestler)) {
      log.info(
          "{} is on failed-challenge cooldown for {} — earned shot not granted",
          wrestler.getName(),
          title.getName());
      return false;
    }
    TitleService.ChallengeResult result =
        titleService.grantEarnedChallengerStatus(title.getId(), wrestler.getId(), true);
    if (result.success()) {
      log.info(
          "{} earned the #1 contender spot for {} (contender-deciding win)",
          wrestler.getName(),
          title.getName());
      eventPublisher.publishEvent(new ContenderDesignatedEvent(this, title, wrestler));
    } else {
      log.warn(
          "Could not grant earned contender status to {} for {}: {}",
          wrestler.getName(),
          title.getName(),
          result.message());
    }
    return result.success();
  }

  /**
   * Makes the winner over the reigning champion available as a challenger for the champion's title
   * (champion-slayer rule): appends them to the challenger list without displacing the current #1
   * contender. Respects the failed-challenge cooldown. No designation event — the champion-defeated
   * notification covers visibility.
   *
   * @param title the champion's title
   * @param winner the wrestler who beat the champion
   * @return true when the winner was appended to the challenger list
   */
  @Transactional
  public boolean grantChampionSlayerAvailability(
      @NonNull final Title title, @NonNull final Wrestler winner) {
    if (isOnCooldown(title, winner)) {
      log.info(
          "{} is on failed-challenge cooldown for {} — champion-slayer availability not granted",
          winner.getName(),
          title.getName());
      return false;
    }
    TitleService.ChallengeResult result =
        titleService.grantEarnedChallengerStatus(title.getId(), winner.getId(), false);
    if (result.success()) {
      log.info(
          "{} is now available as a challenger for {} (defeated the champion in a non-title"
              + " match)",
          winner.getName(),
          title.getName());
    } else {
      log.debug(
          "Champion-slayer availability not granted to {} for {}: {}",
          winner.getName(),
          title.getName(),
          result.message());
    }
    return result.success();
  }

  /**
   * Booking-gate input: which of the given participants do NOT meet the ranking criteria to contend
   * for the title. A participant passes when they appear in the computed rankings (tier filter with
   * the all-comers fallback) or are already a designated challenger (earned shots stay eligible).
   * On-cooldown wrestlers still pass — the cooldown blocks designation, not participation.
   */
  public List<Wrestler> contendersMissingRankingCriteria(
      @NonNull final Title title, @NonNull final Collection<Wrestler> participants) {
    Set<Long> eligibleIds = new HashSet<>();
    rankingService.getRankedContenders(title.getId()).stream()
        .filter(RankedWrestlerDTO.class::isInstance)
        .map(RankedWrestlerDTO.class::cast)
        .map(RankedWrestlerDTO::getId)
        .forEach(eligibleIds::add);
    title.getChallengers().forEach(challenger -> eligibleIds.add(challenger.getId()));
    return participants.stream().filter(w -> !eligibleIds.contains(w.getId())).toList();
  }

  /** Failed-challenge cooldown check (same arithmetic as RankingService's DTO annotation). */
  private boolean isOnCooldown(@NonNull final Title title, @NonNull final Wrestler wrestler) {
    if (title.getUniverse() == null) {
      return false;
    }
    WrestlerState state =
        wrestlerService.getOrCreateState(wrestler.getId(), title.getUniverse().getId());
    if (state == null || state.getId() == null) {
      return false;
    }
    int cooldownDefenses = gameSettingService.getContenderFailedChallengeCooldownDefenses();
    Optional<WrestlerTitleCooldown> cooldown =
        cooldownRepository.findByWrestlerState_IdAndTitle_Id(state.getId(), title.getId());
    return cooldown.isPresent()
        && title.getDefenseCount() - cooldown.get().getDefenseCountAtChallenge() < cooldownDefenses;
  }

  /**
   * Automatically selects the next #1 contender for the title based on the current rankings.
   *
   * <p>When the gap between the top-ranked wrestlers is within the configured tie threshold, a
   * {@link ContenderTieDetectedEvent} is published so a tie-breaker match can be suggested instead
   * of designating anyone outright. Does nothing when auto-selection is disabled, the title is a
   * team championship, or the rankings are empty.
   *
   * @param title the title needing a new contender
   */
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @Transactional
  public void autoSelectNextContender(@NonNull final Title title) {
    if (!gameSettingService.isContenderAutoSelectEnabled()) {
      log.debug("Contender auto-selection disabled; skipping title {}", title.getName());
      return;
    }
    if (title.getChampionshipType() == ChampionshipType.TEAM) {
      log.debug("Contender auto-selection not supported for team title {}", title.getName());
      return;
    }

    List<RankedWrestlerDTO> ranked = getRankedWrestlers(title);
    if (ranked.isEmpty()) {
      log.debug("No ranked contenders available for title {}", title.getName());
      return;
    }

    List<RankedWrestlerDTO> tied = findTiedContenders(ranked);
    if (tied.size() > 1) {
      log.info(
          "Contender tie detected for {}: {} wrestlers within {}% of the leader",
          title.getName(), tied.size(), gameSettingService.getContenderTieThresholdPercent());
      eventPublisher.publishEvent(new ContenderTieDetectedEvent(this, title, tied));
      return;
    }

    RankedWrestlerDTO top = ranked.get(0);
    wrestlerRepository
        .findById(top.getId())
        .ifPresent(wrestler -> designateAsContender(title, wrestler));
  }

  /**
   * Returns true when the top-two ranked wrestlers are within the configured tie threshold of each
   * other (by fan count).
   */
  public boolean isTie(@NonNull final List<RankedWrestlerDTO> ranked) {
    return findTiedContenders(ranked).size() > 1;
  }

  /**
   * Returns every wrestler whose fan count is within the configured percentage of the top-ranked
   * wrestler's fans. A single-element result means there is a clear #1 contender.
   */
  private List<RankedWrestlerDTO> findTiedContenders(final List<RankedWrestlerDTO> ranked) {
    List<RankedWrestlerDTO> tied = new ArrayList<>();
    if (ranked.isEmpty()) {
      return tied;
    }
    long topFans = ranked.get(0).getFans() == null ? 0L : ranked.get(0).getFans();
    int thresholdPercent = gameSettingService.getContenderTieThresholdPercent();
    for (RankedWrestlerDTO candidate : ranked) {
      long fans = candidate.getFans() == null ? 0L : candidate.getFans();
      if (topFans == 0L || ((topFans - fans) * 100.0 / topFans) <= thresholdPercent) {
        tied.add(candidate);
      } else {
        break; // ranked list is sorted; everyone after is further away
      }
    }
    return tied;
  }

  private List<RankedWrestlerDTO> getRankedWrestlers(final Title title) {
    return rankingService.getRankedContenders(title.getId()).stream()
        .filter(RankedWrestlerDTO.class::isInstance)
        .map(RankedWrestlerDTO.class::cast)
        .filter(dto -> !dto.isOnCooldown())
        .toList();
  }
}

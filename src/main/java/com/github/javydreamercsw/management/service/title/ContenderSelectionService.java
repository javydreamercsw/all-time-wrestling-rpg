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
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.event.ContenderDesignatedEvent;
import com.github.javydreamercsw.management.event.ContenderTieDetectedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import java.util.ArrayList;
import java.util.List;
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
        .toList();
  }
}

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
package com.github.javydreamercsw.management.event.inbox;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldown;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldownRepository;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a per-title contender cooldown to every losing challenger when a championship is
 * defended. The cooldown prevents the wrestler from being auto-selected as #1 contender for the
 * same title until the title has been defended the configured number of additional times.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChampionshipDefendedCooldownListener
    implements ApplicationListener<ChampionshipDefendedEvent> {

  private final TitleRepository titleRepository;
  private final WrestlerTitleCooldownRepository cooldownRepository;
  private final WrestlerService wrestlerService;

  @Override
  @Transactional
  public void onApplicationEvent(@NonNull final ChampionshipDefendedEvent event) {
    if (event.getChallengers().isEmpty()) {
      return;
    }

    Title title =
        titleRepository
            .findById(event.getTitleId())
            .orElseThrow(
                () -> new IllegalStateException("Title not found for id: " + event.getTitleId()));

    title.setDefenseCount(title.getDefenseCount() + 1);
    titleRepository.save(title);
    long defenseCountSnapshot = title.getDefenseCount();

    Long universeId = title.getUniverse() != null ? title.getUniverse().getId() : 1L;

    for (Wrestler challenger : event.getChallengers()) {
      WrestlerState state = wrestlerService.getOrCreateState(challenger.getId(), universeId);

      WrestlerTitleCooldown cooldown =
          cooldownRepository
              .findByWrestlerState_IdAndTitle_Id(state.getId(), title.getId())
              .orElseGet(
                  () -> WrestlerTitleCooldown.builder().wrestlerState(state).title(title).build());

      cooldown.setDefenseCountAtChallenge(defenseCountSnapshot);
      cooldownRepository.save(cooldown);

      log.info(
          "Applied title challenge cooldown to {} for title '{}' (defense #{} — must sit out"
              + " configured defenses)",
          challenger.getName(),
          event.getTitleName(),
          defenseCountSnapshot);
    }
  }
}

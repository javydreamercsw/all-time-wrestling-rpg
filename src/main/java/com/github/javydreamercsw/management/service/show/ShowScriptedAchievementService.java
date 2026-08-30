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
package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeNames;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.dto.ShowFinalizedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.achievement.ScriptedAchievementEvaluator;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates scripted achievements whose unlock conditions depend on show-level context: which
 * wrestlers appeared in a promo, who was in the main event, etc. Listens to {@link
 * ShowFinalizedEvent} and calls {@link ScriptedAchievementEvaluator} with the following context
 * variables available to Groovy scripts:
 *
 * <ul>
 *   <li>{@code show} — the finalized {@link com.github.javydreamercsw.management.domain.show.Show}
 *   <li>{@code segments} — ordered {@code List<Segment>} for the show
 *   <li>{@code mainEventParticipants} — {@code List<Wrestler>} in the main-event segment
 *   <li>{@code promoSegments} — {@code List<Segment>} of PROMO-type segments
 *   <li>{@code promoParticipants} — distinct {@code List<Wrestler>} who appeared in any promo
 * </ul>
 *
 * <p>Scripts should guard against being evaluated at a different call site using {@code
 * binding.hasVariable('mainEventParticipants')} so they return {@code false} rather than throwing
 * when the roster-level context lacks these variables.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShowScriptedAchievementService {

  private final ScriptedAchievementEvaluator scriptedAchievementEvaluator;
  private final LegacyService legacyService;
  private final GameSettingService gameSettingService;

  @EventListener
  @Transactional
  public void onShowFinalized(final ShowFinalizedEvent event) {
    List<Segment> segments = event.getSegments();
    if (segments.isEmpty()) {
      return;
    }

    Segment mainEvent =
        segments.stream()
            .filter(Segment::isMainEvent)
            .findFirst()
            .orElse(segments.get(segments.size() - 1));

    List<Segment> promoSegments =
        segments.stream()
            .filter(s -> SegmentTypeNames.PROMO.equals(s.getSegmentType().getName()))
            .toList();

    List<Wrestler> promoParticipants =
        promoSegments.stream().flatMap(s -> s.getWrestlers().stream()).distinct().toList();

    Set<Account> accounts =
        segments.stream()
            .flatMap(s -> s.getWrestlers().stream())
            .map(Wrestler::getAccount)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    if (accounts.isEmpty()) {
      return;
    }

    for (Account account : accounts) {
      // Scope the participant lists to the account's OWN wrestlers: wrestler-specific
      // conditions ("Johnny cut a promo") must only unlock for the account that wrestler
      // is assigned to, not for everyone who happened to be on the same show.
      Map<String, Object> context = new HashMap<>();
      context.put("show", event.getShow());
      context.put("segments", segments);
      context.put("mainEventParticipants", ownedBy(mainEvent.getWrestlers(), account));
      context.put("promoSegments", promoSegments);
      context.put("promoParticipants", ownedBy(promoParticipants, account));
      context.put(
          "gameDate",
          gameSettingService.getCurrentGameDate().atStartOfDay(ZoneOffset.UTC).toInstant());
      scriptedAchievementEvaluator
          .resolveNewlyUnlockedKeys(account, context)
          .forEach(key -> legacyService.unlockAchievement(account, key));
    }
  }

  private static List<Wrestler> ownedBy(final List<Wrestler> wrestlers, final Account account) {
    return wrestlers.stream()
        .filter(w -> w.getAccount() != null && account.getId().equals(w.getAccount().getId()))
        .toList();
  }
}

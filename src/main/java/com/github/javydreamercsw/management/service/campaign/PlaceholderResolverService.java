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
package com.github.javydreamercsw.management.service.campaign;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves {{RIVAL}} and {{CHAMP}} placeholder tokens in encounter text and opponent name fields,
 * and handles opponent pool selection with gender and exclusion filters.
 *
 * <p>Token reference:
 *
 * <ul>
 *   <li>{{RIVAL}} — the player's current campaign rival (campaign_state.rival_id)
 *   <li>{{CHAMP}} — first current champion of the "ATW World" title
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceholderResolverService {

  static final String TOKEN_RIVAL = "{{RIVAL}}";
  static final String TOKEN_CHAMP = "{{CHAMP}}";
  private static final String ATW_WORLD_TITLE = "ATW World";

  private final WrestlerRepository wrestlerRepository;
  private final TitleRepository titleRepository;
  private final TitleReignRepository titleReignRepository;
  private final CampaignStateRepository campaignStateRepository;

  private final Random random = new Random();

  /**
   * Resolves all known placeholders in {@code text} for the given campaign. Returns null if text is
   * null.
   */
  public String resolve(@NonNull final Campaign campaign, final String text) {
    if (text == null) {
      return null;
    }
    String result = text;
    if (result.contains(TOKEN_RIVAL)) {
      String rivalName = getRivalName(campaign).orElse(null);
      if (rivalName != null) {
        result = result.replace(TOKEN_RIVAL, rivalName);
      } else {
        log.warn(
            "{{RIVAL}} placeholder used but campaign has no rival yet — token left unresolved");
      }
    }
    if (result.contains(TOKEN_CHAMP)) {
      String champName = getChampName().orElse(null);
      if (champName != null) {
        result = result.replace(TOKEN_CHAMP, champName);
      } else {
        log.warn(
            "{{CHAMP}} placeholder used but ATW World title has no current champion — token left"
                + " unresolved");
      }
    }
    return result;
  }

  /**
   * Selects an opponent name for a MATCH choice.
   *
   * <p>Priority:
   *
   * <ol>
   *   <li>If {@code forcedOpponentName} is non-blank: resolve placeholders and return it.
   *   <li>If {@code pool} is non-empty: resolve placeholders in each entry, then pick one at random
   *       respecting {@code genderFilter} and {@code excluded}.
   *   <li>Fallback: full active roster minus the player, filtered by {@code genderFilter} and
   *       {@code excluded}.
   * </ol>
   */
  @Transactional(readOnly = true)
  public String resolveOpponent(
      @NonNull final Campaign campaign,
      final String forcedOpponentName,
      final List<String> pool,
      final Gender genderFilter,
      final List<String> excluded) {

    // 1. Forced name (with placeholder resolution)
    if (forcedOpponentName != null && !forcedOpponentName.isBlank()) {
      return resolve(campaign, forcedOpponentName);
    }

    Long playerId = campaign.getWrestler().getId();

    // Resolve placeholders in the exclusion list once, reuse for both pool and roster paths
    List<String> resolvedExcluded =
        excluded == null
            ? List.of()
            : excluded.stream()
                .map(e -> resolve(campaign, e))
                .filter(e -> e != null && !e.contains("{{"))
                .toList();

    // 2. Named pool
    if (pool != null && !pool.isEmpty()) {
      List<String> resolvedPool = new ArrayList<>();
      for (String entry : pool) {
        String resolved = resolve(campaign, entry);
        if (resolved != null && !resolved.contains("{{")) {
          resolvedPool.add(resolved);
        }
      }
      List<Wrestler> candidates =
          resolvedPool.stream()
              .flatMap(name -> wrestlerRepository.findByName(name).stream())
              .filter(w -> !w.getId().equals(playerId))
              .filter(w -> Boolean.TRUE.equals(w.getActive()))
              .filter(w -> genderFilter == null || genderFilter == w.getGender())
              .filter(w -> !resolvedExcluded.contains(w.getName()))
              .toList();
      if (!candidates.isEmpty()) {
        return candidates.get(random.nextInt(candidates.size())).getName();
      }
      log.warn("opponentPool resolved to no valid candidates — falling back to full roster");
    }

    // 3. Full active roster fallback
    List<Wrestler> roster =
        wrestlerRepository.findAll().stream()
            .filter(w -> !w.getId().equals(playerId))
            .filter(w -> Boolean.TRUE.equals(w.getActive()))
            .filter(w -> genderFilter == null || genderFilter == w.getGender())
            .filter(w -> !resolvedExcluded.contains(w.getName()))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

    if (roster.isEmpty()) {
      throw new IllegalStateException("No eligible opponents available for match.");
    }
    String chosen = roster.get(random.nextInt(roster.size())).getName();
    log.info("No opponent specified — randomly selected: {}", chosen);
    return chosen;
  }

  /**
   * Assigns a rival to the campaign from the given pool/filter, then saves the state. Used when
   * {@code assignRivalBeforeMatch=true} on a static choice.
   */
  @Transactional
  public void assignRival(
      @NonNull final Campaign campaign,
      final List<String> pool,
      final Gender genderFilter,
      final List<String> excluded) {
    String chosen = resolveOpponent(campaign, null, pool, genderFilter, excluded);
    wrestlerRepository
        .findByName(chosen)
        .ifPresent(
            rival -> {
              campaign.getState().setRival(rival);
              campaignStateRepository.save(campaign.getState());
              log.info("Assigned rival {} to campaign {}", rival.getName(), campaign.getId());
            });
  }

  private Optional<String> getRivalName(Campaign campaign) {
    if (campaign.getState() == null || campaign.getState().getRival() == null) {
      return Optional.empty();
    }
    return Optional.of(campaign.getState().getRival().getName());
  }

  private Optional<String> getChampName() {
    return titleRepository
        .findByName(ATW_WORLD_TITLE)
        .map(Title::getCurrentChampions)
        .filter(champs -> !champs.isEmpty())
        .map(
            champs ->
                champs.stream()
                    .map(Wrestler::getName)
                    .collect(java.util.stream.Collectors.joining(" & ")));
  }
}

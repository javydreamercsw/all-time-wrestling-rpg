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
package com.github.javydreamercsw.management.service.ranking;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedTeamDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RankingService {

  private final TitleRepository titleRepository;
  private final WrestlerRepository wrestlerRepository;
  private final FactionRepository factionRepository;
  private final TeamRepository teamRepository;
  private final TierBoundaryService tierBoundaryService;

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<ChampionshipDTO> getChampionships() {
    return titleRepository.findAll().stream()
        .filter(Title::getIncludeInRankings)
        .map(this::toChampionshipDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<?> getRankedContenders(@NonNull Long championshipId) {
    Optional<Title> titleOpt = titleRepository.findById(championshipId);
    if (titleOpt.isEmpty()) {
      return List.of();
    }
    Title title = titleOpt.get();
    WrestlerTier tier = title.getTier();

    Gender gender = title.getGender() == null ? Gender.MALE : title.getGender();

    if (title.getChampionshipType() == ChampionshipType.TEAM) {
      List<Team> contenders =
          teamRepository.findAll().stream()
              .filter(
                  team ->
                      team.getWrestler1().getGender() == gender
                          && team.getWrestler2().getGender() == gender
                          && team.isActive()
                          && Boolean.TRUE.equals(team.getWrestler1().getActive())
                          && Boolean.TRUE.equals(team.getWrestler2().getActive()))
              .collect(Collectors.toList());

      // Remove champion team from the contender list
      title
          .getCurrentReign()
          .ifPresent(
              reign -> {
                List<Wrestler> champions = reign.getChampions();
                if (champions.size() == 2) {
                  contenders.removeIf(
                      contender ->
                          (contender.getWrestler1().equals(champions.get(0))
                                  && contender.getWrestler2().equals(champions.get(1)))
                              || (contender.getWrestler1().equals(champions.get(1))
                                  && contender.getWrestler2().equals(champions.get(0))));
                }
              });

      AtomicInteger rank = new AtomicInteger(1);
      return contenders.stream()
          .sorted(
              Comparator.comparing(
                      (Team team) -> team.getWrestler1().getFans() + team.getWrestler2().getFans())
                  .reversed())
          .map(t -> toRankedTeamDTO(t, rank.getAndIncrement()))
          .collect(Collectors.toList());
    } else {
      log.debug(
          "Entering getRankedContenders for single championship. Title tier ordinal: {}",
          tier.ordinal());
      List<Wrestler> initialContenders =
          wrestlerRepository.findAllByGenderAndActive(gender, true).stream().toList();
      log.debug(
          "After findAllByGenderAndActive. Count: {}, Names: {}",
          initialContenders.size(),
          initialContenders.stream().map(Wrestler::getName).collect(Collectors.joining(", ")));

      List<Wrestler> contenders =
          initialContenders.stream()
              .filter(
                  wrestler -> {
                    boolean passesFilter = wrestler.getTier().ordinal() >= tier.ordinal();
                    log.debug(
                        "Wrestler {} (Tier: {}, Ordinal: {}) vs Title Tier {} (Ordinal: {})."
                            + " Passes: {}",
                        wrestler.getName(),
                        wrestler.getTier(),
                        wrestler.getTier().ordinal(),
                        tier,
                        tier.ordinal(),
                        passesFilter);
                    return passesFilter;
                  })
              .collect(Collectors.toList());
      log.debug(
          "After tier filter. Count: {}, Names: {}",
          contenders.size(),
          contenders.stream().map(Wrestler::getName).collect(Collectors.joining(", ")));

      // Remove champions from the contender list
      title
          .getCurrentReign()
          .ifPresent(
              reign -> {
                log.debug(
                    "Champion removal stage. Current champions: {}",
                    reign.getChampions().stream()
                        .map(Wrestler::getName)
                        .collect(Collectors.joining(", ")));
                contenders.removeAll(reign.getChampions());
                log.debug(
                    "After champion removal. Count: {}, Names: {}",
                    contenders.size(),
                    contenders.stream().map(Wrestler::getName).collect(Collectors.joining(", ")));
              });
      
      // Filter by gender and active status
      contenders.removeIf(
          wrestler -> wrestler.getGender() != gender || !Boolean.TRUE.equals(wrestler.getActive()));

      AtomicInteger rank = new AtomicInteger(1);
      return contenders.stream()
          .sorted(
              (w1, w2) -> {
                // Primary sort: wrestlers in the exact title tier come first
                boolean w1IsTitleTier = w1.getTier() == tier;
                boolean w2IsTitleTier = w2.getTier() == tier;

                if (w1IsTitleTier && !w2IsTitleTier) {
                  return -1; // w1 is in title tier, w2 is not, so w1 comes first
                }
                if (!w1IsTitleTier && w2IsTitleTier) {
                  return 1; // w2 is in title tier, w1 is not, so w2 comes first
                }

                // Secondary sort: if both are in title tier or both are not, sort by tier (highest
                // first)
                // WrestlerTier ordinal: ICON(0), MAIN_EVENTER(1), MIDCARDER(2), CONTENDER(3),
                // RISER(4), ROOKIE(5)
                // So, lower ordinal means higher tier.
                int tierComparison =
                    Integer.compare(w1.getTier().ordinal(), w2.getTier().ordinal());
                if (tierComparison != 0) {
                  return tierComparison
                      * -1; // Invert to sort by highest tier (highest ordinal) first
                }

                // Tertiary sort: if tiers are the same, sort by fans (descending)
                return Long.compare(w2.getFans(), w1.getFans());
              })
          .map(w -> toRankedWrestlerDTO(w, rank.getAndIncrement()))
          .toList();
    }
  }

  @Transactional(readOnly = true)
  @PreAuthorize("isAuthenticated()")
  public List<ChampionDTO> getCurrentChampions(@NonNull Long championshipId) {
    return titleRepository
        .findById(championshipId)
        .flatMap(Title::getCurrentReign)
        .map(
            reign ->
                reign.getChampions().stream()
                    .map(
                        champion ->
                            ChampionDTO.builder()
                                .id(champion.getId())
                                .name(champion.getName())
                                .fans(champion.getFans())
                                .reignDays(reign.getReignLengthDays(Instant.now()))
                                .build())
                    .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  private ChampionshipDTO toChampionshipDTO(@NonNull Title title) {
    return ChampionshipDTO.builder()
        .id(title.getId())
        .name(title.getName())
        .imageName(toImageName(title.getName()))
        .tier(title.getTier())
        .build();
  }

  private RankedWrestlerDTO toRankedWrestlerDTO(@NonNull Wrestler wrestler, int rank) {
    return RankedWrestlerDTO.builder()
        .id(wrestler.getId())
        .name(wrestler.getName())
        .fans(wrestler.getFans())
        .rank(rank)
        .tier(wrestler.getTier())
        .build();
  }

  private RankedTeamDTO toRankedTeamDTO(@NonNull Team team, int rank) {
    return RankedTeamDTO.builder()
        .id(team.getId())
        .name(team.getName())
        .fans(team.getWrestler1().getFans() + team.getWrestler2().getFans())
        .rank(rank)
        .build();
  }

  private RankedTeamDTO toRankedTeamDTO(@NonNull Faction faction, int rank) {
    return RankedTeamDTO.builder()
        .id(faction.getId())
        .name(faction.getName())
        .fans(faction.getMembers().stream().mapToLong(Wrestler::getFans).sum())
        .rank(rank)
        .build();
  }

  private String toImageName(@NonNull String name) {
    return name.toLowerCase().replaceAll("\\s+", "-") + ".png";
  }
}

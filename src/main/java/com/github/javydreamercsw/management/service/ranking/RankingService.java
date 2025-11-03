package com.github.javydreamercsw.management.service.ranking;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RankingService {

  private final TitleRepository titleRepository;
  private final WrestlerRepository wrestlerRepository;

  @Transactional(readOnly = true)
  public List<ChampionshipDTO> getChampionships() {
    return titleRepository.findAll().stream()
        .map(this::toChampionshipDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<RankedWrestlerDTO> getRankedContenders(@NonNull Long championshipId) {
    Optional<Title> titleOpt = titleRepository.findById(championshipId);
    if (titleOpt.isEmpty()) {
      return List.of();
    }
    Title title = titleOpt.get();
    WrestlerTier tier = title.getTier();
    long minFans = tier.getMinFans();
    long maxFans = tier.getNextTier() != null ? tier.getNextTier().getMinFans() : Long.MAX_VALUE;

    List<Wrestler> contenders =
        new ArrayList<>(wrestlerRepository.findByFansBetween(minFans, maxFans));

    title.getCurrentReign().ifPresent(reign -> contenders.removeAll(reign.getChampions()));

    AtomicInteger rank = new AtomicInteger(1);
    return contenders.stream()
        .sorted(Comparator.comparing(Wrestler::getFans).reversed())
        .map(w -> toRankedWrestlerDTO(w, rank.getAndIncrement()))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
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
                                .reignDays(reign.getReignLengthDays())
                                .build())
                    .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  private ChampionshipDTO toChampionshipDTO(@NonNull Title title) {
    return ChampionshipDTO.builder()
        .id(title.getId())
        .name(title.getName())
        .imageName(toImageName(title.getName()))
        .build();
  }

  private RankedWrestlerDTO toRankedWrestlerDTO(@NonNull Wrestler wrestler, int rank) {
    return RankedWrestlerDTO.builder()
        .id(wrestler.getId())
        .name(wrestler.getName())
        .fans(wrestler.getFans())
        .rank(rank)
        .build();
  }

  private String toImageName(@NonNull String name) {
    return name.toLowerCase().replaceAll("\\s+", "-") + ".png";
  }
}

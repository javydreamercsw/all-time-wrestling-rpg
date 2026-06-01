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
package com.github.javydreamercsw.management.service.wrestler;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class WrestlerStatsService {

  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final SegmentService segmentService;
  private final TitleService titleService;

  public WrestlerStatsService(
      final WrestlerRepository wrestlerRepository,
      @Lazy final WrestlerService wrestlerService,
      @Lazy final SegmentService segmentService,
      @Lazy final TitleService titleService) {
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerService = wrestlerService;
    this.segmentService = segmentService;
    this.titleService = titleService;
  }

  public List<WrestlerDTO> findAllAsDTO(@NonNull final Long universeId) {
    return wrestlerService.findAll().stream().map(w -> toDTO(w, universeId)).toList();
  }

  public Optional<WrestlerDTO> findByIdAsDTO(
      @NonNull final Long id, @NonNull final Long universeId) {
    return wrestlerRepository.findById(id).map(w -> toDTO(w, universeId));
  }

  public List<WrestlerDTO> findAllBySegment(
      @NonNull final Segment segment, @NonNull final Long universeId) {
    return wrestlerRepository.findAllBySegment(segment).stream()
        .map(w -> toDTO(w, universeId))
        .toList();
  }

  @Cacheable(value = CacheConfig.WRESTLER_STATS_CACHE, key = "#wrestlerId + ':' + #universeId")
  public Optional<WrestlerStats> getWrestlerStats(
      @NonNull final Long wrestlerId, @NonNull final Long universeId) {
    Wrestler wrestler =
        wrestlerRepository
            .findById(wrestlerId)
            .orElseThrow(() -> new IllegalArgumentException("Wrestler not found: " + wrestlerId));

    WrestlerStats stats = new WrestlerStats();
    stats.setWins(segmentService.countWinsByWrestler(wrestler, universeId));
    stats.setLosses(segmentService.countLossesByWrestler(wrestler, universeId));
    stats.setTitlesHeld((long) titleService.findTitlesByChampion(wrestler, universeId).size());

    return Optional.of(stats);
  }

  private WrestlerDTO toDTO(final Wrestler wrestler, final Long universeId) {
    WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), universeId);
    return new WrestlerDTO(state);
  }
}

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

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateHistory;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateHistoryRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.event.dto.ShowFinalizedEvent;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records point-in-time snapshots of wrestler fans and tier for career history charts. */
@Service
@RequiredArgsConstructor
@Slf4j
public class WrestlerStateHistoryService {

  private final WrestlerStateHistoryRepository historyRepository;
  private final WrestlerStateRepository stateRepository;

  @Transactional(readOnly = true)
  public List<WrestlerStateHistory> getHistory(final Long wrestlerId, final Long universeId) {
    return historyRepository.findByWrestlerIdAndUniverseIdOrderByRecordedAtAsc(
        wrestlerId, universeId);
  }

  @Transactional
  public void recordSnapshot(final WrestlerState state) {
    WrestlerStateHistory snapshot =
        WrestlerStateHistory.builder()
            .wrestler(state.getWrestler())
            .universe(state.getUniverse())
            .recordedAt(Instant.now())
            .fans(state.getFans())
            .tier(state.getTier())
            .build();
    historyRepository.save(snapshot);
  }

  @EventListener
  @Transactional
  public void onShowFinalized(final ShowFinalizedEvent event) {
    Long universeId =
        event.getShow().getUniverse() != null ? event.getShow().getUniverse().getId() : null;
    if (universeId == null) {
      return;
    }

    event.getSegments().stream()
        .flatMap(s -> s.getWrestlers().stream())
        .map(Wrestler::getId)
        .filter(id -> id != null)
        .distinct()
        .forEach(
            wrestlerId ->
                stateRepository
                    .findByWrestlerIdAndUniverseId(wrestlerId, universeId)
                    .ifPresent(
                        state -> {
                          log.debug(
                              "Recording career snapshot: wrestler={}, fans={}, tier={}",
                              wrestlerId,
                              state.getFans(),
                              state.getTier());
                          recordSnapshot(state);
                        }));
  }
}

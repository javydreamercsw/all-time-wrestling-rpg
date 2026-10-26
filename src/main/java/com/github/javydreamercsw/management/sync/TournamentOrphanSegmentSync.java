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
package com.github.javydreamercsw.management.sync;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Second line of defense for the standalone tournament runner's REQUIRES_NEW gap (ATW-x5d1): when
 * the runner's in-transaction compensation fails, a committed segment is left on a tournament host
 * show referenced by no bracket match. This contributor flags those rows at every startup — it
 * never deletes, because an orphan candidate can also be a legitimately hand-booked segment the
 * booker hasn't linked to the bracket yet; a human decides from the log.
 */
@Slf4j
@Component
@Order(260)
@RequiredArgsConstructor
public class TournamentOrphanSegmentSync implements DataSyncContributor {

  private final TournamentMatchRepository tournamentMatchRepository;

  @Override
  public void sync() {
    List<Segment> orphans = tournamentMatchRepository.findOrphanCandidatesOnTournamentShows();
    if (orphans.isEmpty()) {
      log.debug("No orphaned tournament segments found.");
      return;
    }
    log.warn(
        "Found {} segment(s) on tournament host shows referenced by no bracket match — likely the"
            + " runner's compensation delete failed after a mid-transaction error. Review and"
            + " delete from the show view if they are stale:",
        orphans.size());
    for (Segment segment : orphans) {
      log.warn(
          "  Orphan candidate: segment {} on show '{}' (status {}, {} participant(s))",
          segment.getId(),
          segment.getShow() != null ? segment.getShow().getName() : "<no show>",
          segment.getStatus(),
          segment.getParticipants().size());
    }
  }
}

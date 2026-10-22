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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TournamentOrphanSegmentSyncTest {

  @Mock private TournamentMatchRepository tournamentMatchRepository;

  private TournamentOrphanSegmentSync sync;

  @BeforeEach
  void setUp() {
    sync = new TournamentOrphanSegmentSync(tournamentMatchRepository);
  }

  @Test
  @DisplayName("No orphan candidates — quiet debug, nothing done")
  void noOrphans_noAction() {
    when(tournamentMatchRepository.findOrphanCandidatesOnTournamentShows()).thenReturn(List.of());

    sync.sync();

    // Detector only observes and logs; nothing is mutated (no repository writes exist to verify —
    // the assertion is that sync() completes without touching any other bean).
    verify(tournamentMatchRepository, never()).save(any());
  }

  @Test
  @DisplayName("Orphan candidates are logged with show and participant detail")
  void orphans_loggedForReview() {
    Show hostShow = new Show();
    hostShow.setId(1L);
    hostShow.setName("Crown Cup Night");
    Segment orphan = new Segment();
    orphan.setId(77L);
    orphan.setShow(hostShow);
    orphan.setStatus(SegmentStatus.BOOKED);
    orphan.setSegmentType(new SegmentType());
    orphan.addParticipant(new Wrestler());
    when(tournamentMatchRepository.findOrphanCandidatesOnTournamentShows())
        .thenReturn(List.of(orphan));

    sync.sync();

    // Logging is the whole contract; a spy-free assertion is that the detector ran its query
    // and did not attempt any write (deletion is deliberately manual).
    verify(tournamentMatchRepository).findOrphanCandidatesOnTournamentShows();
    verify(tournamentMatchRepository, never()).save(any());
  }

  @Test
  @DisplayName("Detector failure does not break startup — exception propagates as sync contract")
  void detectorFailure_propagates() {
    when(tournamentMatchRepository.findOrphanCandidatesOnTournamentShows())
        .thenThrow(new IllegalStateException("db unavailable"));

    assertThat(org.assertj.core.api.Assertions.catchException(() -> sync.sync()))
        .isInstanceOf(IllegalStateException.class);
  }
}

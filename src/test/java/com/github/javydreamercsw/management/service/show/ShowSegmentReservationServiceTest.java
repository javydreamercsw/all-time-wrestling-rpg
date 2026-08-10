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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservation;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationPurpose;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationRepository;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationStatus;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShowSegmentReservationServiceTest {

  @Mock private ShowSegmentReservationRepository repo;

  private ShowSegmentReservationService service;
  private Show show;

  @BeforeEach
  void setUp() {
    service = new ShowSegmentReservationService(repo);
    show = new Show();
    show.setId(1L);
  }

  @Test
  void reserveSlot_savesAndReturnsPendingReservation() {
    ShowSegmentReservation saved = new ShowSegmentReservation();
    when(repo.save(any())).thenReturn(saved);

    ShowSegmentReservation result =
        service.reserveSlot(
            show, ShowSegmentReservationPurpose.TOURNAMENT_ROUND, 42L, "R1: A vs B");

    verify(repo).save(any(ShowSegmentReservation.class));
    assertThat(result).isSameAs(saved);
  }

  @Test
  void fillReservation_setsFilledStatusAndSegment() {
    ShowSegmentReservation reservation = new ShowSegmentReservation();
    Segment segment = new Segment();
    when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ShowSegmentReservation result = service.fillReservation(reservation, segment);

    assertThat(result.getStatus()).isEqualTo(ShowSegmentReservationStatus.FILLED);
    assertThat(result.getSegment()).isSameAs(segment);
  }

  @Test
  void cancelReservation_setsCancelledStatus() {
    ShowSegmentReservation reservation = new ShowSegmentReservation();
    when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.cancelReservation(reservation);

    verify(repo).save(reservation);
    assertThat(reservation.getStatus()).isEqualTo(ShowSegmentReservationStatus.CANCELLED);
  }

  @Test
  void countPendingReservations_returnsZeroWhenShowHasNoId() {
    Show unsaved = new Show();
    assertThat(service.countPendingReservations(unsaved)).isZero();
  }

  @Test
  void countPendingReservations_delegatesToRepo() {
    when(repo.countByShowIdAndStatus(1L, ShowSegmentReservationStatus.PENDING)).thenReturn(3);
    assertThat(service.countPendingReservations(show)).isEqualTo(3);
  }

  @Test
  void findAllForShow_returnsEmptyWhenShowHasNoId() {
    assertThat(service.findAllForShow(new Show())).isEmpty();
  }

  @Test
  void findAllForShow_delegatesToRepo() {
    List<ShowSegmentReservation> list = List.of(new ShowSegmentReservation());
    when(repo.findByShowId(1L)).thenReturn(list);
    assertThat(service.findAllForShow(show)).isSameAs(list);
  }

  @Test
  void findPendingForShow_returnsEmptyWhenShowHasNoId() {
    assertThat(service.findPendingForShow(new Show())).isEmpty();
  }

  @Test
  void findPendingForShow_delegatesToRepo() {
    List<ShowSegmentReservation> list = List.of(new ShowSegmentReservation());
    when(repo.findByShowIdAndStatus(1L, ShowSegmentReservationStatus.PENDING)).thenReturn(list);
    assertThat(service.findPendingForShow(show)).isSameAs(list);
  }

  @Test
  void findBySourceAndPurpose_delegatesToRepo() {
    List<ShowSegmentReservation> list = List.of(new ShowSegmentReservation());
    when(repo.findBySourceIdAndPurpose(7L, ShowSegmentReservationPurpose.TOURNAMENT_ROUND))
        .thenReturn(list);
    assertThat(service.findBySourceAndPurpose(7L, ShowSegmentReservationPurpose.TOURNAMENT_ROUND))
        .isSameAs(list);
  }
}

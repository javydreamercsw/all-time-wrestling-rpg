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

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservation;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationPurpose;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationRepository;
import com.github.javydreamercsw.management.domain.show.reservation.ShowSegmentReservationStatus;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShowSegmentReservationService {

  private final ShowSegmentReservationRepository reservationRepository;

  /** Reserve a slot on a show for a specific purpose, excluding it from auto-booking. */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public ShowSegmentReservation reserveSlot(
      Show show, ShowSegmentReservationPurpose purpose, @Nullable Long sourceId, String label) {
    ShowSegmentReservation reservation =
        ShowSegmentReservation.builder()
            .show(show)
            .purpose(purpose)
            .sourceId(sourceId)
            .label(label)
            .status(ShowSegmentReservationStatus.PENDING)
            .build();
    return reservationRepository.save(reservation);
  }

  /** Mark a reservation as filled by linking it to the actual segment that was created. */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public ShowSegmentReservation fillReservation(
      ShowSegmentReservation reservation, Segment segment) {
    reservation.setSegment(segment);
    reservation.setStatus(ShowSegmentReservationStatus.FILLED);
    return reservationRepository.save(reservation);
  }

  /** Cancel a pending reservation, freeing the slot for auto-booking. */
  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')"
          + " or @universeAuthz.hasRoleInCurrentUniverse('BOOKER')")
  public void cancelReservation(ShowSegmentReservation reservation) {
    reservation.setStatus(ShowSegmentReservationStatus.CANCELLED);
    reservationRepository.save(reservation);
  }

  @Transactional(readOnly = true)
  public int countPendingReservations(Show show) {
    if (show.getId() == null) {
      return 0;
    }
    return reservationRepository.countByShowIdAndStatus(
        show.getId(), ShowSegmentReservationStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public List<ShowSegmentReservation> findAllForShow(Show show) {
    if (show.getId() == null) {
      return List.of();
    }
    return reservationRepository.findByShowId(show.getId());
  }

  @Transactional(readOnly = true)
  public List<ShowSegmentReservation> findPendingForShow(Show show) {
    if (show.getId() == null) {
      return List.of();
    }
    return reservationRepository.findByShowIdAndStatus(
        show.getId(), ShowSegmentReservationStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public List<ShowSegmentReservation> findBySourceAndPurpose(
      Long sourceId, ShowSegmentReservationPurpose purpose) {
    return reservationRepository.findBySourceIdAndPurpose(sourceId, purpose);
  }
}

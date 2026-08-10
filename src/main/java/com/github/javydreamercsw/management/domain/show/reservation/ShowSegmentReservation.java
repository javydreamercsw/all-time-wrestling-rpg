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
package com.github.javydreamercsw.management.domain.show.reservation;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/** A pre-committed segment slot on a show, excluded from auto-booking. */
@Entity
@Table(name = "show_segment_reservation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSegmentReservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "show_id", nullable = false)
  private Show show;

  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false, length = 32)
  private ShowSegmentReservationPurpose purpose;

  /** Back-reference to the originating domain object (tournament id, rivalry id, etc.). */
  @Column(name = "source_id")
  @Nullable private Long sourceId;

  /** Human-readable label shown in the show planner, e.g. "Round 1 – Summer Classic". */
  @Column(name = "label", nullable = false)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  @Builder.Default
  private ShowSegmentReservationStatus status = ShowSegmentReservationStatus.PENDING;

  /** Filled once the actual segment is booked into this slot. */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "segment_id")
  @Nullable private Segment segment;
}

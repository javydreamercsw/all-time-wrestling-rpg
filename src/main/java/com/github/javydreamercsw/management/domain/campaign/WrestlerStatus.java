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
package com.github.javydreamercsw.management.domain.campaign;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Maps a wrestler to a status card and tracks their current level (I or II). */
@Entity
@Table(name = "wrestler_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrestlerStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "wrestler_id", nullable = false)
  private Wrestler wrestler;

  @ManyToOne(optional = false)
  @JoinColumn(name = "status_card_id", nullable = false)
  private StatusCard statusCard;

  @Column(nullable = false)
  @Builder.Default
  private int level = 1;

  @CreationTimestamp
  @Column(name = "creation_date", nullable = false, updatable = false)
  private LocalDateTime creationDate;

  @UpdateTimestamp
  @Column(name = "last_updated", nullable = false)
  private LocalDateTime lastUpdated;
}

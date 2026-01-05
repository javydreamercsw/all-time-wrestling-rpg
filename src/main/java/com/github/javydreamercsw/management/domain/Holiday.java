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
package com.github.javydreamercsw.management.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.Month;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "holiday")
@Data
public class Holiday {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @NotNull @Column(name = "description", nullable = false, unique = true)
  private String description;

  @NotNull @Column(name = "theme", nullable = false)
  private String theme;

  @Column(name = "decorations")
  private String decorations;

  @Column(name = "day_of_month")
  private Integer dayOfMonth;

  @Enumerated(EnumType.STRING)
  @Column(name = "holiday_month")
  private Month holidayMonth;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week")
  private DayOfWeek dayOfWeek;

  @Column(name = "week_of_month")
  private Integer weekOfMonth;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private HolidayType type;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }
}

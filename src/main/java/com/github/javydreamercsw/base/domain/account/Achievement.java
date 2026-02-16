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
package com.github.javydreamercsw.base.domain.account;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(
    name = "achievement",
    uniqueConstraints = @UniqueConstraint(columnNames = {"achievement_key"}))
@Getter
@Setter
@NoArgsConstructor
public class Achievement extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "achievement_id")
  private Long id;

  @Column(name = "achievement_key", nullable = false, unique = true)
  @NotNull private String key;

  @Column(nullable = false)
  @NotNull private String name;

  @Column(nullable = false, length = 1000)
  @NotNull private String description;

  @Column(name = "xp_value", nullable = false)
  @NotNull private Integer xpValue;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull private AchievementCategory category;

  @Column(name = "icon_url")
  @Size(max = 512) private String iconUrl;

  @Override
  public @Nullable Long getId() {
    return id;
  }
}

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
import lombok.NonNull;
import lombok.Setter;

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

  @Column(name = "expansion_code", nullable = false)
  private String expansionCode = "BASE_GAME";

  @Column(name = "icon_url")
  @Size(max = 512) private String iconUrl;

  /**
   * Optional Groovy boolean expression evaluated against a context map at the relevant unlock check
   * site. Null means no scripted condition — the achievement relies solely on hardcoded Java checks
   * (if any). See AchievementScriptService for evaluation semantics.
   */
  @Column(name = "unlock_condition", length = 2000)
  @Size(max = 2000) private String unlockCondition;

  /**
   * Copies the content-authored fields from {@code source} onto this achievement — used when
   * upserting an existing achievement from achievements.json or a remote content pack. Identity
   * fields ({@code id}, {@code key}) are left untouched. Single source of truth for which fields a
   * content update is allowed to change, so the two independent upsert call sites (AchievementSync,
   * ChallengeUpdateService) can't drift out of sync with each other.
   */
  public void copyContentFrom(@NonNull final Achievement source) {
    this.name = source.name;
    this.description = source.description;
    this.xpValue = source.xpValue;
    this.category = source.category;
    this.unlockCondition = source.unlockCondition;
  }
}

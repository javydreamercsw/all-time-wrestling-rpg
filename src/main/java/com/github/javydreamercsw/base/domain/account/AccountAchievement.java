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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Read-only view of the account_achievement join table. Inserts are managed by the @ManyToMany on
 * Account.achievements; this entity exists solely to expose the unlocked_at timestamp.
 */
@Entity
@Table(name = "account_achievement")
@IdClass(AccountAchievementId.class)
@Immutable
@Getter
public class AccountAchievement {

  @Id
  @Column(name = "account_id", insertable = false, updatable = false)
  private Long accountId;

  @Id
  @Column(name = "achievement_id", insertable = false, updatable = false)
  private Long achievementId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "achievement_id", insertable = false, updatable = false)
  private Achievement achievement;

  @Column(name = "unlocked_at", insertable = false, updatable = false)
  private Instant unlockedAt;
}

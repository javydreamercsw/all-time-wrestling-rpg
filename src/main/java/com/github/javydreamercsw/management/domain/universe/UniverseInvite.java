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
package com.github.javydreamercsw.management.domain.universe;

import com.github.javydreamercsw.base.domain.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "universe_invite")
@Getter
@Setter
public class UniverseInvite {

  public enum InviteType {
    /** Single-use; expires after {@code max_uses} (default 1) or after {@code expires_at}. */
    TARGETED,
    /** Multi-use; no expiry by default; admin revokes manually. */
    COMMUNITY
  }

  @Id
  @Column(name = "id", length = 36, nullable = false)
  private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "universe_id", nullable = false)
  private Universe universe;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private InviteType type;

  @ManyToOne(optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private Account createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  /** NULL means no expiry. */
  @Column(name = "expires_at")
  private Instant expiresAt;

  /** Set when an admin manually revokes the link. */
  @Column(name = "revoked_at")
  private Instant revokedAt;

  /** NULL means unlimited uses. TARGETED defaults to 1. */
  @Column(name = "max_uses")
  private Integer maxUses;

  @Column(name = "use_count", nullable = false)
  private int useCount = 0;

  public boolean isActive() {
    if (revokedAt != null) {
      return false;
    }
    if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
      return false;
    }
    return maxUses == null || useCount < maxUses;
  }
}

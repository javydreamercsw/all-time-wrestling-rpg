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
package com.github.javydreamercsw.base.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@MappedSuperclass
@Getter
@Setter
public abstract class AbstractEntity<ID> implements NamedEntity {

  /** Maximum length for description fields across all entities */
  public static final int DESCRIPTION_MAX_LENGTH = 255;

  @Column(name = "external_id", unique = true)
  @Size(max = 255) private String externalId;

  @Column(name = "last_sync")
  private Instant lastSync;

  @Column(name = "updated_at")
  private Instant updatedAt = Instant.now();

  public abstract @Nullable ID getId();

  /**
   * Checks if the entity has changed since the last synchronization.
   *
   * @return true if there are unsynced changes, false otherwise
   */
  public boolean hasUnsyncedChanges() {
    if (lastSync == null || updatedAt == null) {
      return true; // Never synced or missing update record - sync to be safe
    }
    // Any update at or after the last sync time should be synced.
    // We use !isBefore to include equal timestamps.
    return !updatedAt.isBefore(lastSync);
  }

  @jakarta.persistence.PrePersist
  protected void onBaseCreate() {
    if (updatedAt == null) {
      updatedAt = Instant.now();
    }
  }

  @jakarta.persistence.PreUpdate
  protected void onBaseUpdate() {
    updatedAt = Instant.now();
  }

  @Override
  public @Nullable String getName() {
    return null;
  }

  @Override
  public String toString() {
    return "%s{id=%s}".formatted(getClass().getSimpleName(), getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractEntity<?> that)) return false;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }
}

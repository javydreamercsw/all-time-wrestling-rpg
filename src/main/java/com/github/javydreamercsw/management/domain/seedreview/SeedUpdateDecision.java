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
package com.github.javydreamercsw.management.domain.seedreview;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * A single seed-data collision parked for an administrator decision when a version upgrade boots
 * and the incoming seed data no longer matches the database (seed-update review feature).
 *
 * <p>Rows start as {@link #STATUS_PENDING} and are transitioned to exactly one of the {@code
 * APPLIED_*} statuses once the administrator chooses an outcome (mapped from {@code
 * SeedFieldDecision}: KEEP_MINE&rarr;APPLIED_KEEP, USE_NEW&rarr;APPLIED_NEW,
 * KEEP_AS_CUSTOM&rarr;APPLIED_KEEP_AS_CUSTOM, REMOVE&rarr;APPLIED_REMOVE).
 *
 * <p>There is intentionally no DB-level unique constraint on {@code (domain_key, entity_key,
 * field_name)} restricted to {@code PENDING} rows: H2 and MySQL lack partial indexes. The
 * at-most-one-open-decision-per-coordinate rule is enforced by the review service's
 * query-then-insert dedupe; see the migration comments for details.
 */
@Entity
@Table(name = "seed_update_decision")
@Getter
@Setter
public class SeedUpdateDecision extends AbstractEntity<Long> {

  /** Open collision awaiting an administrator decision. */
  public static final String STATUS_PENDING = "PENDING";

  /** Administrator kept the database's current value; the incoming seed value was discarded. */
  public static final String STATUS_APPLIED_KEEP = "APPLIED_KEEP";

  /** Administrator adopted the incoming seed value; the database value was overwritten. */
  public static final String STATUS_APPLIED_NEW = "APPLIED_NEW";

  /** Administrator kept the database value and marked it custom (no longer seed-owned). */
  public static final String STATUS_APPLIED_KEEP_AS_CUSTOM = "APPLIED_KEEP_AS_CUSTOM";

  /** Administrator removed the database row; the seed no longer carries it. */
  public static final String STATUS_APPLIED_REMOVE = "APPLIED_REMOVE";

  /** The seed value differs from the database value for a tracked field. */
  public static final String DECISION_TYPE_FIELD_CONFLICT = "FIELD_CONFLICT";

  /** The seed no longer carries the entity; the database row is up for removal. */
  public static final String DECISION_TYPE_REMOVAL = "REMOVAL";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter(onMethod_ = {@Nullable})
  @Column(name = "id")
  private Long id;

  /** Seed domain the colliding entity belongs to (e.g. "wrestler", "npc", "title"). */
  @Column(name = "domain_key", nullable = false, length = 64)
  private String domainKey;

  /** Stable business key of the colliding entity within its domain (e.g. "wrestler:bret-hart"). */
  @Column(name = "entity_key", nullable = false, length = 255)
  private String entityKey;

  /** Human-readable name for the review UI (e.g. "Bret Hart"). */
  @Column(name = "entity_display_name", nullable = false, length = 255)
  private String entityDisplayName;

  /** Field that collided; {@code NULL} for {@link #DECISION_TYPE_REMOVAL} rows. */
  @Column(name = "field_name", length = 128)
  private @Nullable String fieldName;

  /** {@link #DECISION_TYPE_FIELD_CONFLICT} or {@link #DECISION_TYPE_REMOVAL}. */
  @Column(name = "decision_type", nullable = false, length = 32)
  private String decisionType;

  /** Database's current value at detection time; {@code NULL} for removal decisions. */
  @Column(name = "current_value", columnDefinition = "TEXT")
  private @Nullable String currentValue;

  /** Incoming seed value at detection time; {@code NULL} for removal decisions. */
  @Column(name = "new_value", columnDefinition = "TEXT")
  private @Nullable String newValue;

  /** Fingerprint of the incoming seed data, for later re-verification before applying. */
  @Column(name = "new_fingerprint", nullable = false, length = 64)
  private String newFingerprint;

  /** {@link #STATUS_PENDING} until applied, then exactly one {@code APPLIED_*} status. */
  @Column(name = "status", nullable = false, length = 32)
  private String status;

  /** Application version whose boot detected the collision. */
  @Column(name = "app_version", nullable = false, length = 32)
  private String appVersion;

  /** When the collision was detected and parked. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}

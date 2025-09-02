package com.github.javydreamercsw.management.domain.show.match.stipulation;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a match rule in the ATW RPG system. Match rules define special conditions,
 * stipulations, or modifications that can be applied to wrestling matches.
 *
 * <p>Examples: No Disqualification, Steel Cage, Ladder Match, Hell in a Cell, etc. A match can have
 * multiple rules applied to it.
 */
@Entity
@Table(name = "match_rule", uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
@Getter
@Setter
public class MatchRule extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "match_rule_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "requires_high_heat", nullable = false)
  private Boolean requiresHighHeat = false;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  /** Ensure default values before persisting. */
  @PrePersist
  private void ensureDefaults() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (isActive == null) {
      isActive = true;
    }
    if (requiresHighHeat == null) {
      requiresHighHeat = false;
    }
  }

  /** Check if this rule is suitable for high-heat rivalries. */
  public boolean isSuitableForHighHeat() {
    return requiresHighHeat != null && requiresHighHeat;
  }

  @Override
  public String toString() {
    return name;
  }
}

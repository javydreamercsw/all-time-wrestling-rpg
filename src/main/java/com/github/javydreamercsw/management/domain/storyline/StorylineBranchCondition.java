package com.github.javydreamercsw.management.domain.storyline;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a condition that must be met for a storyline branch to activate. Conditions can check
 * segment outcomes, wrestler states, heat levels, etc.
 */
@Entity
@Table(name = "storyline_branch_condition")
@Getter
@Setter
public class StorylineBranchCondition extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "storyline_branch_condition_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "storyline_branch_id", nullable = false)
  @JsonIgnoreProperties({"conditions", "effects"})
  private StorylineBranch storylineBranch;

  @Column(name = "condition_type", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String conditionType;

  @Column(name = "condition_key", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String conditionKey;

  @Lob
  @Column(name = "condition_value")
  private String conditionValue;

  @Column(name = "is_condition_met", nullable = false)
  private Boolean isConditionMet = false;

  @Column(name = "condition_description")
  @Size(max = DESCRIPTION_MAX_LENGTH) private String conditionDescription;

  @Column(name = "last_checked_date")
  private Instant lastCheckedDate;

  @Column(name = "met_date")
  private Instant metDate;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
  }

  // ==================== ATW RPG METHODS ====================

  /** Mark this condition as met. */
  public void markAsMet() {
    if (!isConditionMet) {
      this.isConditionMet = true;
      this.metDate = Instant.now();
    }
  }

  /** Mark this condition as not met. */
  public void markAsNotMet() {
    if (isConditionMet) {
      this.isConditionMet = false;
      this.metDate = null;
    }
  }

  /** Update the last checked timestamp. */
  public void updateLastChecked() {
    this.lastCheckedDate = Instant.now();
  }

  /** Check if this condition has been checked recently (within hours). */
  public boolean isRecentlyChecked(int hours) {
    if (lastCheckedDate == null) {
      return false;
    }
    Instant threshold = Instant.now().minusSeconds(hours * 3600L);
    return lastCheckedDate.isAfter(threshold);
  }

  /** Get display string for this condition. */
  public String getDisplayString() {
    String status = isConditionMet ? "✅ MET" : "❌ NOT MET";
    return String.format("%s: %s [%s]", conditionType, conditionDescription, status);
  }

  /** Get condition summary. */
  public String getConditionSummary() {
    StringBuilder summary = new StringBuilder();
    summary.append(conditionType).append(" - ");

    if (conditionDescription != null && !conditionDescription.isEmpty()) {
      summary.append(conditionDescription);
    } else {
      summary.append(conditionKey).append(" = ").append(conditionValue);
    }

    if (isConditionMet && metDate != null) {
      summary.append(" (Met on ").append(metDate).append(")");
    }

    return summary.toString();
  }

  /** Check if this is a segment outcome condition. */
  public boolean isMatchOutcomeCondition() {
    return "WRESTLER_WINS".equals(conditionType)
        || "WRESTLER_LOSES".equals(conditionType)
        || "MATCH_TYPE".equals(conditionType)
        || "STIPULATION".equals(conditionType);
  }

  /** Check if this is a rivalry condition. */
  public boolean isRivalryCondition() {
    return "HEAT_THRESHOLD".equals(conditionType)
        || "RIVALRY_ACTIVE".equals(conditionType)
        || "WRESTLERS_INVOLVED".equals(conditionType);
  }

  /** Check if this is a faction condition. */
  public boolean isFactionCondition() {
    return "FACTION_MEMBER".equals(conditionType)
        || "FACTION_ACTIVE".equals(conditionType)
        || "MEMBER_COUNT".equals(conditionType);
  }

  /** Check if this is a time-based condition. */
  public boolean isTimeBasedCondition() {
    return "DATE_REACHED".equals(conditionType)
        || "DAYS_PASSED".equals(conditionType)
        || "SHOW_COUNT".equals(conditionType);
  }

  /** Get the priority for checking this condition (higher = more urgent). */
  public int getCheckPriority() {
    return switch (conditionType) {
      case "WRESTLER_WINS", "WRESTLER_LOSES" -> 10; // Highest - segment outcomes
      case "HEAT_THRESHOLD", "RIVALRY_ACTIVE" -> 8; // High - rivalry conditions
      case "FACTION_MEMBER", "FACTION_ACTIVE" -> 6; // Medium - faction conditions
      case "DATE_REACHED", "DAYS_PASSED" -> 4; // Low - time conditions
      default -> 2; // Lowest - other conditions
    };
  }

  /** Check if this condition needs frequent checking. */
  public boolean needsFrequentChecking() {
    return isMatchOutcomeCondition() || isRivalryCondition();
  }

  /** Get recommended check interval in hours. */
  public int getRecommendedCheckIntervalHours() {
    if (isMatchOutcomeCondition()) {
      return 1; // Check every hour for segment outcomes
    } else if (isRivalryCondition() || isFactionCondition()) {
      return 6; // Check every 6 hours for rivalry/faction changes
    } else if (isTimeBasedCondition()) {
      return 24; // Check daily for time-based conditions
    } else {
      return 12; // Default to every 12 hours
    }
  }
}

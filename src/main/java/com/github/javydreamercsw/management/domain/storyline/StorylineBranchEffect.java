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
 * Represents an effect that occurs when a storyline branch is activated. Effects can create
 * rivalries, add heat, change alignments, etc.
 */
@Entity
@Table(name = "storyline_branch_effect")
@Getter
@Setter
public class StorylineBranchEffect extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "storyline_branch_effect_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "storyline_branch_id", nullable = false)
  @JsonIgnoreProperties({"conditions", "effects"})
  private StorylineBranch storylineBranch;

  @Column(name = "effect_type", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String effectType;

  @Column(name = "effect_key", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String effectKey;

  @Lob
  @Column(name = "effect_value")
  private String effectValue;

  @Column(name = "is_executed", nullable = false)
  private Boolean isExecuted = false;

  @Column(name = "execution_order", nullable = false)
  private Integer executionOrder = 1;

  @Column(name = "effect_description")
  @Size(max = DESCRIPTION_MAX_LENGTH) private String effectDescription;

  @Column(name = "executed_date")
  private Instant executedDate;

  @Lob
  @Column(name = "execution_result")
  private String executionResult;

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

  /** Execute this effect. */
  public void execute() {
    if (isExecuted) {
      return; // Already executed
    }

    try {
      // The actual execution logic would be handled by a service
      // This method just marks the effect as executed
      this.isExecuted = true;
      this.executedDate = Instant.now();
      this.executionResult = "Effect executed successfully";
    } catch (Exception e) {
      this.executionResult = "Effect execution failed: " + e.getMessage();
    }
  }

  /** Mark this effect as executed with a custom result. */
  public void markAsExecuted(String result) {
    this.isExecuted = true;
    this.executedDate = Instant.now();
    this.executionResult = result;
  }

  /** Reset this effect to unexecuted state. */
  public void resetExecution() {
    this.isExecuted = false;
    this.executedDate = null;
    this.executionResult = null;
  }

  /** Get display string for this effect. */
  public String getDisplayString() {
    String status = isExecuted ? "✅ EXECUTED" : "⏳ PENDING";
    return String.format("%s: %s [%s]", effectType, effectDescription, status);
  }

  /** Get effect summary. */
  public String getEffectSummary() {
    StringBuilder summary = new StringBuilder();
    summary.append(effectType).append(" - ");

    if (effectDescription != null && !effectDescription.isEmpty()) {
      summary.append(effectDescription);
    } else {
      summary.append(effectKey).append(" = ").append(effectValue);
    }

    if (isExecuted && executedDate != null) {
      summary.append(" (Executed on ").append(executedDate).append(")");
    }

    return summary.toString();
  }

  /** Check if this is a rivalry effect. */
  public boolean isRivalryEffect() {
    return "CREATE_RIVALRY".equals(effectType)
        || "ADD_HEAT".equals(effectType)
        || "ESCALATE_HEAT".equals(effectType)
        || "END_RIVALRY".equals(effectType);
  }

  /** Check if this is a faction effect. */
  public boolean isFactionEffect() {
    return "CREATE_FACTION".equals(effectType)
        || "ADD_MEMBER".equals(effectType)
        || "REMOVE_MEMBER".equals(effectType)
        || "DISBAND_FACTION".equals(effectType);
  }

  /** Check if this is a match effect. */
  public boolean isMatchEffect() {
    return "FORCE_MATCH".equals(effectType)
        || "ADD_STIPULATION".equals(effectType)
        || "TITLE_SHOT".equals(effectType)
        || "SPECIAL_MATCH".equals(effectType);
  }

  /** Check if this is a wrestler effect. */
  public boolean isWrestlerEffect() {
    return "AWARD_FANS".equals(effectType)
        || "CHANGE_ALIGNMENT".equals(effectType)
        || "TIER_PROMOTION".equals(effectType)
        || "INJURY_WRESTLER".equals(effectType);
  }

  /** Check if this is a storyline effect. */
  public boolean isStorylineEffect() {
    return "CREATE_STORYLINE".equals(effectType)
        || "END_STORYLINE".equals(effectType)
        || "STORYLINE_TWIST".equals(effectType)
        || "NARRATIVE_CHANGE".equals(effectType);
  }

  /** Get the priority for executing this effect (higher = more urgent). */
  public int getExecutionPriority() {
    return switch (effectType) {
      case "CREATE_RIVALRY", "ADD_HEAT" -> 10; // Highest - rivalry effects
      case "FORCE_MATCH", "TITLE_SHOT" -> 9; // Very high - match effects
      case "CREATE_FACTION", "ADD_MEMBER" -> 8; // High - faction effects
      case "AWARD_FANS", "CHANGE_ALIGNMENT" -> 6; // Medium - wrestler effects
      case "CREATE_STORYLINE", "STORYLINE_TWIST" -> 4; // Low - storyline effects
      default -> 2; // Lowest - other effects
    };
  }

  /** Check if this effect should be executed immediately. */
  public boolean shouldExecuteImmediately() {
    return isRivalryEffect() || isMatchEffect();
  }

  /** Check if this effect can be delayed. */
  public boolean canBeDelayed() {
    return isStorylineEffect() || effectType.startsWith("CUSTOM_");
  }

  /** Get recommended execution delay in hours. */
  public int getRecommendedDelayHours() {
    if (shouldExecuteImmediately()) {
      return 0; // Execute immediately
    } else if (isFactionEffect() || isWrestlerEffect()) {
      return 1; // Execute within an hour
    } else if (isStorylineEffect()) {
      return 24; // Execute within a day
    } else {
      return 6; // Default to 6 hours
    }
  }

  /** Check if this effect has dependencies on other effects. */
  public boolean hasDependencies() {
    // Effects with higher execution order typically depend on lower order effects
    return executionOrder > 1;
  }

  /** Check if this effect was executed successfully. */
  public boolean wasExecutedSuccessfully() {
    return isExecuted
        && executionResult != null
        && !executionResult.toLowerCase().contains("failed")
        && !executionResult.toLowerCase().contains("error");
  }
}

package com.github.javydreamercsw.management.domain.storyline;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.show.match.Match;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a storyline branch that can be triggered by match outcomes. This allows for dynamic
 * storyline development based on who wins/loses matches.
 */
@Entity
@Table(name = "storyline_branch")
@Getter
@Setter
public class StorylineBranch extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "storyline_branch_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  @Column(name = "branch_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private StorylineBranchType branchType;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "priority", nullable = false)
  private Integer priority = 1; // Higher numbers = higher priority

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Column(name = "activated_date")
  private Instant activatedDate;

  @Column(name = "completed_date")
  private Instant completedDate;

  // The match result that triggered this branch (if applicable)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "triggering_match_id")
  @JsonIgnoreProperties({"participants", "show"})
  private Match triggeringMatch;

  // Conditions that must be met for this branch to activate
  @OneToMany(mappedBy = "storylineBranch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"storylineBranch"})
  private List<StorylineBranchCondition> conditions = new ArrayList<>();

  // Effects that occur when this branch is activated
  @OneToMany(mappedBy = "storylineBranch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"storylineBranch"})
  private List<StorylineBranchEffect> effects = new ArrayList<>();

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

  /** Add a condition to this storyline branch. */
  public void addCondition(StorylineBranchCondition condition) {
    if (condition != null) {
      condition.setStorylineBranch(this);
      conditions.add(condition);
    }
  }

  /** Add an effect to this storyline branch. */
  public void addEffect(StorylineBranchEffect effect) {
    if (effect != null) {
      effect.setStorylineBranch(this);
      effects.add(effect);
    }
  }

  /** Check if all conditions are met for this branch to activate. */
  public boolean areConditionsMet() {
    return conditions.stream().allMatch(condition -> condition.getIsConditionMet());
  }

  /** Activate this storyline branch. */
  public void activate(Match triggeringMatch) {
    if (!isActive || activatedDate != null) {
      return; // Already activated or inactive
    }

    this.triggeringMatch = triggeringMatch;
    this.activatedDate = Instant.now();

    // Execute all effects
    for (StorylineBranchEffect effect : effects) {
      effect.execute();
    }
  }

  /** Complete this storyline branch. */
  public void complete(String reason) {
    this.completedDate = Instant.now();
    this.isActive = false;
  }

  /** Check if this branch has been activated. */
  public boolean isActivated() {
    return activatedDate != null;
  }

  /** Check if this branch has been completed. */
  public boolean isCompleted() {
    return completedDate != null;
  }

  /** Get the status of this storyline branch. */
  public StorylineBranchStatus getStatus() {
    if (isCompleted()) {
      return StorylineBranchStatus.COMPLETED;
    } else if (isActivated()) {
      return StorylineBranchStatus.ACTIVATED;
    } else if (areConditionsMet()) {
      return StorylineBranchStatus.READY_TO_ACTIVATE;
    } else {
      return StorylineBranchStatus.WAITING_FOR_CONDITIONS;
    }
  }

  /** Get display name with status. */
  public String getDisplayName() {
    return name + " (" + getStatus().getDisplayName() + ")";
  }

  /** Get branch summary with type and priority. */
  public String getBranchSummary() {
    return String.format(
        "%s [%s - Priority %d]", getDisplayName(), branchType.getDisplayName(), priority);
  }

  /** Get the number of conditions that are met. */
  public int getMetConditionsCount() {
    return (int) conditions.stream().filter(condition -> condition.getIsConditionMet()).count();
  }

  /** Get the total number of conditions. */
  public int getTotalConditionsCount() {
    return conditions.size();
  }

  /** Get conditions progress as a percentage. */
  public double getConditionsProgress() {
    if (conditions.isEmpty()) {
      return 100.0;
    }
    return (double) getMetConditionsCount() / getTotalConditionsCount() * 100.0;
  }

  /** Get the number of effects that have been executed. */
  public int getExecutedEffectsCount() {
    return (int) effects.stream().filter(effect -> effect.getIsExecuted()).count();
  }

  /** Get the total number of effects. */
  public int getTotalEffectsCount() {
    return effects.size();
  }

  /** Check if all effects have been executed. */
  public boolean areAllEffectsExecuted() {
    return effects.stream().allMatch(effect -> effect.getIsExecuted());
  }
}

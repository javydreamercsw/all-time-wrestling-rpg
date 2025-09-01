package com.github.javydreamercsw.management.domain.injury;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents an injury sustained by a wrestler in the ATW RPG system. Injuries affect starting
 * health and can be healed through recovery phases.
 *
 * <p>Injury System: - 3 bump tokens convert to 1 injury - Injuries reduce starting health - Can be
 * healed by staying off shows or spending fans
 */
@Entity
@Table(name = "injury")
@Getter
@Setter
public class Injury extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "injury_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "wrestler_id", nullable = false)
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns"})
  private Wrestler wrestler;

  @Column(name = "name", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String name;

  @Lob
  @Column(name = "description")
  private String description;

  @Column(name = "severity", nullable = false)
  @Enumerated(EnumType.STRING)
  private InjurySeverity severity;

  @Column(name = "health_penalty", nullable = false)
  @Min(1) private Integer healthPenalty;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "injury_date", nullable = false)
  private Instant injuryDate;

  @Column(name = "healed_date")
  private Instant healedDate;

  @Column(name = "healing_cost", nullable = false)
  @Min(0) private Long healingCost = 10000L; // Default 10k fans to heal

  @Lob
  @Column(name = "injury_notes")
  private String injuryNotes;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Column(name = "external_id", unique = true)
  @Size(max = 255) private String externalId; // External system ID (e.g., Notion page ID)

  // ==================== ATW RPG METHODS ====================

  /** Check if this injury is currently affecting the wrestler. */
  public boolean isCurrentlyActive() {
    return isActive && healedDate == null;
  }

  /** Heal this injury. */
  public void heal() {
    this.isActive = false;
    this.healedDate = Instant.now();
  }

  /** Get the number of days this injury has been active. */
  public long getDaysActive() {
    Instant end = healedDate != null ? healedDate : Instant.now();
    return java.time.Duration.between(injuryDate, end).toDays();
  }

  /** Get display string for this injury. */
  public String getDisplayString() {
    String status = isCurrentlyActive() ? " (Active)" : " (Healed)";
    return String.format(
        "%s - %s (%d health penalty)%s", name, severity.getDisplayName(), healthPenalty, status);
  }

  /** Get injury status emoji. */
  public String getStatusEmoji() {
    if (!isActive) return "✅";
    return severity.getEmoji();
  }

  /** Get the total health impact of this injury. */
  public int getHealthImpact() {
    return isCurrentlyActive() ? healthPenalty : 0;
  }

  /** Check if this injury can be healed with the recovery system. */
  public boolean canBeHealed() {
    return isCurrentlyActive();
  }

  /** Get the fan cost to attempt healing this injury. */
  public Long getHealingFanCost() {
    return healingCost;
  }

  /** Get injury duration display. */
  public String getDurationDisplay() {
    long days = getDaysActive();

    if (days == 0) {
      return "Less than 1 day";
    } else if (days == 1) {
      return "1 day";
    } else if (days < 7) {
      return days + " days";
    } else if (days < 30) {
      long weeks = days / 7;
      return weeks + (weeks == 1 ? " week" : " weeks");
    } else {
      long months = days / 30;
      return months + (months == 1 ? " month" : " months");
    }
  }

  @Override
  public @Nullable Long getId() {
    return id;
  }

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (injuryDate == null) {
      injuryDate = Instant.now();
    }
  }
}

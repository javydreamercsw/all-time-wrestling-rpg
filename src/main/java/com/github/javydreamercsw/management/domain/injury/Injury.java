package com.github.javydreamercsw.management.domain.injury;

import static com.github.javydreamercsw.base.domain.AbstractEntity.DESCRIPTION_MAX_LENGTH;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

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
  @JsonIgnore
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
  @Min(0) private Long healingCost = 10000L;

  @Lob
  @Column(name = "injury_notes")
  private String injuryNotes;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Column(name = "external_id", unique = true)
  @Size(max = 255) private String externalId;

  @Column(name = "last_sync")
  private Instant lastSync;

  @JsonIgnore
  public boolean isCurrentlyActive() {
    return isActive && healedDate == null;
  }

  public void heal() {
    this.isActive = false;
    this.healedDate = Instant.now();
  }

  @JsonIgnore
  public long getDaysActive() {
    Instant end = healedDate != null ? healedDate : Instant.now();
    return java.time.Duration.between(injuryDate, end).toDays();
  }

  @JsonIgnore
  public String getDisplayString() {
    String status = isCurrentlyActive() ? " (Active)" : " (Healed)";
    return String.format(
        "%s - %s (%d health penalty)%s", name, severity.getDisplayName(), healthPenalty, status);
  }

  @JsonIgnore
  public String getStatusEmoji() {
    if (!isActive) return "✅";
    return severity.getEmoji();
  }

  @JsonIgnore
  public int getHealthImpact() {
    return isCurrentlyActive() ? healthPenalty : 0;
  }

  @JsonIgnore
  public boolean canBeHealed() {
    return isCurrentlyActive();
  }

  @JsonIgnore
  public Long getHealingFanCost() {
    return healingCost;
  }

  @JsonIgnore
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

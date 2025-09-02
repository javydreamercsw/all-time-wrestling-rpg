package com.github.javydreamercsw.management.domain.rivalry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a rivalry between two wrestlers in the ATW RPG system. Tracks heat levels and
 * escalation rules for storyline development.
 *
 * <p>Heat System Rules: - At 10 Heat: They must wrestle at the next show - At 20 Heat: Repeat roll
 * to end rivalry - At 30 Heat: Forced into Stipulation Match (steel cage, hardcore, etc.)
 */
@Entity
@Table(
    name = "rivalry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"wrestler1_id", "wrestler2_id"}))
@Getter
@Setter
public class Rivalry extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rivalry_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "wrestler1_id", nullable = false)
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns"})
  private Wrestler wrestler1;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "wrestler2_id", nullable = false)
  @JsonIgnoreProperties({"rivalries", "injuries", "deck", "titleReigns"})
  private Wrestler wrestler2;

  @Column(name = "heat", nullable = false)
  @Min(0) private Integer heat = 0;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "started_date", nullable = false)
  private Instant startedDate;

  @Column(name = "ended_date")
  private Instant endedDate;

  @Lob
  @Column(name = "storyline_notes")
  private String storylineNotes;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  // Track heat events for history
  @OneToMany(mappedBy = "rivalry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnoreProperties({"rivalry"})
  private List<HeatEvent> heatEvents = new ArrayList<>();

  // ==================== ATW RPG METHODS ====================

  /** Add heat to the rivalry based on ATW RPG rules. */
  public void addHeat(int heatGain, String reason) {
    this.heat += heatGain;

    // Create heat event for tracking
    HeatEvent event = new HeatEvent();
    event.setRivalry(this);
    event.setHeatChange(heatGain);
    event.setReason(reason);
    event.setEventDate(Instant.now());
    event.setHeatAfterEvent(this.heat);
    heatEvents.add(event);
  }

  /** Check if wrestlers must wrestle at next show (10+ heat). */
  public boolean mustWrestleNextShow() {
    return isActive && heat >= 10;
  }

  /** Check if rivalry can be resolved with a roll (20+ heat). */
  public boolean canAttemptResolution() {
    return isActive && heat >= 20;
  }

  /** Check if rivalry requires stipulation match (30+ heat). */
  public boolean requiresStipulationMatch() {
    return isActive && heat >= 30;
  }

  /**
   * Attempt to resolve the rivalry with dice roll. ATW Rule: Both roll d20 → total >30 = rivalry
   * ends
   */
  public boolean attemptResolution(int wrestler1Roll, int wrestler2Roll) {
    if (!canAttemptResolution()) {
      return false;
    }

    int totalRoll = wrestler1Roll + wrestler2Roll;
    boolean resolved = totalRoll > 30;

    if (resolved) {
      endRivalry(
          "Resolved by dice roll: " + wrestler1Roll + " + " + wrestler2Roll + " = " + totalRoll);
    }

    // Create heat event for the resolution attempt
    String reason =
        resolved
            ? "Rivalry resolved by dice roll (" + totalRoll + ")"
            : "Failed resolution attempt (" + totalRoll + ")";

    HeatEvent event = new HeatEvent();
    event.setRivalry(this);
    event.setHeatChange(0);
    event.setReason(reason);
    event.setEventDate(Instant.now());
    event.setHeatAfterEvent(this.heat);
    heatEvents.add(event);

    return resolved;
  }

  /** End the rivalry. */
  public void endRivalry(String reason) {
    this.isActive = false;
    this.endedDate = Instant.now();

    // Add final heat event
    HeatEvent event = new HeatEvent();
    event.setRivalry(this);
    event.setHeatChange(0);
    event.setReason("Rivalry ended: " + reason);
    event.setEventDate(Instant.now());
    event.setHeatAfterEvent(this.heat);
    heatEvents.add(event);
  }

  /** Get the other wrestler in the rivalry. */
  public Wrestler getOpponent(Wrestler wrestler) {
    if (wrestler.equals(wrestler1)) {
      return wrestler2;
    } else if (wrestler.equals(wrestler2)) {
      return wrestler1;
    }
    throw new IllegalArgumentException("Wrestler is not part of this rivalry");
  }

  /** Check if a wrestler is involved in this rivalry. */
  public boolean involvesWrestler(Wrestler wrestler) {
    return wrestler.equals(wrestler1) || wrestler.equals(wrestler2);
  }

  /** Get rivalry intensity level based on heat. */
  public RivalryIntensity getIntensity() {
    if (heat < 10) return RivalryIntensity.SIMMERING;
    if (heat < 20) return RivalryIntensity.HEATED;
    if (heat < 30) return RivalryIntensity.INTENSE;
    return RivalryIntensity.EXPLOSIVE;
  }

  /** Get display name for the rivalry. */
  public String getDisplayName() {
    return wrestler1.getName()
        + " vs "
        + wrestler2.getName()
        + " ("
        + heat
        + " heat - "
        + getIntensity().getDisplayName()
        + ")";
  }

  /** Get duration of the rivalry in days. */
  public long getDurationDays() {
    Instant end = endedDate != null ? endedDate : Instant.now();
    return java.time.Duration.between(startedDate, end).toDays();
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
    if (startedDate == null) {
      startedDate = Instant.now();
    }
  }
}

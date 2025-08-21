package com.github.javydreamercsw.management.domain.faction;

import static com.github.javydreamercsw.management.domain.card.Card.DESCRIPTION_MAX_LENGTH;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single heat-generating event in a faction rivalry. Tracks what happened, when, and
 * how much heat was gained/lost.
 */
@Entity
@Table(name = "faction_heat_event")
@Getter
@Setter
public class FactionHeatEvent extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "faction_heat_event_id")
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "faction_rivalry_id", nullable = false)
  private FactionRivalry factionRivalry;

  @Column(name = "heat_change", nullable = false)
  private Integer heatChange;

  @Column(name = "heat_after_event", nullable = false)
  private Integer heatAfterEvent;

  @Column(name = "reason", nullable = false)
  @Size(max = DESCRIPTION_MAX_LENGTH) private String reason;

  @Column(name = "event_date", nullable = false)
  private Instant eventDate;

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
    if (eventDate == null) {
      eventDate = Instant.now();
    }
  }

  // ==================== ATW RPG METHODS ====================

  /** Check if this event increased heat. */
  public boolean isHeatIncrease() {
    return heatChange > 0;
  }

  /** Check if this event decreased heat. */
  public boolean isHeatDecrease() {
    return heatChange < 0;
  }

  /** Check if this was a neutral event (no heat change). */
  public boolean isNeutralEvent() {
    return heatChange == 0;
  }

  /** Get display string for this heat event. */
  public String getDisplayString() {
    String changeStr;
    if (heatChange > 0) {
      changeStr = "+" + heatChange;
    } else if (heatChange < 0) {
      changeStr = String.valueOf(heatChange);
    } else {
      changeStr = "±0";
    }

    return String.format("%s (%s heat → %d total)", reason, changeStr, heatAfterEvent);
  }

  /** Get the heat change with appropriate emoji. */
  public String getHeatChangeWithEmoji() {
    if (heatChange > 0) {
      return "🔥 +" + heatChange;
    } else if (heatChange < 0) {
      return "❄️ " + heatChange;
    } else {
      return "⚖️ ±0";
    }
  }

  /** Get faction names involved in this event. */
  public String getFactionNames() {
    if (factionRivalry != null) {
      return factionRivalry.getDisplayName();
    }
    return "Unknown Factions";
  }
}

package com.github.javydreamercsw.management.domain.injury;

import com.github.javydreamercsw.base.domain.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a type of injury in the ATW RPG card game system. This is reference data that defines
 * the game effects of different injury types (Head injury, Back injury, etc.) rather than tracking
 * individual wrestler injuries.
 *
 * <p>Injury types define: - Health effects (penalty to health) - Stamina effects (penalty to
 * stamina) - Card effects (penalty to card play) - Special game rule modifications
 */
@Entity
@Table(name = "injury_type")
@Getter
@Setter
public class InjuryType extends AbstractEntity<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "injury_type_id")
  private Long id;

  @Column(name = "injury_name", nullable = false, unique = true)
  @Size(max = 100) private String injuryName;

  @Column(name = "health_effect")
  private Integer healthEffect; // Typically negative (e.g., -3, -1, -2)

  @Column(name = "stamina_effect")
  private Integer staminaEffect; // Typically negative or zero (e.g., 0, -3, -2)

  @Column(name = "card_effect")
  private Integer cardEffect; // Typically negative or zero (e.g., -2, 0, -1)

  @Lob
  @Column(name = "special_effects")
  private String specialEffects; // Text description of special game effects

  // ==================== HELPER METHODS ====================

  /** Gets the effective health effect, defaulting to 0 if not specified. */
  public int getEffectiveHealthEffect() {
    return healthEffect != null ? healthEffect : 0;
  }

  /** Gets the effective stamina effect, defaulting to 0 if not specified. */
  public int getEffectiveStaminaEffect() {
    return staminaEffect != null ? staminaEffect : 0;
  }

  /** Gets the effective card effect, defaulting to 0 if not specified. */
  public int getEffectiveCardEffect() {
    return cardEffect != null ? cardEffect : 0;
  }

  /** Checks if this injury type has any special effects. */
  public boolean hasSpecialEffects() {
    return specialEffects != null
        && !specialEffects.trim().isEmpty()
        && !"N/A".equals(specialEffects.trim());
  }

  /** Gets a summary of the injury type for display. */
  public String getSummary() {
    return String.format(
        "%s (Health: %d, Stamina: %d, Card: %d)",
        injuryName,
        getEffectiveHealthEffect(),
        getEffectiveStaminaEffect(),
        getEffectiveCardEffect());
  }

  /** Calculates the total penalty severity of this injury type. */
  public int getTotalPenalty() {
    return Math.abs(getEffectiveHealthEffect())
        + Math.abs(getEffectiveStaminaEffect())
        + Math.abs(getEffectiveCardEffect());
  }

  @Override
  public String toString() {
    return "InjuryType{"
        + "id="
        + id
        + ", injuryName='"
        + injuryName
        + '\''
        + ", healthEffect="
        + healthEffect
        + ", staminaEffect="
        + staminaEffect
        + ", cardEffect="
        + cardEffect
        + ", specialEffects='"
        + specialEffects
        + '\''
        + '}';
  }
}

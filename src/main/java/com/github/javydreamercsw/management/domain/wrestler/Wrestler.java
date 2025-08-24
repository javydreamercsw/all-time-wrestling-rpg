package com.github.javydreamercsw.management.domain.wrestler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.title.Title;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "wrestler")
@Getter
@Setter
public class Wrestler extends AbstractEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "wrestler_id")
  private Long id;

  @Column(name = "name", nullable = false)
  @Size(max = Card.DESCRIPTION_MAX_LENGTH) private String name;

  @Column(name = "starting_stamina", nullable = false)
  private Integer startingStamina;

  @Column(name = "low_stamina", nullable = false)
  private Integer lowStamina;

  @Column(name = "starting_health", nullable = false)
  private Integer startingHealth;

  @Column(name = "low_health", nullable = false)
  private Integer lowHealth;

  @Column(name = "deck_size", nullable = false)
  private Integer deckSize;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  @Column(name = "external_id", unique = true)
  @Size(max = 255) private String externalId;

  // ==================== ATW RPG FIELDS ====================

  /** Fan count (stored in thousands, so 25000 fans = 25) */
  @Column(name = "fans")
  @Min(0) private Long fans = 0L;

  /** Current wrestler tier based on fan count */
  @Column(name = "tier", nullable = false)
  @Enumerated(EnumType.STRING)
  private WrestlerTier tier = WrestlerTier.ROOKIE;

  /** Number of bump tokens (0-2, converts to injury at 3) */
  @Column(name = "bumps")
  @Min(0) private Integer bumps = 0;

  /** Current health (modified by injuries and bumps) */
  @Column(name = "current_health")
  private Integer currentHealth;

  /** Whether this wrestler is controlled by a player or is an NPC */
  @Column(name = "is_player", nullable = false)
  private Boolean isPlayer = false;

  /** Character description for AI narration */
  @Column(name = "description", length = 1000)
  private String description;

  /** Wrestling style/gimmick for match narration */
  @Column(name = "wrestling_style")
  @Size(max = Card.DESCRIPTION_MAX_LENGTH) private String wrestlingStyle;

  // ==================== ATW RPG RELATIONSHIPS ====================

  /** Titles currently held by this wrestler */
  @OneToMany(mappedBy = "currentChampion", fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Title> currentTitles = new ArrayList<>();

  /** Active injuries affecting this wrestler */
  @OneToMany(mappedBy = "wrestler", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Injury> injuries = new ArrayList<>();

  /** Rivalries where this wrestler is wrestler1 */
  @OneToMany(mappedBy = "wrestler1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Rivalry> rivalriesAsWrestler1 = new ArrayList<>();

  /** Rivalries where this wrestler is wrestler2 */
  @OneToMany(mappedBy = "wrestler2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Rivalry> rivalriesAsWrestler2 = new ArrayList<>();

  /** Faction this wrestler belongs to (if any) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "faction_id")
  @com.fasterxml.jackson.annotation.JsonIgnoreProperties({
    "members",
    "rivalriesAsFaction1",
    "rivalriesAsFaction2"
  })
  private com.github.javydreamercsw.management.domain.faction.Faction faction;

  // ==================== ATW RPG METHODS ====================

  /** Calculate fan weight for match outcome system (+1 weight per full 5,000 fans) */
  public Integer getFanWeight() {
    return Math.toIntExact(fans / 5);
  }

  /** Get effective starting health (base health - bumps - injury penalties) */
  public Integer getEffectiveStartingHealth() {
    int effective = startingHealth - bumps - getTotalInjuryPenalty();
    return Math.max(1, effective); // Never go below 1
  }

  /** Check if wrestler is eligible for a specific title tier */
  public boolean isEligibleForTitle(TitleTier titleTier) {
    return titleTier.isEligible(fans);
  }

  /** Update tier based on current fan count */
  public void updateTier() {
    this.tier = WrestlerTier.fromFanCount(fans);
  }

  /** Add fans and update tier automatically */
  public void addFans(long fanGain) {
    this.fans = Math.max(0, this.fans + fanGain);
    updateTier();
  }

  /**
   * Add bump tokens, converting to injury if needed. When 3 bumps are reached, they are reset to 0
   * and an injury should be created by the calling service (typically WrestlerService.addBump()).
   *
   * @return true if injury occurred (3 bumps reached)
   */
  public boolean addBump() {
    bumps++;
    if (bumps >= 3) {
      bumps = 0; // Reset bumps - injury creation handled by service layer
      return true; // Indicates injury occurred
    }
    return false;
  }

  /** Get display name with tier emoji for UI */
  public String getDisplayNameWithTier() {
    return tier.getEmoji() + " " + name;
  }

  /** Check if wrestler can afford a specific fan cost */
  public boolean canAfford(Long cost) {
    return fans >= cost;
  }

  /** Spend fans for actions (returns true if successful) */
  public boolean spendFans(Long cost) {
    if (canAfford(cost)) {
      fans -= cost;
      updateTier();
      return true;
    }
    return false;
  }

  // ==================== ATW RPG RELATIONSHIP METHODS ====================

  /** Get all active rivalries for this wrestler. */
  public List<Rivalry> getActiveRivalries() {
    List<Rivalry> allRivalries = new ArrayList<>();
    allRivalries.addAll(rivalriesAsWrestler1.stream().filter(Rivalry::getIsActive).toList());
    allRivalries.addAll(rivalriesAsWrestler2.stream().filter(Rivalry::getIsActive).toList());
    return allRivalries;
  }

  /** Get all active injuries affecting this wrestler. */
  public List<Injury> getActiveInjuries() {
    return injuries.stream().filter(Injury::isCurrentlyActive).toList();
  }

  /** Get total health penalty from all active injuries. */
  public Integer getTotalInjuryPenalty() {
    return getActiveInjuries().stream().mapToInt(Injury::getHealthPenalty).sum();
  }

  /** Get effective starting health including injury penalties. */
  public Integer getEffectiveStartingHealthWithInjuries() {
    int effective = startingHealth - bumps - getTotalInjuryPenalty();
    return Math.max(1, effective); // Never go below 1
  }

  /** Check if wrestler is currently a champion. */
  public boolean isCurrentChampion() {
    return !currentTitles.isEmpty();
  }

  /** Get the number of titles currently held. */
  public int getTitleCount() {
    return currentTitles.size();
  }

  /** Check if wrestler has an active rivalry with another wrestler. */
  public boolean hasActiveRivalryWith(Wrestler otherWrestler) {
    return getActiveRivalries().stream()
        .anyMatch(rivalry -> rivalry.involvesWrestler(otherWrestler));
  }

  @Override
  public @Nullable Long getId() {
    return id;
  }

  // ==================== JPA LIFECYCLE METHODS ====================

  @PrePersist
  protected void onCreate() {
    if (creationDate == null) {
      creationDate = Instant.now();
    }
    if (currentHealth == null) {
      currentHealth = startingHealth;
    }
    if (fans == null) {
      fans = 0L;
    }
    if (bumps == null) {
      bumps = 0;
    }
    updateTier();
  }

  @PreUpdate
  protected void onUpdate() {
    if (fans == null) {
      fans = 0L;
    }
    if (bumps == null) {
      bumps = 0;
    }
    updateTier();
  }
}

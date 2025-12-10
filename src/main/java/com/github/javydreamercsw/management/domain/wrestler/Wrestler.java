/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.domain.wrestler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.base.domain.WrestlerData;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "wrestler")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wrestler extends AbstractEntity<Long> implements WrestlerData {
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

  @Column(name = "last_sync")
  private Instant lastSync;

  // ==================== ATW RPG FIELDS ====================

  @Column(name = "fans")
  @Min(0) @Builder.Default
  private Long fans = 0L;

  @Column(name = "tier", nullable = false)
  @Enumerated(EnumType.STRING)
  private WrestlerTier tier;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender;

  @Column(name = "bumps")
  @Min(0) @Builder.Default
  private Integer bumps = 0;

  @Column(name = "current_health")
  private Integer currentHealth;

  @Column(name = "is_player", nullable = false)
  @Builder.Default
  private Boolean isPlayer = false;

  @Column(name = "description", length = 4000)
  private String description;

  @Column(name = "image_url")
  @Size(max = 255) private String imageUrl;

  // ==================== ATW RPG RELATIONSHIPS ====================

  @ManyToMany(mappedBy = "champions", fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<TitleReign> reigns = new ArrayList<>();

  @OneToMany(mappedBy = "wrestler", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<Injury> injuries = new ArrayList<>();

  @OneToMany(mappedBy = "wrestler1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<Rivalry> rivalriesAsWrestler1 = new ArrayList<>();

  @OneToMany(mappedBy = "wrestler2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<Rivalry> rivalriesAsWrestler2 = new ArrayList<>();

  @OneToMany(
      mappedBy = "wrestler",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @JsonIgnore
  @Builder.Default
  private List<Deck> decks = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "faction_id")
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Faction faction;

  // ==================== ATW RPG METHODS ====================

  @JsonIgnore
  public Integer getFanWeight() {
    return Math.toIntExact(fans / 5);
  }

  @JsonIgnore
  public Integer getEffectiveStartingHealth() {
    int effective = startingHealth - bumps - getTotalInjuryPenalty();
    return Math.max(1, effective); // Never go below 1
  }

  public void addFans(long fanGain) {
    this.fans = Math.max(0, this.fans + fanGain);
  }

  public boolean addBump() {
    bumps++;
    if (bumps >= 3) {
      bumps = 0; // Reset bumps - injury creation handled by service layer
      return true; // Indicates injury occurred
    }
    return false;
  }

  @JsonIgnore
  public String getDisplayNameWithTier() {
    return tier.getEmoji() + " " + name;
  }

  public boolean canAfford(Long cost) {
    return fans >= cost;
  }

  public boolean spendFans(Long cost) {
    if (canAfford(cost)) {
      fans -= cost;
      return true;
    }
    return false;
  }

  // ==================== ATW RPG RELATIONSHIP METHODS ====================

  @JsonIgnore
  public List<Rivalry> getActiveRivalries() {
    List<Rivalry> allRivalries = new ArrayList<>();
    allRivalries.addAll(rivalriesAsWrestler1.stream().filter(Rivalry::getIsActive).toList());
    allRivalries.addAll(rivalriesAsWrestler2.stream().filter(Rivalry::getIsActive).toList());
    return allRivalries;
  }

  @JsonIgnore
  public List<Injury> getActiveInjuries() {
    return injuries.stream().filter(Injury::isCurrentlyActive).toList();
  }

  @JsonIgnore
  public Integer getTotalInjuryPenalty() {
    return getActiveInjuries().stream().mapToInt(Injury::getHealthPenalty).sum();
  }

  @JsonIgnore
  public Integer getCurrentHealthWithPenalties() {
    if (currentHealth == null) {
      return getEffectiveStartingHealth();
    }
    int healthWithPenalties = currentHealth - bumps - getTotalInjuryPenalty();
    return Math.max(1, healthWithPenalties); // Never go below 1
  }

  public void refreshCurrentHealth() {
    this.currentHealth = getEffectiveStartingHealth();
  }

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
    if (lowHealth == null) {
      lowHealth = startingHealth;
    }
    if (lowStamina == null) {
      lowStamina = startingStamina;
    }
    if (fans == null) {
      fans = 0L;
    }
    if (bumps == null) {
      bumps = 0;
    }
    if (gender == null) {
      gender = Gender.MALE;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    if (fans == null) {
      fans = 0L;
    }
    if (bumps == null) {
      bumps = 0;
    }
  }
}

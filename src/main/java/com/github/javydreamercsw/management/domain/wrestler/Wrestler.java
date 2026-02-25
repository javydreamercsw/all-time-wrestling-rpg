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
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.npc.Npc;
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
import org.jspecify.annotations.NonNull;
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
  @Builder.Default
  private Integer startingStamina = 0;

  @Column(name = "low_stamina", nullable = false)
  @Builder.Default
  private Integer lowStamina = 0;

  @Column(name = "starting_health", nullable = false)
  @Builder.Default
  private Integer startingHealth = 0;

  @Column(name = "low_health", nullable = false)
  @Builder.Default
  private Integer lowHealth = 0;

  @Column(name = "deck_size", nullable = false)
  @Builder.Default
  private Integer deckSize = 0;

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  // ==================== ATW RPG FIELDS ====================

  @Column(name = "fans")
  @Min(0) @Builder.Default
  private Long fans = 0L;

  @Column(name = "tier", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private WrestlerTier tier = WrestlerTier.MIDCARDER;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private Gender gender = Gender.MALE;

  @Column(name = "bumps")
  @Min(0) @Builder.Default
  private Integer bumps = 0;

  @Column(name = "current_health")
  private Integer currentHealth;

  @Column(name = "is_player", nullable = false)
  @Builder.Default
  private Boolean isPlayer = false;

  @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(name = "description", length = 4000)
  private String description;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "physical_condition")
  @Min(0) @jakarta.validation.constraints.Max(100) @Builder.Default
  private Integer physicalCondition = 100;

  @Column(name = "heritage_tag")
  private String heritageTag;

  // ==================== CAMPAIGN ATTRIBUTES ====================
  @Column(name = "drive")
  @Min(1) @jakarta.validation.constraints.Max(6) @Builder.Default
  private Integer drive = 1;

  @Column(name = "resilience")
  @Min(1) @jakarta.validation.constraints.Max(6) @Builder.Default
  private Integer resilience = 1;

  @Column(name = "charisma")
  @Min(1) @jakarta.validation.constraints.Max(6) @Builder.Default
  private Integer charisma = 1;

  @Column(name = "brawl")
  @Min(1) @jakarta.validation.constraints.Max(6) @Builder.Default
  private Integer brawl = 1;

  // ==================== ATW RPG RELATIONSHIPS ====================
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "manager_id")
  @JsonIgnore
  private Npc manager;

  @ManyToOne
  @JoinColumn(name = "account_id")
  private Account account;

  @OneToOne(mappedBy = "wrestler", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JsonIgnore
  private WrestlerAlignment alignment;

  @ManyToMany(mappedBy = "champions", fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<TitleReign> reigns = new ArrayList<>();

  @OneToMany(mappedBy = "wrestler", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
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

  @Override
  public @NonNull Gender getGender() {
    return this.gender;
  }

  // ==================== ATW RPG METHODS ====================

  @JsonIgnore
  public Integer getFanWeight() {
    return Math.toIntExact(fans / 5);
  }

  @JsonIgnore
  public Integer getEffectiveStartingHealth() {
    int bonus = 0;
    int penalty = 0;
    if (alignment != null
        && alignment.getCampaign() != null
        && alignment.getCampaign().getState() != null) {
      bonus = alignment.getCampaign().getState().getCampaignHealthBonus();
      penalty = alignment.getCampaign().getState().getHealthPenalty();
    }

    // Physical condition penalty: -1 health for every 5% lost from 100%
    // Capped at -5 health points.
    int conditionPenalty = Math.min(5, (100 - physicalCondition) / 5);

    int effective =
        startingHealth + bonus - penalty - bumps - getTotalInjuryPenalty() - conditionPenalty;
    return Math.max(1, effective); // Never go below 1
  }

  @JsonIgnore
  public Integer getEffectiveStartingStamina() {
    int bonus =
        (alignment != null
                && alignment.getCampaign() != null
                && alignment.getCampaign().getState() != null)
            ? alignment.getCampaign().getState().getCampaignStaminaBonus()
            : 0;
    return startingStamina + bonus;
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
    allRivalries.addAll(
        rivalriesAsWrestler1.stream()
            .filter(Rivalry::getIsActive)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    allRivalries.addAll(
        rivalriesAsWrestler2.stream()
            .filter(Rivalry::getIsActive)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    return allRivalries;
  }

  @JsonIgnore
  public List<Injury> getActiveInjuries() {
    return injuries.stream()
        .filter(Injury::isCurrentlyActive)
        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
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
    if (physicalCondition == null) {
      physicalCondition = 100;
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

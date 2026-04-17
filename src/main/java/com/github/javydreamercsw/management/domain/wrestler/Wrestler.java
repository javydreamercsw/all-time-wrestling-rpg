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
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private Gender gender = Gender.MALE;

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

  @Column(name = "heritage_tag")
  private String heritageTag;

  @Column(name = "expansion_code", nullable = false)
  @Builder.Default
  private String expansionCode = "BASE_GAME";

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

  @OneToMany(mappedBy = "wrestler1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<Rivalry> rivalriesAsWrestler1 = new ArrayList<>();

  @OneToMany(mappedBy = "wrestler2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<Rivalry> rivalriesAsWrestler2 = new ArrayList<>();

  @OneToMany(mappedBy = "wrestler1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship>
      relationshipsAsWrestler1 = new ArrayList<>();

  @OneToMany(mappedBy = "wrestler2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship>
      relationshipsAsWrestler2 = new ArrayList<>();

  @OneToMany(
      mappedBy = "wrestler",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @JsonIgnore
  @Builder.Default
  private List<Deck> decks = new ArrayList<>();

  @OneToMany(mappedBy = "wrestler", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<WrestlerState> wrestlerStates = new ArrayList<>();

  @JsonIgnore
  public java.util.Optional<WrestlerState> getState(Long universeId) {
    if (universeId == null) return java.util.Optional.empty();
    return wrestlerStates.stream()
        .filter(s -> s.getUniverse().getId().equals(universeId))
        .findFirst();
  }

  @Override
  public @NonNull Gender getGender() {
    return this.gender;
  }

  // ==================== ATW RPG METHODS ====================

  @JsonIgnore
  public Integer getFanWeight(Long universeId) {
    return Math.toIntExact(getFans(universeId) / 5);
  }

  @JsonIgnore
  public Long getFans(Long universeId) {
    return wrestlerStates.stream()
        .filter(s -> s.getUniverse().getId().equals(universeId))
        .map(WrestlerState::getFans)
        .findFirst()
        .orElse(0L);
  }

  @JsonIgnore
  public Integer getEffectiveStartingHealth(Long universeId) {
    int bonus = 0;
    int penalty = 0;
    if (alignment != null
        && alignment.getCampaign() != null
        && alignment.getCampaign().getState() != null) {
      bonus = alignment.getCampaign().getState().getCampaignHealthBonus();
      penalty = alignment.getCampaign().getState().getHealthPenalty();
    }

    WrestlerState state =
        wrestlerStates.stream()
            .filter(s -> s.getUniverse().getId().equals(universeId))
            .findFirst()
            .orElse(null);

    int bumpsValue = state != null ? state.getBumps() : 0;
    int condition = state != null ? state.getPhysicalCondition() : 100;
    int injuryPenalty = state != null ? state.getTotalInjuryPenalty() : 0;

    // Physical condition penalty: -1 health for every 5% lost from 100%
    // Capped at -5 health points.
    int conditionPenalty = Math.min(5, (100 - condition) / 5);

    int effective =
        startingHealth + bonus - penalty - bumpsValue - injuryPenalty - conditionPenalty;
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

  // ==================== ATW RPG RELATIONSHIP METHODS ====================

  @JsonIgnore
  public List<com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship>
      getAllRelationships() {
    List<com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship>
        allRelationships = new ArrayList<>();
    allRelationships.addAll(relationshipsAsWrestler1);
    allRelationships.addAll(relationshipsAsWrestler2);
    return allRelationships;
  }

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
    if (gender == null) {
      gender = Gender.MALE;
    }
    if (expansionCode == null) {
      expansionCode = "BASE_GAME";
    }
    if (alignment == null) {
      alignment =
          WrestlerAlignment.builder()
              .wrestler(this)
              .alignmentType(AlignmentType.NEUTRAL)
              .level(0)
              .build();
    }
  }

  @PreUpdate
  protected void onUpdate() {}

  @JsonIgnore
  public java.util.Optional<WrestlerState> getDefaultState() {
    return wrestlerStates.isEmpty()
        ? java.util.Optional.empty()
        : java.util.Optional.of(wrestlerStates.get(0));
  }

  @Override
  @JsonIgnore
  @Deprecated
  public Long getFans() {
    return getDefaultState().map(WrestlerState::getFans).orElse(0L);
  }

  @Override
  @JsonIgnore
  @Deprecated
  public WrestlerTier getTier() {
    return getDefaultState().map(WrestlerState::getTier).orElse(WrestlerTier.ROOKIE);
  }

  @Override
  @Deprecated
  public void setTier(WrestlerTier tier) {
    getDefaultState().ifPresent(s -> s.setTier(tier));
  }

  @JsonIgnore
  @Deprecated
  public Integer getBumps() {
    return getDefaultState().map(WrestlerState::getBumps).orElse(0);
  }

  @Deprecated
  public void setBumps(Integer bumps) {
    getDefaultState().ifPresent(s -> s.setBumps(bumps));
  }

  @JsonIgnore
  @Deprecated
  public Faction getFaction() {
    return getDefaultState().map(WrestlerState::getFaction).orElse(null);
  }

  @Deprecated
  public void setFaction(Faction faction) {
    getDefaultState().ifPresent(s -> s.setFaction(faction));
  }

  @JsonIgnore
  @Deprecated
  public Npc getManager() {
    return getDefaultState().map(WrestlerState::getManager).orElse(null);
  }

  @Deprecated
  public void setManager(Npc manager) {
    getDefaultState().ifPresent(s -> s.setManager(manager));
  }

  @JsonIgnore
  @Deprecated
  public Integer getPhysicalCondition() {
    return getDefaultState().map(WrestlerState::getPhysicalCondition).orElse(100);
  }

  @Deprecated
  public void setPhysicalCondition(Integer physicalCondition) {
    getDefaultState().ifPresent(s -> s.setPhysicalCondition(physicalCondition));
  }

  @JsonIgnore
  @Deprecated
  public Integer getCurrentHealth() {
    return getDefaultState().map(WrestlerState::getCurrentHealth).orElse(startingHealth);
  }

  @Deprecated
  public void setCurrentHealth(Integer currentHealth) {
    getDefaultState().ifPresent(s -> s.setCurrentHealth(currentHealth));
  }

  @JsonIgnore
  @Deprecated
  public Integer getMorale() {
    return getDefaultState().map(WrestlerState::getMorale).orElse(100);
  }

  @Deprecated
  public void setMorale(Integer morale) {
    getDefaultState().ifPresent(s -> s.setMorale(morale));
  }

  @JsonIgnore
  @Deprecated
  public Integer getManagementStamina() {
    return getDefaultState().map(WrestlerState::getManagementStamina).orElse(100);
  }

  @Deprecated
  public void setManagementStamina(Integer managementStamina) {
    getDefaultState().ifPresent(s -> s.setManagementStamina(managementStamina));
  }

  @JsonIgnore
  @Deprecated
  public List<Injury> getInjuries() {
    return getDefaultState().map(WrestlerState::getInjuries).orElse(new ArrayList<>());
  }

  @JsonIgnore
  @Deprecated
  public List<Injury> getActiveInjuries() {
    return getDefaultState().map(WrestlerState::getActiveInjuries).orElse(new ArrayList<>());
  }

  @JsonIgnore
  @Deprecated
  public Integer getTotalInjuryPenalty() {
    return getDefaultState().map(WrestlerState::getTotalInjuryPenalty).orElse(0);
  }

  @Deprecated
  public void addFans(long fanGain) {
    getDefaultState().ifPresent(s -> s.setFans(Math.max(0, s.getFans() + fanGain)));
  }

  @Deprecated
  public boolean addBump() {
    return getDefaultState().map(WrestlerState::addBump).orElse(false);
  }

  @Deprecated
  public boolean canAfford(Long cost) {
    return getFans() >= cost;
  }

  @Deprecated
  public boolean spendFans(Long cost) {
    if (canAfford(cost)) {
      addFans(-cost);
      return true;
    }
    return false;
  }

  @Deprecated
  public void refreshCurrentHealth() {
    setCurrentHealth(getEffectiveStartingHealth(1L));
  }

  @JsonIgnore
  @Deprecated
  public Integer getCurrentHealthWithPenalties() {
    return Math.max(1, getCurrentHealth() - getBumps() - getTotalInjuryPenalty());
  }

  @JsonIgnore
  @Deprecated
  public Integer getFanWeight() {
    return Math.toIntExact(getFans() / 5);
  }
}

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
import com.github.javydreamercsw.base.domain.AbstractSyncableEntity;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerStatus;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
public class Wrestler extends AbstractSyncableEntity<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter(onMethod_ = {@Nullable})
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

  @OneToMany(mappedBy = "wrestler", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @com.fasterxml.jackson.annotation.JsonIgnore
  private List<WrestlerStatus> statuses = new ArrayList<>();

  @Column(name = "creation_date", nullable = false)
  private Instant creationDate;

  // ==================== ATW RPG FIELDS ====================

  @Enumerated(EnumType.STRING)
  @Getter(onMethod_ = {@NonNull})
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

  @OneToMany(
      mappedBy = "wrestler",
      cascade = CascadeType.ALL,
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  @JsonIgnore
  @Builder.Default
  private Set<WrestlerAlignment> alignments = new LinkedHashSet<>();

  @JsonIgnore
  public WrestlerAlignment getAlignment() {
    return alignments.stream()
        .filter(a -> a.getCampaign() == null)
        .findFirst()
        .orElse(alignments.isEmpty() ? null : alignments.iterator().next());
  }

  public void setAlignment(final WrestlerAlignment alignment) {
    if (alignment != null) {
      alignment.setWrestler(this);
      this.alignments.add(alignment);
    }
  }

  @ManyToMany(mappedBy = "champions", fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private Set<TitleReign> reigns = new LinkedHashSet<>();

  @OneToMany(mappedBy = "wrestler1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private Set<Rivalry> rivalriesAsWrestler1 = new LinkedHashSet<>();

  @OneToMany(mappedBy = "wrestler2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private Set<Rivalry> rivalriesAsWrestler2 = new LinkedHashSet<>();

  @OneToMany(mappedBy = "wrestler1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private Set<com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship>
      relationshipsAsWrestler1 = new LinkedHashSet<>();

  @OneToMany(mappedBy = "wrestler2", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private Set<com.github.javydreamercsw.management.domain.relationship.WrestlerRelationship>
      relationshipsAsWrestler2 = new LinkedHashSet<>();

  @OneToMany(
      mappedBy = "wrestler",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @JsonIgnore
  @Builder.Default
  private Set<Deck> decks = new LinkedHashSet<>();

  @OneToMany(mappedBy = "wrestler", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JsonIgnore
  @Builder.Default
  private Set<WrestlerState> wrestlerStates = new LinkedHashSet<>();

  @JsonIgnore
  public java.util.Optional<WrestlerState> getState(final Long universeId) {
    if (universeId == null) {
      return java.util.Optional.empty();
    }
    return wrestlerStates.stream()
        .filter(s -> s.getUniverse() != null && universeId.equals(s.getUniverse().getId()))
        .findFirst();
  }

  // ==================== ATW RPG METHODS ====================

  @JsonIgnore
  public Integer getFanWeight(final Long universeId) {
    return Math.toIntExact(getFans(universeId) / 5);
  }

  @JsonIgnore
  public Long getFans(final Long universeId) {
    if (universeId == null) {
      return getDefaultState().map(WrestlerState::getFans).orElse(0L);
    }
    return wrestlerStates.stream()
        .filter(s -> s.getUniverse() != null && universeId.equals(s.getUniverse().getId()))
        .map(WrestlerState::getFans)
        .findFirst()
        .orElse(0L);
  }

  @JsonIgnore
  public Integer getEffectiveStartingHealth(final Long universeId) {
    int bonus = 0;
    int penalty = 0;
    WrestlerAlignment alignment = getAlignment();
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
    WrestlerAlignment alignment = getAlignment();
    int bonus =
        alignment != null
                && alignment.getCampaign() != null
                && alignment.getCampaign().getState() != null
            ? alignment.getCampaign().getState().getCampaignStaminaBonus()
            : 0;
    return startingStamina + bonus;
  }

  @JsonIgnore
  public Integer getEffectiveStartingMomentum() {
    int momentum = 0;
    WrestlerAlignment alignment = getAlignment();
    if (alignment != null
        && alignment.getCampaign() != null
        && alignment.getCampaign().getState() != null) {
      momentum += alignment.getCampaign().getState().getMomentumBonus();
    }

    for (var status : statuses) {
      String effect =
          status.getLevel() == 1
              ? status.getStatusCard().getLevel1Effect()
              : status.getStatusCard().getLevel2Effect();
      if (effect != null && effect.contains("momentum:")) {
        momentum += parseEffectValue(effect, "momentum:");
      }
    }
    return momentum;
  }

  @JsonIgnore
  public Integer getEffectiveHandSize() {
    int handSize = 5; // Default base hand size
    WrestlerAlignment alignment = getAlignment();
    if (alignment != null
        && alignment.getCampaign() != null
        && alignment.getCampaign().getState() != null) {
      handSize -= alignment.getCampaign().getState().getHandSizePenalty();
    }

    handSize -= getTotalHandSizePenalty();

    for (var status : statuses) {
      String effect =
          status.getLevel() == 1
              ? status.getStatusCard().getLevel1Effect()
              : status.getStatusCard().getLevel2Effect();
      if (effect != null && effect.contains("handSize:")) {
        handSize += parseEffectValue(effect, "handSize:");
      }
    }
    return Math.max(1, handSize);
  }

  private int parseEffectValue(final String effect, final String key) {
    try {
      String[] parts = effect.split(",");
      for (String part : parts) {
        if (part.trim().startsWith(key)) {
          String val = part.split(":")[1].trim().replace("+", "");
          return Integer.parseInt(val);
        }
      }
    } catch (Exception e) {
      // Ignore parsing errors
    }
    return 0;
  }

  @JsonIgnore
  public Integer getTotalHandSizePenalty() {
    return getDefaultState()
        .map(
            s ->
                s.getInjuries().stream()
                    .mapToInt(
                        com.github.javydreamercsw.management.domain.injury.Injury
                            ::getHandSizePenalty)
                    .sum())
        .orElse(0);
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

  public boolean hasActiveRivalryWith(final Wrestler otherWrestler) {
    return getActiveRivalries().stream()
        .anyMatch(rivalry -> rivalry.involvesWrestler(otherWrestler));
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
  }

  @PreUpdate
  protected void onUpdate() {}

  @JsonIgnore
  public java.util.Optional<WrestlerState> getDefaultState() {
    return wrestlerStates.isEmpty()
        ? java.util.Optional.empty()
        : java.util.Optional.of(wrestlerStates.iterator().next());
  }
}

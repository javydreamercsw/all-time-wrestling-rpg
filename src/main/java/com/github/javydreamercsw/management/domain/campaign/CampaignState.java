/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.domain.campaign;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "campaign_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignState {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "campaign_id", nullable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Campaign campaign;

  @Column(name = "current_chapter_id")
  private String currentChapterId;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "campaign_completed_chapters",
      joinColumns = @JoinColumn(name = "campaign_state_id"))
  @Column(name = "chapter_id")
  @Builder.Default
  private List<String> completedChapterIds = new ArrayList<>();

  @Column(name = "victory_points", nullable = false)
  private int victoryPoints;

  @Column(name = "skill_tokens", nullable = false)
  private int skillTokens;

  @Column(name = "health_penalty", nullable = false)
  private int healthPenalty;

  @Column(name = "opponent_health_penalty", nullable = false)
  @Builder.Default
  private int opponentHealthPenalty = 0;

  @Column(name = "hand_size_penalty", nullable = false)
  private int handSizePenalty;

  @Column(name = "stamina_penalty", nullable = false)
  private int staminaPenalty;

  @Column(name = "momentum_bonus", nullable = false)
  @Builder.Default
  private int momentumBonus = 0;

  @Column(name = "current_game_date")
  private java.time.LocalDate currentGameDate;

  @Column(name = "last_sync")
  private LocalDateTime lastSync;

  // ==================== NEW FIELDS ====================

  @Enumerated(EnumType.STRING)
  @Column(name = "current_phase", nullable = false)
  @Builder.Default
  private CampaignPhase currentPhase = CampaignPhase.BACKSTAGE;

  @Column(name = "actions_taken", nullable = false)
  @Builder.Default
  private int actionsTaken = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "last_action_type")
  private BackstageActionType lastActionType;

  @Column(name = "last_action_success")
  @Builder.Default
  private Boolean lastActionSuccess = true;

  @Column(name = "promo_unlocked", nullable = false)
  @Builder.Default
  private boolean promoUnlocked = false;

  @Column(name = "attack_unlocked", nullable = false)
  @Builder.Default
  private boolean attackUnlocked = false;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "campaign_state_cards",
      joinColumns = @JoinColumn(name = "campaign_state_id"),
      inverseJoinColumns = @JoinColumn(name = "card_id"))
  @Builder.Default
  private List<CampaignAbilityCard> activeCards = new ArrayList<>();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "campaign_state_upgrades",
      joinColumns = @JoinColumn(name = "campaign_state_id"),
      inverseJoinColumns = @JoinColumn(name = "upgrade_id"))
  @Builder.Default
  private List<CampaignUpgrade> upgrades = new ArrayList<>();

  @Column(name = "pending_l1_picks", nullable = false)
  @Builder.Default
  private int pendingL1Picks = 0;

  @Column(name = "pending_l2_picks", nullable = false)
  @Builder.Default
  private int pendingL2Picks = 0;

  @Column(name = "pending_l3_picks", nullable = false)
  @Builder.Default
  private int pendingL3Picks = 0;

  @Column(name = "matches_played", nullable = false)
  @Builder.Default
  private int matchesPlayed = 0;

  @Column(name = "wins", nullable = false)
  @Builder.Default
  private int wins = 0;

  @Column(name = "losses", nullable = false)
  @Builder.Default
  private int losses = 0;

  @jakarta.persistence.ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rival_id")
  private com.github.javydreamercsw.management.domain.npc.Npc rival;

  @Column(name = "finals_phase", nullable = false)
  @Builder.Default
  private boolean finalsPhase = false;

  @Column(name = "tournament_winner", nullable = false)
  @Builder.Default
  private boolean tournamentWinner = false;

  @Column(name = "failed_to_qualify", nullable = false)
  @Builder.Default
  private boolean failedToQualify = false;

  @jakarta.persistence.ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "current_match_id")
  private com.github.javydreamercsw.management.domain.show.segment.Segment currentMatch;

  @Column(name = "tournament_state")
  @Lob
  private String tournamentState;

  @com.fasterxml.jackson.annotation.JsonIgnore
  public int getCampaignStaminaBonus() {
    return (int) upgrades.stream().filter(u -> "STAMINA".equals(u.getType())).count() * 2;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public int getCampaignHealthBonus() {
    return (int) upgrades.stream().filter(u -> "HEALTH".equals(u.getType())).count() * 2;
  }
}

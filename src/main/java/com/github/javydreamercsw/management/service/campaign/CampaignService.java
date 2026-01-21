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
package com.github.javydreamercsw.management.service.campaign;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.SegmentParticipantRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CampaignService {

  private final CampaignRepository campaignRepository;
  private final CampaignStateRepository campaignStateRepository;
  private final CampaignAbilityCardRepository campaignAbilityCardRepository;
  private final WrestlerAlignmentRepository wrestlerAlignmentRepository;
  private final CampaignScriptService campaignScriptService;
  private final CampaignChapterService chapterService;
  private final WrestlerRepository wrestlerRepository;
  private final ShowService showService;
  private final SegmentService segmentService;
  private final SeasonRepository seasonRepository;
  private final SegmentTypeRepository segmentTypeRepository;
  private final ShowTypeRepository showTypeRepository;
  private final SegmentParticipantRepository participantRepository;

  public Campaign startCampaign(Wrestler wrestler) {
    if (hasActiveCampaign(wrestler)) {
      throw new IllegalStateException("Wrestler already has an active campaign.");
    }

    // Ensure alignment is initialized
    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(wrestler)
            .orElseGet(
                () -> {
                  WrestlerAlignment newAlignment =
                      WrestlerAlignment.builder()
                          .wrestler(wrestler)
                          .alignmentType(AlignmentType.NEUTRAL)
                          .level(0)
                          .build();
                  return wrestlerAlignmentRepository.save(newAlignment);
                });

    Campaign campaign =
        Campaign.builder()
            .wrestler(wrestler)
            .status(CampaignStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();

    campaign = campaignRepository.save(campaign);

    CampaignState state =
        CampaignState.builder()
            .campaign(campaign)
            .victoryPoints(0)
            .skillTokens(0)
            .bumps(0)
            .healthPenalty(0)
            .handSizePenalty(0)
            .staminaPenalty(0)
            .pendingL1Picks(0) // No picks for neutral start
            .lastSync(LocalDateTime.now())
            .build();

    // Select initial chapter
    List<CampaignChapterDTO> available = chapterService.findAvailableChapters(state);
    if (!available.isEmpty()) {
      state.setCurrentChapterId(available.get(0).getId());
    }

    campaignStateRepository.save(state);
    campaign.setState(state);

    updateAbilityCards(campaign);

    return campaignRepository.save(campaign);
  }

  /**
   * Creates a match segment for a campaign encounter.
   *
   * @param campaign The campaign.
   * @param opponentName The name of the opponent.
   * @param narration The narrative text for the match.
   * @param segmentTypeName The name of the segment type (e.g., "One on One").
   * @return The created Segment.
   */
  public Segment createMatchForEncounter(
      Campaign campaign, String opponentName, String narration, String segmentTypeName) {
    CampaignState state = campaign.getState();
    Wrestler player = campaign.getWrestler();

    // 1. Find/Create Campaign Season
    Season season =
        seasonRepository
            .findByName("Campaign Mode")
            .orElseGet(
                () -> {
                  Season s = new Season();
                  s.setName("Campaign Mode");
                  s.setStartDate(
                      java.time.LocalDate.now().atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
                  s.setEndDate(
                      java.time.LocalDate.now()
                          .plusYears(1)
                          .atStartOfDay()
                          .toInstant(java.time.ZoneOffset.UTC));
                  return seasonRepository.save(s);
                });

    // 2. Find/Create Campaign Show
    ShowType weekly =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(() -> showTypeRepository.findAll().get(0));

    Show show =
        showService.findByName("Campaign: " + player.getName()).stream()
            .findFirst()
            .orElseGet(
                () ->
                    showService.createShow(
                        "Campaign: " + player.getName(),
                        "Story matches for " + player.getName(),
                        weekly.getId(),
                        java.time.LocalDate.now(),
                        season.getId(),
                        null));

    // 3. Create Segment
    String finalTypeName = segmentTypeName != null ? segmentTypeName : "One on One";
    SegmentType type =
        segmentTypeRepository
            .findByName(finalTypeName)
            .orElseGet(
                () ->
                    segmentTypeRepository
                        .findByName("One on One")
                        .orElseGet(() -> segmentTypeRepository.findAll().get(0)));

    Segment segment = segmentService.createSegment(show, type, java.time.Instant.now());
    segment.setNarration(narration);
    segmentService.updateSegment(segment);

    // 4. Assign Participants
    Wrestler opponent =
        wrestlerRepository
            .findByName(opponentName)
            .orElseThrow(() -> new IllegalArgumentException("Opponent not found: " + opponentName));

    addParticipant(segment, player);
    addParticipant(segment, opponent);

    state.setCurrentMatch(segment);
    state.setCurrentPhase(CampaignPhase.MATCH);
    campaignStateRepository.save(state);

    return segment;
  }

  private void addParticipant(Segment segment, Wrestler wrestler) {
    SegmentParticipant participant = new SegmentParticipant();
    participant.setSegment(segment);
    participant.setWrestler(wrestler);
    participant.setIsWinner(false);
    participantRepository.save(participant);
  }

  /**
   * Shifts the wrestler's alignment on the track. Positive moves toward Face, negative moves toward
   * Heel.
   *
   * @param campaign The campaign.
   * @param amount The shift amount.
   */
  public void shiftAlignment(Campaign campaign, int amount) {
    if (amount == 0) return;

    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(campaign.getWrestler())
            .orElseThrow(() -> new IllegalStateException("Alignment not found"));

    int oldLevel = alignment.getLevel();
    AlignmentType oldType = alignment.getAlignmentType();

    // Logic for bidirectional shift
    if (oldType == AlignmentType.NEUTRAL) {
      if (amount > 0) {
        alignment.setAlignmentType(AlignmentType.FACE);
        alignment.setLevel(amount);
      } else {
        alignment.setAlignmentType(AlignmentType.HEEL);
        alignment.setLevel(Math.abs(amount));
      }
    } else if (oldType == AlignmentType.FACE) {
      int newLevel = oldLevel + amount;
      if (newLevel <= 0) {
        alignment.setAlignmentType(AlignmentType.NEUTRAL);
        alignment.setLevel(0);
      } else {
        alignment.setLevel(Math.min(5, newLevel));
      }
    } else { // HEEL
      // amount > 0 moves toward Neutral (reduces level)
      // amount < 0 moves further toward Heel (increases level)
      int newLevel = oldLevel - amount;
      if (newLevel <= 0) {
        alignment.setAlignmentType(AlignmentType.NEUTRAL);
        alignment.setLevel(0);
      } else {
        alignment.setLevel(Math.min(5, newLevel));
      }
    }

    wrestlerAlignmentRepository.save(alignment);
    handleLevelChange(campaign, oldLevel, alignment.getLevel());

    // If type changed (e.g. turned), update ability cards
    if (alignment.getAlignmentType() != oldType) {
      updateAbilityCards(campaign);
    }
  }

  public com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO getCurrentChapter(
      Campaign campaign) {
    return chapterService
        .getChapter(campaign.getState().getCurrentChapterId())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Chapter config not found: " + campaign.getState().getCurrentChapterId()));
  }

  /**
   * Checks if the wrestler has an active campaign.
   *
   * @param wrestler The wrestler.
   * @return true if an active campaign exists.
   */
  public boolean hasActiveCampaign(Wrestler wrestler) {
    return campaignRepository.findActiveByWrestler(wrestler).isPresent();
  }

  public void processMatchResult(Campaign campaign, boolean won) {
    CampaignState state = campaign.getState();
    Wrestler wrestler = campaign.getWrestler();
    com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO.ChapterRules rules =
        getCurrentChapter(campaign).getRules();

    state.setMatchesPlayed(state.getMatchesPlayed() + 1);
    if (won) {
      state.setWins(state.getWins() + 1);
      state.setVictoryPoints(state.getVictoryPoints() + rules.getVictoryPointsWin());
    } else {
      state.setLosses(state.getLosses() + 1);
      state.setVictoryPoints(state.getVictoryPoints() + rules.getVictoryPointsLoss());
    }

    // Check for chapter completion/tournament qualification
    if ("ch2_tournament".equals(state.getCurrentChapterId())) {
      if (!state.isFinalsPhase()) {
        // Qualifying Phase
        if (state.getMatchesPlayed() >= rules.getQualifyingMatches()) {
          if (state.getWins() >= rules.getMinWinsToQualify()) {
            log.info("Wrestler {} QUALIFIED for the tournament finals!", wrestler.getName());
            state.setFinalsPhase(true);
            state.setMatchesPlayed(0); // Reset for finals phase tracking
            state.setWins(0);
            state.setLosses(0);
          } else {
            log.info(
                "Wrestler {} FAILED to qualify for the tournament finals.", wrestler.getName());
            state.setFailedToQualify(true);
          }
        }
      } else {
        // Finals Phase
        if (!won) {
          log.info("Wrestler {} ELIMINATED from the tournament finals.", wrestler.getName());
          // They lost in the finals, they didn't win the tournament
          // We can use a flag or just let the matchesPlayed count determine where they failed
        } else if (state.getWins() >= rules.getTotalFinalsMatches()) {
          log.info("Wrestler {} WON the tournament finals!", wrestler.getName());
          state.setTournamentWinner(true);
        }
      }
    }

    state.setCurrentPhase(CampaignPhase.POST_MATCH);

    // Unlock backstage actions after first match
    if (state.getMatchesPlayed() == 1 && state.getCompletedChapterIds().isEmpty()) {
      WrestlerAlignment alignment =
          wrestlerAlignmentRepository
              .findByWrestler(wrestler)
              .orElseThrow(() -> new IllegalStateException("Alignment not found"));

      if (alignment.getAlignmentType() == AlignmentType.FACE) {
        state.setPromoUnlocked(true);
        log.info("Wrestler {} unlocked Promo action (Face alignment).", wrestler.getName());
      } else if (alignment.getAlignmentType() == AlignmentType.HEEL) {
        state.setPromoUnlocked(true);
        state.setAttackUnlocked(true);
        log.info(
            "Wrestler {} unlocked Promo and Attack actions (Heel alignment).", wrestler.getName());
      }
    }

    campaignStateRepository.save(state);

    // Automatic check for chapter completion
    if (chapterService.isChapterComplete(state)) {
      log.info("Chapter {} complete! Ready to advance.", state.getCurrentChapterId());
      // For now we don't automatically advance here to allow for post-match narrative.
      // The CampaignNarrativeView choice will likely trigger advanceChapter.
    }
  }

  /**
   * Completes the post-match narrative and returns the campaign to the backstage phase.
   *
   * @param campaign The campaign to transition.
   */
  public void completePostMatch(Campaign campaign) {
    CampaignState state = campaign.getState();
    state.setCurrentPhase(CampaignPhase.BACKSTAGE);
    state.setActionsTaken(0); // Reset actions for the next "turn"
    state.setCurrentMatch(null); // Clear the match reference now that post-match is done
    campaignStateRepository.save(state);
  }

  public void advanceChapter(Campaign campaign) {
    CampaignState state = campaign.getState();
    String oldId = state.getCurrentChapterId();

    if (oldId != null) {
      state.getCompletedChapterIds().add(oldId);
    }

    List<CampaignChapterDTO> available = chapterService.findAvailableChapters(state);
    if (!available.isEmpty()) {
      // Pick next chapter - could be enhanced with choosing logic
      state.setCurrentChapterId(available.get(0).getId());
      state.setMatchesPlayed(0); // Reset chapter-specific counters
      state.setWins(0);
      state.setLosses(0);
      state.setFinalsPhase(false);
      campaignStateRepository.save(state);
      log.info("Advanced to chapter: {}", state.getCurrentChapterId());
    } else {
      completeCampaign(campaign);
    }
  }

  public void completeCampaign(Campaign campaign) {
    campaign.setStatus(CampaignStatus.COMPLETED);
    campaign.setEndedAt(LocalDateTime.now());
    campaignRepository.save(campaign);
  }

  /**
   * Updates the wrestler's available ability cards based on their alignment and level. If the
   * wrestler has turned (changed alignment), all current cards are removed and the system resets
   * card inventory based on the current level.
   *
   * @param campaign The campaign to update.
   */
  public void updateAbilityCards(Campaign campaign) {
    Wrestler wrestler = campaign.getWrestler();
    Optional<WrestlerAlignment> alignmentOpt = wrestlerAlignmentRepository.findByWrestler(wrestler);

    if (alignmentOpt.isEmpty()) {
      return; // No alignment tracking yet
    }

    WrestlerAlignment alignment = alignmentOpt.get();
    CampaignState state = campaign.getState();
    List<CampaignAbilityCard> currentCards = state.getActiveCards();

    // Check for alignment mismatch (Turn happened)
    boolean alignmentChanged =
        currentCards.stream().anyMatch(c -> c.getAlignmentType() != alignment.getAlignmentType());

    if (alignmentChanged) {
      // Discard all cards of wrong alignment
      currentCards.clear();
      recalculatePendingPicks(state, alignment);
      log.info(
          "Wrestler {} turned. Card inventory cleared and picks recalculated.", wrestler.getName());
    }

    state.setActiveCards(currentCards);
    campaignStateRepository.save(state);
  }

  private void recalculatePendingPicks(CampaignState state, WrestlerAlignment alignment) {
    int level = alignment.getLevel();
    AlignmentType type = alignment.getAlignmentType();

    state.setPendingL1Picks(0);
    state.setPendingL2Picks(0);
    state.setPendingL3Picks(0);

    if (type == AlignmentType.FACE) {
      if (level >= 1 && level < 5) state.setPendingL1Picks(1);
      if (level >= 4) state.setPendingL2Picks(1);
      if (level >= 5) state.setPendingL3Picks(1);
    } else {
      // HEEL
      if (level >= 1 && level < 4) state.setPendingL1Picks(1);
      if (level >= 4) state.setPendingL2Picks(1);
      if (level >= 5) state.setPendingL1Picks(1); // Regain L1 slot
    }
  }

  /**
   * Handles track level changes, applying gain/loss rules for ability cards.
   *
   * @param campaign The campaign.
   * @param oldLevel The previous level.
   * @param newLevel The new level.
   */
  public void handleLevelChange(Campaign campaign, int oldLevel, int newLevel) {
    Wrestler wrestler = campaign.getWrestler();
    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(wrestler)
            .orElseThrow(() -> new IllegalStateException("Alignment not found"));

    AlignmentType type = alignment.getAlignmentType();
    CampaignState state = campaign.getState();
    List<CampaignAbilityCard> cards = state.getActiveCards();

    // Grant first pick when reaching Level 1 from 0 (Neutral)
    if (oldLevel == 0 && newLevel >= 1 && type != AlignmentType.NEUTRAL) {
      log.info("Reached Level 1 {}: Eligible for first Level 1 card.", type);
      state.setPendingL1Picks(state.getPendingL1Picks() + 1);
    }

    // Rules logic
    if (type == AlignmentType.FACE) {
      // Face Level 4: Gain a level 2 card
      if (oldLevel < 4 && newLevel >= 4) {
        log.info("Face reached Level 4: Eligible for Level 2 card.");
        state.setPendingL2Picks(state.getPendingL2Picks() + 1);
      }
      // Face Level 5: Gain a level 3 card, lose a level 1 card
      if (oldLevel < 5 && newLevel >= 5) {
        log.info("Face reached Level 5: Gain Level 3 card, Lose Level 1 card.");
        removeOneCardOfLevel(cards, 1);
        state.setPendingL3Picks(state.getPendingL3Picks() + 1);
        // If they had no L1 card yet, we might want to decrement pending L1 picks instead?
        if (state.getPendingL1Picks() > 0) {
          state.setPendingL1Picks(state.getPendingL1Picks() - 1);
        }
      }
    } else {
      // Heel Level 4: Gain a level 2 card, lose a level 1 card
      if (oldLevel < 4 && newLevel >= 4) {
        log.info("Heel reached Level 4: Gain Level 2 card, Lose Level 1 card.");
        removeOneCardOfLevel(cards, 1);
        state.setPendingL2Picks(state.getPendingL2Picks() + 1);
        if (state.getPendingL1Picks() > 0) {
          state.setPendingL1Picks(state.getPendingL1Picks() - 1);
        }
      }
      // Heel Level 5: Gain another level 1 card
      if (oldLevel < 5 && newLevel >= 5) {
        log.info("Heel reached Level 5: Eligible for another Level 1 card.");
        state.setPendingL1Picks(state.getPendingL1Picks() + 1);
      }
    }

    campaignStateRepository.save(state);
  }

  private void removeOneCardOfLevel(List<CampaignAbilityCard> cards, int level) {
    cards.stream()
        .filter(c -> c.getLevel() == level)
        .findFirst()
        .ifPresent(
            card -> {
              cards.remove(card);
              log.info("Removed Level {} card: {}", level, card.getName());
            });
  }

  /**
   * Gets cards that the wrestler is eligible to pick based on current state.
   *
   * @param campaign The campaign.
   * @return List of pickable cards.
   */
  public List<CampaignAbilityCard> getPickableCards(Campaign campaign) {
    Wrestler wrestler = campaign.getWrestler();
    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(wrestler)
            .orElseThrow(() -> new IllegalStateException("Alignment not found"));

    CampaignState state = campaign.getState();
    AlignmentType type = alignment.getAlignmentType();

    List<CampaignAbilityCard> pickable = new ArrayList<>();

    if (state.getPendingL1Picks() > 0) pickable.addAll(getAvailableCards(type, 1));
    if (state.getPendingL2Picks() > 0) pickable.addAll(getAvailableCards(type, 2));
    if (state.getPendingL3Picks() > 0) pickable.addAll(getAvailableCards(type, 3));

    // Filter out cards already owned
    pickable.removeIf(state.getActiveCards()::contains);

    return pickable;
  }

  private List<CampaignAbilityCard> getAvailableCards(AlignmentType type, int level) {
    return campaignAbilityCardRepository.findByAlignmentTypeAndLevel(type, level);
  }

  /**
   * Picks a card for the wrestler.
   *
   * @param campaign The campaign.
   * @param cardId The card ID.
   */
  public void pickAbilityCard(Campaign campaign, Long cardId) {
    CampaignAbilityCard card =
        campaignAbilityCardRepository
            .findById(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Card not found"));

    CampaignState state = campaign.getState();
    if (!state.getActiveCards().contains(card)) {
      state.getActiveCards().add(card);

      // Decrement appropriate pending pick counter
      switch (card.getLevel()) {
        case 1:
          if (state.getPendingL1Picks() > 0) state.setPendingL1Picks(state.getPendingL1Picks() - 1);
          break;
        case 2:
          if (state.getPendingL2Picks() > 0) state.setPendingL2Picks(state.getPendingL2Picks() - 1);
          break;
        case 3:
          if (state.getPendingL3Picks() > 0) state.setPendingL3Picks(state.getPendingL3Picks() - 1);
          break;
      }

      campaignStateRepository.save(state);
      log.info("Wrestler {} picked card: {}", campaign.getWrestler().getName(), card.getName());
    }
  }

  /**
   * Uses a one-time ability card, removing it from the active inventory.
   *
   * @param campaign The campaign.
   * @param cardId The ID of the card to use.
   */
  public void useAbilityCard(Campaign campaign, Long cardId) {
    CampaignState state = campaign.getState();
    CampaignAbilityCard card =
        campaignAbilityCardRepository
            .findById(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Card not found"));

    if (state.getActiveCards().contains(card)) {
      boolean anyEffectTriggered = false;

      if (card.isOneTimeUse()) {
        campaignScriptService.executeEffect(card.getEffectScript(), campaign);
        anyEffectTriggered = true;
      }

      if (card.isSecondaryOneTimeUse()) {
        campaignScriptService.executeEffect(card.getSecondaryEffectScript(), campaign);
        anyEffectTriggered = true;
      }

      if (anyEffectTriggered) {
        // TODO: For cards with both passive and one-time effects, we might want to keep the card
        // in activeCards but mark the one-time effect as spent.
        // For now, we remove the card entirely when a one-time effect is used.
        state.getActiveCards().remove(card);
        campaignStateRepository.save(state);
        log.info(
            "Used ability card: {} for wrestler: {}",
            card.getName(),
            campaign.getWrestler().getName());
      }
    }
  }

  /**
   * Switches the wrestler's alignment (Turn).
   *
   * @param campaign The campaign.
   */
  public void turnWrestler(Campaign campaign) {
    Wrestler wrestler = campaign.getWrestler();
    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(wrestler)
            .orElseThrow(() -> new IllegalStateException("Wrestler has no alignment set"));

    AlignmentType newType =
        alignment.getAlignmentType() == AlignmentType.FACE
            ? AlignmentType.HEEL
            : AlignmentType.FACE;
    alignment.setAlignmentType(newType);
    // Keep level? Rules say "based on their current position on the track".
    // Usually a turn might reset or keep progress. Assuming keep level for now based on "based on
    // their current position".
    wrestlerAlignmentRepository.save(alignment);

    updateAbilityCards(campaign);
    log.info("Wrestler {} turned to {}", wrestler.getName(), newType);
  }
}

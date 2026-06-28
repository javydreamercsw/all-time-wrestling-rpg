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
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.domain.campaign.CampaignStorylineRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.SegmentParticipantRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
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
  private final CampaignChapterService chapterService;
  private final AlignmentService alignmentService;
  private final WrestlerRepository wrestlerRepository;
  private final ShowService showService;
  private final ShowRepository showRepository;
  private final SegmentService segmentService;
  private final SegmentRepository segmentRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final SeasonRepository seasonRepository;
  private final SegmentTypeRepository segmentTypeRepository;
  private final ShowTypeRepository showTypeRepository;
  private final ShowTemplateRepository showTemplateRepository;
  private final SegmentParticipantRepository participantRepository;
  private final TournamentService tournamentService;
  private final CampaignStorylineRepository storylineRepository;
  private final UniverseContextService universeContextService;
  private final UniverseRepository universeRepository;
  private final TitleRepository titleRepository;
  private final TitleReignRepository titleReignRepository;
  private final TeamRepository teamRepository;
  private final TitleService titleService;
  private final SegmentAdjudicationService adjudicationService;
  private final NewsGenerationService newsGenerationService;
  private final StorylineDirectorService storylineDirectorService;
  private final StorylineExportService storylineExportService;
  private final WrestlerStatusService wrestlerStatusService;
  private final FeatureDataService featureDataService;

  // Field-injected to break circular dependency: CampaignService ↔ MatchResultProcessorService
  @org.springframework.beans.factory.annotation.Autowired
  private MatchResultProcessorService matchResultProcessorService;

  @org.springframework.beans.factory.annotation.Autowired
  private CampaignProgressionService campaignProgressionService;

  public void setFeatureValue(final CampaignState state, final String key, final Object value) {
    featureDataService.setFeatureValue(state, key, value);
  }

  public List<CampaignChapterDTO> findStartingChapters(@NonNull final Wrestler wrestlerParam) {
    Wrestler wrestler =
        wrestlerRepository
            .findById(wrestlerParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Wrestler not found"));
    wrestler.getReigns().size();
    Campaign fakeCampaign =
        Campaign.builder()
            .wrestler(wrestler)
            .status(CampaignStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();
    CampaignState blankState =
        CampaignState.builder()
            .campaign(fakeCampaign)
            .victoryPoints(0)
            .skillTokens(0)
            .healthPenalty(0)
            .handSizePenalty(0)
            .staminaPenalty(0)
            .pendingL1Picks(0)
            .build();
    List<CampaignChapterDTO> available =
        chapterService.findAvailableChapters(blankState, wrestler.getName());
    if (available.isEmpty()) {
      available = chapterService.findAvailableChapters(blankState, null);
    }
    return available;
  }

  public Campaign startCampaign(@NonNull final Wrestler wrestler) {
    List<CampaignChapterDTO> available = findStartingChapters(wrestler);
    return startCampaign(wrestler, available.isEmpty() ? null : available.get(0).getId());
  }

  public Campaign startCampaign(
      @NonNull final Wrestler wrestlerParam, final String startingChapterId) {
    // Re-fetch to ensure attached and initialize lazy collections
    Wrestler wrestler =
        wrestlerRepository
            .findById(wrestlerParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Wrestler not found"));

    // Initialize lazy collections accessed by criteria checks (e.g. isChampion checks reigns)
    wrestler.getReigns().size();

    if (hasActiveCampaign(wrestler)) {
      throw new IllegalStateException("Wrestler already has an active campaign.");
    }

    // Every new campaign starts at NEUTRAL regardless of the wrestler's previous alignment.
    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(wrestler)
            .orElseGet(
                () ->
                    WrestlerAlignment.builder()
                        .wrestler(wrestler)
                        .alignmentType(AlignmentType.NEUTRAL)
                        .level(0)
                        .build());
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    wrestlerAlignmentRepository.save(alignment);

    Universe universe =
        universeRepository.findById(universeContextService.getCurrentUniverseId()).orElse(null);

    Campaign campaign =
        Campaign.builder()
            .wrestler(wrestler)
            .universe(universe)
            .status(CampaignStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();

    campaign = campaignRepository.save(campaign);

    // Link alignment to campaign
    alignment.setCampaign(campaign);
    wrestlerAlignmentRepository.save(alignment);

    CampaignState state =
        CampaignState.builder()
            .campaign(campaign)
            .victoryPoints(0)
            .skillTokens(0)
            .healthPenalty(0)
            .handSizePenalty(0)
            .staminaPenalty(0)
            .pendingL1Picks(0) // No picks for neutral start
            .build();

    // Select initial chapter and apply any title setup it declares.
    // First try wrestler-specific chapters; fall back to unrestricted so generic wrestlers
    // (e.g. those whose name isn't listed in allowedWrestlerNames) still get a starting chapter.
    List<CampaignChapterDTO> available =
        chapterService.findAvailableChapters(state, wrestler.getName());
    if (available.isEmpty()) {
      available = chapterService.findAvailableChapters(state, null);
    }
    if (!available.isEmpty()) {
      CampaignChapterDTO initialChapter =
          startingChapterId != null
              ? available.stream()
                  .filter(c -> c.getId().equals(startingChapterId))
                  .findFirst()
                  .orElse(available.get(0))
              : available.get(0);
      state.setCurrentChapterId(initialChapter.getId());
      campaignProgressionService.applyInitialChampions(initialChapter);
    }

    campaignStateRepository.save(state);
    campaign.setState(state);

    alignmentService.updateAbilityCards(campaign);

    campaign = campaignRepository.save(campaign);

    return campaign;
  }

  /**
   * Creates a match segment for a campaign encounter.
   *
   * @param campaignParam The campaign.
   * @param opponentName The name of the opponent.
   * @param narration The narrative text for the match.
   * @param segmentTypeName The name of the segment type (e.g., "One on One").
   * @return The created Segment.
   */
  public Segment createMatchForEncounter(
      @NonNull Campaign campaignParam,
      @NonNull String opponentName,
      @NonNull String narration,
      @NonNull String segmentTypeName,
      String... segmentRules) {
    return matchResultProcessorService.createMatchForEncounter(
        campaignParam, opponentName, narration, segmentTypeName, segmentRules);
  }

  /**
   * Shifts the wrestler's alignment on the track. Positive moves toward Face, negative moves toward
   * Heel.
   *
   * @param campaign The campaign.
   * @param amount The shift amount.
   */
  public void shiftAlignment(@NonNull Campaign campaign, int amount) {
    alignmentService.shiftAlignment(campaign, amount);
  }

  public Optional<CampaignChapterDTO> getCurrentChapter(@NonNull Campaign campaign) {
    CampaignState state = campaign.getState();
    String currentChapterId = state.getCurrentChapterId();
    if (currentChapterId == null) {
      return Optional.empty();
    }

    Optional<CampaignChapterDTO> chapter = chapterService.getChapter(currentChapterId);
    if (chapter.isPresent()) {
      return chapter;
    }

    // Not found in static chapters, check if it's an active AI storyline
    if (state.getActiveStoryline() != null
        && currentChapterId.equals(state.getActiveStoryline().getTitle())) {
      return Optional.of(storylineExportService.toChapterDTO(state.getActiveStoryline()));
    }

    return Optional.empty();
  }

  /**
   * Retrieves all storylines for a campaign, initialized for view usage.
   *
   * @param campaign The campaign.
   * @return List of storylines.
   */
  public List<CampaignStoryline> getStorylineHistory(@NonNull Campaign campaign) {
    List<CampaignStoryline> storylines =
        storylineRepository.findByCampaignOrderByStartedAtDesc(campaign);
    storylines.forEach(s -> s.getMilestones().size());
    return storylines;
  }

  /**
   * Retrieves the active campaign for a wrestler and initializes necessary lazy collections.
   *
   * @param wrestler The wrestler.
   * @return Optional campaign.
   */
  public Optional<Campaign> getCampaignForWrestler(@NonNull final Wrestler wrestler) {
    return campaignRepository
        .findActiveByWrestler(wrestler)
        .map(
            campaign -> {
              CampaignState state = campaign.getState();
              if (state != null && state.getActiveStoryline() != null) {
                // Initialize milestones to prevent LazyInitializationException in views
                state.getActiveStoryline().getMilestones().size();
              }
              // Initialize collections used in many views/criteria
              campaign.getWrestler().getReigns().size();
              // Initialize alignments to prevent LazyInitializationException in
              // CampaignDashboardView
              campaign.getWrestler().getAlignments().size();
              campaign.getWrestler().getWrestlerStates().size();
              return campaign;
            });
  }

  /**
   * Returns the active campaign for the given wrestler scoped to a specific universe. Use this
   * instead of {@link #getCampaignForWrestler(Wrestler)} when the current universe context matters
   * (e.g. campaign dashboard, to prevent tutorial campaigns from leaking into other universes).
   */
  public Optional<Campaign> getCampaignForWrestlerInUniverse(
      @NonNull final Wrestler wrestler, @NonNull final Universe universe) {
    return campaignRepository
        .findActiveByWrestlerAndUniverse(wrestler, universe)
        .map(
            campaign -> {
              CampaignState state = campaign.getState();
              if (state != null && state.getActiveStoryline() != null) {
                state.getActiveStoryline().getMilestones().size();
              }
              campaign.getWrestler().getReigns().size();
              campaign.getWrestler().getAlignments().size();
              campaign.getWrestler().getWrestlerStates().size();
              return campaign;
            });
  }

  /**
   * Checks if the wrestler has an active campaign.
   *
   * @param wrestler The wrestler.
   * @return true if an active campaign exists.
   */
  public boolean hasActiveCampaign(@NonNull final Wrestler wrestler) {
    return campaignRepository.findActiveByWrestler(wrestler).isPresent();
  }

  /** Universe-scoped variant of {@link #hasActiveCampaign(Wrestler)}. */
  public boolean hasActiveCampaignInUniverse(
      @NonNull final Wrestler wrestler, @NonNull final Universe universe) {
    return campaignRepository.findActiveByWrestlerAndUniverse(wrestler, universe).isPresent();
  }

  public void processMatchResult(@NonNull final Campaign campaignParam, final boolean won) {
    matchResultProcessorService.processMatchResult(campaignParam, won);
  }

  /**
   * Completes the post-match narrative and returns the campaign to the backstage phase.
   *
   * @param campaignParam The campaign to transition.
   */
  public void completePostMatch(@NonNull final Campaign campaignParam) {
    matchResultProcessorService.completePostMatch(campaignParam);
  }

  public Show getOrCreateCampaignShow(@NonNull Campaign campaign) {
    return matchResultProcessorService.getOrCreateCampaignShow(campaign);
  }

  public SegmentType getPromoSegmentType() {
    return matchResultProcessorService.getPromoSegmentType();
  }

  public void saveSegment(@NonNull final Segment segment) {
    matchResultProcessorService.saveSegment(segment);
  }

  public void completeCampaign(@NonNull final Campaign campaign) {
    campaignProgressionService.completeCampaign(campaign);
  }

  public void abandonCampaign(@NonNull final Campaign campaign) {
    campaignProgressionService.abandonCampaign(campaign);
  }

  public Optional<String> advanceChapter(@NonNull final Campaign campaignParam) {
    return campaignProgressionService.advanceChapter(campaignParam);
  }

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public java.util.List<com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO>
      getAvailableNextChapters(@NonNull final Campaign campaignParam) {
    return campaignProgressionService.getAvailableNextChapters(campaignParam);
  }

  public Optional<String> advanceToChapter(
      @NonNull final Campaign campaignParam, @NonNull final String targetChapterId) {
    return campaignProgressionService.advanceToChapter(campaignParam, targetChapterId);
  }

  @Transactional(readOnly = true)
  public boolean isChapterComplete(@NonNull final Campaign campaignParam) {
    return campaignProgressionService.isChapterComplete(campaignParam);
  }

  /**
   * Gets cards that the wrestler is eligible to pick based on current state.
   *
   * @param campaign The campaign.
   * @return List of pickable cards.
   */
  public List<CampaignAbilityCard> getPickableCards(@NonNull Campaign campaign) {
    Wrestler wrestler = campaign.getWrestler();
    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(wrestler)
            .orElseThrow(() -> new IllegalStateException("Alignment not found"));

    CampaignState state = campaign.getState();
    AlignmentType type = alignment.getAlignmentType();

    List<CampaignAbilityCard> pickable = new ArrayList<>();

    if (state.getPendingL1Picks() > 0) {
      pickable.addAll(getAvailableCards(type, 1));
    }
    if (state.getPendingL2Picks() > 0) {
      pickable.addAll(getAvailableCards(type, 2));
    }
    if (state.getPendingL3Picks() > 0) {
      pickable.addAll(getAvailableCards(type, 3));
    }

    // Filter out cards already owned
    pickable.removeIf(state.getActiveCards()::contains);

    return pickable;
  }

  private List<CampaignAbilityCard> getAvailableCards(
      @NonNull final AlignmentType type, final int level) {
    return campaignAbilityCardRepository.findByAlignmentTypeAndLevel(type, level);
  }

  /**
   * Picks a card for the wrestler.
   *
   * @param campaign The campaign.
   * @param cardId The card ID.
   */
  public void pickAbilityCard(@NonNull Campaign campaign, @NonNull Long cardId) {
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
          if (state.getPendingL1Picks() > 0) {
            state.setPendingL1Picks(state.getPendingL1Picks() - 1);
          }
          break;
        case 2:
          if (state.getPendingL2Picks() > 0) {
            state.setPendingL2Picks(state.getPendingL2Picks() - 1);
          }
          break;
        case 3:
          if (state.getPendingL3Picks() > 0) {
            state.setPendingL3Picks(state.getPendingL3Picks() - 1);
          }
          break;
      }

      campaignStateRepository.save(state);
      log.info("Wrestler {} picked card: {}", campaign.getWrestler().getName(), card.getName());
    }
  }
}

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
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
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.service.match.MatchRewardService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
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
  private final WrestlerRepository wrestlerRepository;
  private final ShowService showService;
  private final SegmentService segmentService;
  private final SegmentRepository segmentRepository;
  private final SegmentRuleRepository segmentRuleRepository;
  private final SeasonRepository seasonRepository;
  private final SegmentTypeRepository segmentTypeRepository;
  private final ShowTypeRepository showTypeRepository;
  private final ShowTemplateRepository showTemplateRepository;
  private final SegmentParticipantRepository participantRepository;
  private final TournamentService tournamentService;
  private final TitleRepository titleRepository;
  private final TitleReignRepository titleReignRepository;
  private final TeamRepository teamRepository;
  private final TitleService titleService;
  private final SegmentAdjudicationService adjudicationService;
  private final MatchRewardService matchRewardService;
  private final ObjectMapper objectMapper;

  private final Random random = new Random();

  private static final String KEY_FINALS_PHASE = "finalsPhase";
  private static final String KEY_TOURNAMENT_WINNER = "tournamentWinner";
  private static final String KEY_FAILED_TO_QUALIFY = "failedToQualify";
  private static final String KEY_WON_FINALE = "wonFinale";
  private static final String KEY_PARTNER_ID = "partnerId";
  private static final String KEY_TOURNAMENT_STATE = "tournamentState";
  private static final String KEY_RECRUITING_PARTNER = "recruitingPartner";

  private Map<String, Object> getFeatureData(CampaignState state) {
    if (state.getFeatureData() == null) {
      return new HashMap<>();
    }
    try {
      return objectMapper.readValue(
          state.getFeatureData(), new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      log.error("Error parsing feature data", e);
      return new HashMap<>();
    }
  }

  private void saveFeatureData(CampaignState state, Map<String, Object> data) {
    try {
      state.setFeatureData(objectMapper.writeValueAsString(data));
    } catch (JsonProcessingException e) {
      log.error("Error serializing feature data", e);
    }
  }

  private <T> T getFeatureValue(CampaignState state, String key, Class<T> type, T defaultValue) {
    Map<String, Object> data = getFeatureData(state);
    Object value = data.get(key);
    if (value == null) {
      return defaultValue;
    }
    return objectMapper.convertValue(value, type);
  }

  public void setFeatureValue(CampaignState state, String key, Object value) {
    Map<String, Object> data = getFeatureData(state);
    data.put(key, value);
    saveFeatureData(state, data);
  }

  public Campaign startCampaign(@NonNull Wrestler wrestlerParam) {
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
      @NonNull Campaign campaignParam,
      @NonNull String opponentName,
      @NonNull String narration,
      @NonNull String segmentTypeName,
      String... segmentRules) {
    // Reload campaign to ensure it's attached and we can fetch lazy collections
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

    CampaignState state = campaign.getState();
    Wrestler player = campaign.getWrestler();

    // Initialize lazy collections to prevent LazyInitializationException
    player.getReigns().size();

    // 1. Find/Create Campaign Show
    Show show = getOrCreateCampaignShow(campaign);

    // Finale Logic Override
    CampaignChapterDTO chapter = getCurrentChapter(campaign);
    boolean isFinalsPhase = getFeatureValue(state, KEY_FINALS_PHASE, Boolean.class, false);
    boolean isNarrativeFinale = isFinalsPhase && !chapter.isTournament();

    String actualTypeName = segmentTypeName;
    List<String> actualRules =
        segmentRules != null
            ? new ArrayList<>(java.util.Arrays.asList(segmentRules))
            : new ArrayList<>();

    if (isNarrativeFinale) {
      if (chapter.getRules().getFinalMatchType() != null) {
        actualTypeName = chapter.getRules().getFinalMatchType();
      }
      if (chapter.getRules().getFinalMatchRules() != null) {
        actualRules.clear();
        actualRules.addAll(chapter.getRules().getFinalMatchRules());
      }
      narration = "CHAPTER FINALE: " + narration; // Optional flavor
    }

    // 3. Create Segment
    String finalTypeName = actualTypeName != null ? actualTypeName : "One on One";
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

    if ("fighting_champion".equals(chapter.getId())) {
      segment.setIsTitleSegment(true);
      player.getReigns().stream()
          .filter(TitleReign::isCurrentReign)
          .map(TitleReign::getTitle)
          .forEach(segment.getTitles()::add);
    }

    // Add Segment Rules
    if (actualRules.isEmpty()) {
      segmentRuleRepository.findByName("Normal").ifPresent(segment::addSegmentRule);
    } else {
      actualRules.forEach(
          rule -> segmentRuleRepository.findByName(rule).ifPresent(segment::addSegmentRule));
    }
    segmentRepository.save(segment);

    // 4. Assign Participants
    Wrestler opponent =
        wrestlerRepository
            .findByName(opponentName)
            .orElseThrow(() -> new IllegalArgumentException("Opponent not found: " + opponentName));

    addParticipant(segment, player);

    // Tag Team Logic
    if ("Tag Team".equalsIgnoreCase(type.getName())) {
      // Player Partner
      Long partnerId = getFeatureValue(state, KEY_PARTNER_ID, Long.class, null);
      if (partnerId != null) {
        wrestlerRepository.findById(partnerId).ifPresent(p -> addParticipant(segment, p));
      } else {
        // Let's assign a random partner for now if missing.
        List<Wrestler> freeAgents = wrestlerRepository.findAll(); // Optimization needed in future
        freeAgents.remove(player);
        freeAgents.remove(opponent);
        if (!freeAgents.isEmpty()) {
          addParticipant(segment, freeAgents.get(random.nextInt(freeAgents.size())));
        }
      }

      addParticipant(segment, opponent);

      // Opponent Partner
      Team oppTeam = teamRepository.findByWrestler(opponent).stream().findFirst().orElse(null);
      if (oppTeam != null) {
        Wrestler oppPartner =
            oppTeam.getWrestler1().equals(opponent)
                ? oppTeam.getWrestler2()
                : oppTeam.getWrestler1();
        addParticipant(segment, oppPartner);
      } else {
        // Random partner for opponent
        List<Wrestler> freeAgents = wrestlerRepository.findAll();
        freeAgents.remove(player);
        freeAgents.remove(opponent);
        if (partnerId != null) {
          Long finalPartnerId = partnerId;
          freeAgents.removeIf(w -> w.getId().equals(finalPartnerId));
        }
        // Filter out participants already added
        // Better: get all potential, remove existing participants.

        if (!freeAgents.isEmpty()) {
          addParticipant(segment, freeAgents.get(random.nextInt(freeAgents.size())));
        }
      }
    } else {
      addParticipant(segment, opponent);
    }

    state.setCurrentMatch(segment);
    state.setCurrentPhase(CampaignPhase.MATCH);
    campaignStateRepository.save(state);

    return segment;
  }

  private void addParticipant(@NonNull Segment segment, @NonNull Wrestler wrestler) {
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
  public void shiftAlignment(@NonNull Campaign campaign, int amount) {
    if (amount == 0) {
      return;
    }

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

  public CampaignChapterDTO getCurrentChapter(@NonNull Campaign campaign) {
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
  public boolean hasActiveCampaign(@NonNull Wrestler wrestler) {
    return campaignRepository.findActiveByWrestler(wrestler).isPresent();
  }

  public void processMatchResult(@NonNull Campaign campaignParam, boolean won) {
    // Reload campaign to ensure it's attached and we can fetch lazy collections
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

    CampaignState state = campaign.getState();
    Wrestler wrestler = campaign.getWrestler();

    // Initialize lazy collections to prevent LazyInitializationException in ChapterService
    wrestler.getReigns().size();

    CampaignChapterDTO.ChapterRules rules = getCurrentChapter(campaign).getRules();
    CampaignChapterDTO currentChapter = getCurrentChapter(campaign);

    // Update Segment if it exists
    if (state.getCurrentMatch() != null) {
      Segment match = state.getCurrentMatch();
      List<Wrestler> winners = new ArrayList<>();
      if (won) {
        winners.add(wrestler);
      } else {
        // If lost, add all other participants as winners
        match.getWrestlers().stream().filter(w -> !w.equals(wrestler)).forEach(winners::add);
      }
      match.setWinners(winners);

      // Determine difficulty multiplier
      double multiplier = 1.0;
      if (currentChapter.getDifficulty() != null) {
        switch (currentChapter.getDifficulty()) {
          case LEGENDARY:
            multiplier = 2.0;
            break;
          case HARD:
            multiplier = 1.5;
            break;
          case EASY:
            multiplier = 0.8;
            break;
          default:
        }
      }

      final double finalMultiplier = multiplier;
      final Segment finalMatch = match;
      // Apply rewards directly for Campaign
      GeneralSecurityUtils.runAsAdmin(
          (java.util.function.Supplier<Void>)
              () -> {
                adjudicationService.adjudicateMatch(finalMatch, finalMultiplier);
                return null;
              });
      match.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
      segmentRepository.save(match);
    }

    state.setMatchesPlayed(state.getMatchesPlayed() + 1);
    int previousVP = state.getVictoryPoints();
    if (won) {
      state.setWins(state.getWins() + 1);
      state.setVictoryPoints(state.getVictoryPoints() + rules.getVictoryPointsWin());
      log.debug(
          "Match Won. VP Change: {} + {} = {}",
          previousVP,
          rules.getVictoryPointsWin(),
          state.getVictoryPoints());
    } else {
      state.setLosses(state.getLosses() + 1);
      state.setVictoryPoints(state.getVictoryPoints() + rules.getVictoryPointsLoss());
      log.debug(
          "Match Lost. VP Change: {} + {} = {}",
          previousVP,
          rules.getVictoryPointsLoss(),
          state.getVictoryPoints());
    }

    // Check for chapter completion/tournament qualification
    if (currentChapter.isTournament()) {
      boolean isFinalsPhase = getFeatureValue(state, KEY_FINALS_PHASE, Boolean.class, false);

      // Ensure tournament is initialized (handle legacy saves or missed init)
      if (!isFinalsPhase || tournamentService.getTournamentState(campaign) == null) {
        setFeatureValue(state, KEY_FINALS_PHASE, true);
        tournamentService.initializeTournament(campaign);
      }

      // Finals Phase (Tournament Bracket)
      Show currentShow = state.getCurrentMatch().getShow();
      tournamentService.advanceTournament(campaign, won, currentShow);

      if (!won) {
        log.info("Wrestler {} ELIMINATED from the tournament finals.", wrestler.getName());

        // Simulate rest of tournament
        TournamentDTO tournament = tournamentService.getTournamentState(campaign);
        while (tournament != null && tournament.getCurrentRound() <= tournament.getTotalRounds()) {
          log.info("Simulating round {}...", tournament.getCurrentRound());
          // Player effectively lost/eliminated, so passing 'false' handles their match if any
          // (redundant check but safe)
          // For purely NPC rounds, 'won' param is ignored by advanceTournament logic for player
          // match
          tournamentService.advanceTournament(campaign, false, currentShow);
          // Refresh state
          tournament = tournamentService.getTournamentState(campaign);
        }
      }

      // Check for Champion (Player or NPC)
      if (tournamentService.isPlayerChampion(campaign)) {
        log.info("Wrestler {} WON the tournament finals!", wrestler.getName());
        setFeatureValue(state, KEY_TOURNAMENT_WINNER, true);
        awardTitleToWinner(wrestler.getId());
      } else {
        // Find NPC winner
        TournamentDTO tournament = tournamentService.getTournamentState(campaign);
        if (tournament != null) {
          TournamentDTO.TournamentMatch finals =
              tournament.getMatches().stream()
                  .filter(m -> m.getRound() == tournament.getTotalRounds())
                  .findFirst()
                  .orElse(null);

          if (finals != null && finals.getWinnerId() != null) {
            log.info("Tournament won by wrestler ID: {}", finals.getWinnerId());
            awardTitleToWinner(finals.getWinnerId());
          }
        }
      }
    } else if (currentChapter.getRules() != null
        && currentChapter.getRules().getFinaleTriggerVP() != null) {
      // Narrative Finale Logic
      boolean isFinalsPhase = getFeatureValue(state, KEY_FINALS_PHASE, Boolean.class, false);
      boolean hasWonFinale = getFeatureValue(state, KEY_WON_FINALE, Boolean.class, false);

      if (!isFinalsPhase
          && !hasWonFinale
          && state.getVictoryPoints() >= currentChapter.getRules().getFinaleTriggerVP()) {
        log.info("Triggering Finale Phase for chapter {}", currentChapter.getId());
        setFeatureValue(state, KEY_FINALS_PHASE, true);
      } else if (isFinalsPhase) {
        if (won) {
          log.info("Wrestler {} WON the chapter finale!", wrestler.getName());
          setFeatureValue(state, KEY_WON_FINALE, true);
          setFeatureValue(state, KEY_FINALS_PHASE, false); // Reset phase
        } else {
          log.info("Wrestler {} LOST the chapter finale. Retry needed.", wrestler.getName());
          // Optionally reduce VP to force re-trigger? Or just keep in finalsPhase?
          // If we keep in finalsPhase, next match is also Finale.
          // If we want to punish, maybe reduce VP below threshold?
          // Rules say "victoryPointsLoss: -2".
          // If 12 -> 10.
          // Next loop: 10 < 12. So finalsPhase NOT set (if we rely on check).
          // But we check !state.isFinalsPhase().
          // If I am ALREADY in finalsPhase, I stay there unless I set it false.
          // So if lost, stay in finalsPhase?
          // User: "Retry needed".
          // So stay in finalsPhase.
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
        log.debug("Wrestler {} unlocked Promo action (Face alignment).", wrestler.getName());
      } else if (alignment.getAlignmentType() == AlignmentType.HEEL) {
        state.setPromoUnlocked(true);
        state.setAttackUnlocked(true);
        log.debug(
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
  public void completePostMatch(@NonNull Campaign campaignParam) {
    // Reload campaign to ensure attached entity and fresh state
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

    CampaignState state = campaign.getState();
    state.setCurrentPhase(CampaignPhase.BACKSTAGE);
    state.setActionsTaken(0); // Reset actions for the next "turn"
    state.setCurrentMatch(null); // Clear the match reference now that post-match is done
    state.setMomentumBonus(0); // Reset momentum bonus for the next "turn"

    // Advance game date by 1 day
    if (state.getCurrentGameDate() == null) {
      state.setCurrentGameDate(java.time.LocalDate.now());
    }
    state.setCurrentGameDate(state.getCurrentGameDate().plusDays(1));

    campaignStateRepository.save(state);
  }

  public Show getOrCreateCampaignShow(@NonNull Campaign campaign) {
    CampaignState state = campaign.getState();
    Wrestler player = campaign.getWrestler();
    LocalDate date = state.getCurrentGameDate();
    if (date == null) {
      date = LocalDate.now();
      state.setCurrentGameDate(date);
    }

    // Find/Create Campaign Season
    Season season =
        seasonRepository
            .findByName("Campaign Mode")
            .orElseGet(
                () -> {
                  Season s = new Season();
                  s.setName("Campaign Mode");
                  s.setStartDate(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC));
                  s.setEndDate(
                      LocalDate.now().plusYears(1).atStartOfDay().toInstant(ZoneOffset.UTC));
                  return seasonRepository.save(s);
                });

    // Determine show name and template
    String showName;
    Long templateId = null;
    if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
      showName = "Continuum";
      templateId =
          showTemplateRepository.findByName("Continuum").map(ShowTemplate::getId).orElse(null);
    } else {
      showName = "Campaign: " + player.getName() + " - " + date;
    }

    final String finalShowName = showName;
    final Long finalTemplateId = templateId;
    final java.time.LocalDate finalDate = date;
    return showService.findByName(finalShowName).stream()
        .filter(s -> s.getShowDate().equals(finalDate))
        .findFirst()
        .orElseGet(
            () -> {
              ShowType weekly =
                  showTypeRepository
                      .findByName("Weekly")
                      .orElseGet(() -> showTypeRepository.findAll().get(0));

              return showService.createShow(
                  finalShowName,
                  "Story matches for " + player.getName(),
                  weekly.getId(),
                  finalDate,
                  season.getId(),
                  finalTemplateId,
                  null);
            });
  }

  public SegmentType getPromoSegmentType() {
    return segmentTypeRepository
        .findByName("Promo")
        .orElseGet(
            () ->
                segmentTypeRepository
                    .findByName("One on One")
                    .orElseGet(() -> segmentTypeRepository.findAll().get(0)));
  }

  public void saveSegment(@NonNull Segment segment) {
    segmentRepository.save(segment);
  }

  public Optional<String> advanceChapter(@NonNull Campaign campaignParam) {
    // Reload to ensure attached entity and initialize lazy collections
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

    CampaignState state = campaign.getState();
    String oldId = state.getCurrentChapterId();

    // Initialize lazy collections accessed by criteria checks (e.g. isChampion checks reigns)
    campaign.getWrestler().getReigns().size();

    if (oldId != null) {
      state.getCompletedChapterIds().add(oldId);
    }

    List<CampaignChapterDTO> available = chapterService.findAvailableChapters(state);
    if (!available.isEmpty()) {
      // Pick next chapter - could be enhanced with choosing logic
      CampaignChapterDTO nextChapter = available.get(0);
      String newChapterId = nextChapter.getId();
      state.setCurrentChapterId(newChapterId);
      state.setMatchesPlayed(0); // Reset chapter-specific counters
      state.setWins(0);
      state.setLosses(0);

      // Initialize Tournament Opponents if entering a tournament chapter
      if (nextChapter.isTournament()) {
        setFeatureValue(state, KEY_FINALS_PHASE, true); // Skip qualifying, go straight to bracket
        tournamentService.initializeTournament(campaign);
      } else {
        setFeatureValue(state, KEY_FINALS_PHASE, false);
      }

      if (nextChapter.isTagTeam()) {
        initializeTagTeamChapter(campaign);
      }

      campaignStateRepository.save(state);
      log.info("Advanced to chapter: {}", state.getCurrentChapterId());
      return Optional.of(newChapterId);
    } else {
      completeCampaign(campaign);
      return Optional.empty();
    }
  }

  private void initializeTagTeamChapter(@NonNull Campaign campaign) {
    // 1. Ensure Tag Team Champions exist
    titleRepository
        .findByName("ATW Tag Team")
        .ifPresent(
            title -> {
              if (title.isVacant()) {
                List<Team> teams = teamRepository.findAll();
                // Filter teams that do NOT include the player
                teams.removeIf(
                    t ->
                        t.getWrestler1().equals(campaign.getWrestler())
                            || t.getWrestler2().equals(campaign.getWrestler()));

                if (!teams.isEmpty()) {
                  Team newChamps = teams.get(random.nextInt(teams.size()));
                  List<Wrestler> champs =
                      List.of(newChamps.getWrestler1(), newChamps.getWrestler2());
                  titleService.awardTitleTo(title, champs);
                  log.info(
                      "Initialized Tag Team Chapter: Awarded vacant title to {}",
                      newChamps.getName());
                }
              }
            });

    // 2. Reset Partner ID (User needs to find one)
    setFeatureValue(campaign.getState(), KEY_PARTNER_ID, null);
    setFeatureValue(campaign.getState(), KEY_RECRUITING_PARTNER, true);
  }

  public void completeCampaign(@NonNull Campaign campaign) {
    campaign.setStatus(CampaignStatus.COMPLETED);
    campaign.setEndedAt(LocalDateTime.now());
    campaignRepository.save(campaign);
  }

  @Transactional(readOnly = true)
  public boolean isChapterComplete(@NonNull Campaign campaignParam) {
    // Reload to ensure attached entity and initialize lazy collections
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

    // Initialize lazy collections accessed by criteria checks (e.g. isChampion checks reigns)
    campaign.getWrestler().getReigns().size();

    return chapterService.isChapterComplete(campaign.getState());
  }

  /**
   * Updates the wrestler's available ability cards based on their alignment and level. If the
   * wrestler has turned (changed alignment), all current cards are removed and the system resets
   * card inventory based on the current level.
   *
   * @param campaign The campaign to update.
   */
  public void updateAbilityCards(@NonNull Campaign campaign) {
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

  private void recalculatePendingPicks(
      @NonNull CampaignState state, @NonNull WrestlerAlignment alignment) {
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
  public void handleLevelChange(@NonNull Campaign campaign, int oldLevel, int newLevel) {
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

  private void removeOneCardOfLevel(@NonNull List<CampaignAbilityCard> cards, int level) {
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
  public List<CampaignAbilityCard> getPickableCards(@NonNull Campaign campaign) {
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

  private List<CampaignAbilityCard> getAvailableCards(@NonNull AlignmentType type, int level) {
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

  private void awardTitleToWinner(Long winnerId) {
    Wrestler winner =
        wrestlerRepository
            .findById(winnerId)
            .orElseThrow(() -> new IllegalStateException("Winner not found: " + winnerId));

    Title title =
        titleRepository
            .findByName("ATW World")
            .orElseThrow(() -> new IllegalStateException("ATW World Championship not found"));

    // End current active reign if exists
    titleReignRepository
        .findByTitleAndEndDateIsNull(title)
        .forEach(
            reign -> {
              reign.setEndDate(java.time.Instant.now());
              titleReignRepository.save(reign);
            });

    // Create new reign
    TitleReign newReign = new TitleReign();
    newReign.setTitle(title);
    newReign.getChampions().add(winner);
    newReign.setStartDate(java.time.Instant.now());
    titleReignRepository.save(newReign);

    log.info("Awarded {} to {}", title.getName(), winner.getName());
  }
}

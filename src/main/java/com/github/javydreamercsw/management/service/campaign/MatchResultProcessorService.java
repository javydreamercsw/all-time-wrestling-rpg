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

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.SegmentParticipantRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeNames;
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
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles match/segment lifecycle for campaigns: creating encounters, processing results, and
 * managing post-match transitions. Extracted from {@link CampaignService} to reduce its scope.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class MatchResultProcessorService {

  private final CampaignRepository campaignRepository;
  private final CampaignStateRepository campaignStateRepository;
  private final WrestlerAlignmentRepository wrestlerAlignmentRepository;
  private final WrestlerRepository wrestlerRepository;
  private final ShowRepository showRepository;
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
  private final NewsGenerationService newsGenerationService;
  private final StorylineDirectorService storylineDirectorService;
  private final WrestlerStatusService wrestlerStatusService;
  private final FeatureDataService featureDataService;

  // Field-injected with @Lazy to break the circular dependency with CampaignService
  @org.springframework.beans.factory.annotation.Autowired
  @org.springframework.context.annotation.Lazy
  private CampaignService campaignService;

  private final Random random = new Random();

  private static final java.util.Set<String> PROMO_CAMPAIGN_RULES =
      java.util.Set.of("Faction Beatdown", "GM Office Confrontation", "Performance Review");

  private static final String KEY_FINALS_PHASE = "finalsPhase";
  private static final String KEY_TOURNAMENT_WINNER = "tournamentWinner";
  private static final String KEY_WON_FINALE = "wonFinale";
  private static final String KEY_PARTNER_ID = "partnerId";
  private static final String KEY_RECRUITING_PARTNER = "recruitingPartner";

  public Segment createMatchForEncounter(
      @NonNull Campaign campaignParam,
      @NonNull String opponentName,
      @NonNull String narration,
      @NonNull String segmentTypeName,
      String... segmentRules) {
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

    CampaignState state = campaign.getState();
    Wrestler player = campaign.getWrestler();
    player.getReigns().size();

    Show show = getOrCreateCampaignShow(campaign);

    CampaignChapterDTO chapter =
        campaignService
            .getCurrentChapter(campaign)
            .orElseThrow(() -> new IllegalStateException("No active chapter for match encounter"));
    boolean isFinalsPhase =
        featureDataService.getFeatureValue(state, KEY_FINALS_PHASE, Boolean.class, false);
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
      narration = "CHAPTER FINALE: " + narration;
    }

    if (PROMO_CAMPAIGN_RULES.stream().anyMatch(actualRules::contains)) {
      actualTypeName = SegmentTypeNames.PROMO;
    }
    String finalTypeName = actualTypeName != null ? actualTypeName : SegmentTypeNames.ONE_ON_ONE;
    SegmentType type =
        segmentTypeRepository
            .findByName(finalTypeName)
            .orElseGet(
                () ->
                    segmentTypeRepository
                        .findByName(SegmentTypeNames.ONE_ON_ONE)
                        .orElseGet(() -> segmentTypeRepository.findAll().get(0)));

    Segment newSegment = new Segment();
    newSegment.setShow(show);
    newSegment.setSegmentType(type);
    newSegment.setSegmentDate(java.time.Instant.now());
    newSegment.setIsTitleSegment(false);
    newSegment.setTitles(new java.util.HashSet<>());
    final Segment segment = segmentRepository.save(newSegment);
    segment.setNarration(narration);

    if ("fighting_champion".equals(chapter.getId())) {
      segment.setIsTitleSegment(true);
      player.getReigns().stream()
          .filter(TitleReign::isCurrentReign)
          .map(TitleReign::getTitle)
          .forEach(segment.getTitles()::add);
    }

    if (actualRules.isEmpty()) {
      segmentRuleRepository.findByName("Normal").ifPresent(segment::addSegmentRule);
    } else {
      actualRules.forEach(
          rule -> segmentRuleRepository.findByName(rule).ifPresent(segment::addSegmentRule));
    }
    segmentRepository.save(segment);

    Wrestler opponent =
        wrestlerRepository
            .findByName(opponentName)
            .orElseThrow(() -> new IllegalArgumentException("Opponent not found: " + opponentName));

    addParticipant(segment, player);

    if (SegmentTypeNames.TAG_TEAM.equalsIgnoreCase(type.getName())) {
      Long partnerId = featureDataService.getFeatureValue(state, KEY_PARTNER_ID, Long.class, null);
      if (partnerId != null) {
        wrestlerRepository.findById(partnerId).ifPresent(p -> addParticipant(segment, p));
      } else {
        List<Wrestler> freeAgents = freeAgents(player, opponent);
        if (!freeAgents.isEmpty()) {
          addParticipant(segment, freeAgents.get(random.nextInt(freeAgents.size())));
        }
      }

      addParticipant(segment, opponent);

      Team oppTeam = teamRepository.findByWrestler(opponent).stream().findFirst().orElse(null);
      if (oppTeam != null) {
        Wrestler oppPartner =
            oppTeam.getWrestler1().equals(opponent)
                ? oppTeam.getWrestler2()
                : oppTeam.getWrestler1();
        addParticipant(segment, oppPartner);
      } else {
        List<Wrestler> freeAgents = freeAgents(player, opponent);
        if (partnerId != null) {
          Long finalPartnerId = partnerId;
          freeAgents.removeIf(w -> w.getId().equals(finalPartnerId));
        }
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

  public void processMatchResult(@NonNull final Campaign campaignParam, final boolean won) {
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

    CampaignState state = campaign.getState();
    Wrestler wrestler = campaign.getWrestler();
    wrestler.getReigns().size();

    CampaignChapterDTO currentChapter =
        campaignService
            .getCurrentChapter(campaign)
            .orElseThrow(() -> new IllegalStateException("No active chapter for match result"));
    CampaignChapterDTO.ChapterRules rules = currentChapter.getRules();

    if (state.getCurrentMatch() != null) {
      Segment match = state.getCurrentMatch();
      List<Wrestler> winners = new ArrayList<>();
      if (won) {
        winners.add(wrestler);
      } else {
        match.getWrestlers().stream().filter(w -> !w.equals(wrestler)).forEach(winners::add);
      }
      match.setWinners(winners);

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
      GeneralSecurityUtils.runAsAdmin(
          (java.util.function.Supplier<Void>)
              () -> {
                adjudicationService.adjudicateMatchForCampaign(finalMatch, finalMultiplier);
                return null;
              });
      match.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
      segmentRepository.save(match);
      newsGenerationService.generateNewsForSegment(match);
    }

    int momentum = wrestler.getEffectiveStartingMomentum();
    wrestler
        .getStatuses()
        .forEach(status -> wrestlerStatusService.evaluateTriggerConditions(status, momentum, !won));

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

    if (currentChapter.isTournament()) {
      boolean isFinalsPhase =
          featureDataService.getFeatureValue(state, KEY_FINALS_PHASE, Boolean.class, false);

      if (!isFinalsPhase || tournamentService.getTournamentState(campaign) == null) {
        featureDataService.setFeatureValue(state, KEY_FINALS_PHASE, true);
        tournamentService.initializeTournament(campaign);
      }

      Show currentShow = state.getCurrentMatch().getShow();
      final boolean finalWon = won;
      final Show finalShow = currentShow;
      GeneralSecurityUtils.runAsAdmin(
          (java.util.function.Supplier<Void>)
              () -> {
                tournamentService.advanceTournament(campaign, finalWon, finalShow);
                return null;
              });

      if (!won) {
        log.info("Wrestler {} ELIMINATED from the tournament finals.", wrestler.getName());
        TournamentDTO tournament = tournamentService.getTournamentState(campaign);
        while (tournament != null && tournament.getCurrentRound() <= tournament.getTotalRounds()) {
          log.info("Simulating round {}...", tournament.getCurrentRound());
          GeneralSecurityUtils.runAsAdmin(
              (java.util.function.Supplier<Void>)
                  () -> {
                    tournamentService.advanceTournament(campaign, false, currentShow);
                    return null;
                  });
          tournament = tournamentService.getTournamentState(campaign);
        }
      }

      if (tournamentService.isPlayerChampion(campaign)) {
        log.info("Wrestler {} WON the tournament finals!", wrestler.getName());
        featureDataService.setFeatureValue(state, KEY_TOURNAMENT_WINNER, true);
        awardTitleToWinner(wrestler.getId());
      } else {
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
      boolean isFinalsPhase =
          featureDataService.getFeatureValue(state, KEY_FINALS_PHASE, Boolean.class, false);
      boolean hasWonFinale =
          featureDataService.getFeatureValue(state, KEY_WON_FINALE, Boolean.class, false);

      if (!isFinalsPhase
          && !hasWonFinale
          && state.getVictoryPoints() >= currentChapter.getRules().getFinaleTriggerVP()) {
        log.info("Triggering Finale Phase for chapter {}", currentChapter.getId());
        featureDataService.setFeatureValue(state, KEY_FINALS_PHASE, true);
      } else if (isFinalsPhase) {
        if (won) {
          log.info("Wrestler {} WON the chapter finale!", wrestler.getName());
          featureDataService.setFeatureValue(state, KEY_WON_FINALE, true);
          featureDataService.setFeatureValue(state, KEY_FINALS_PHASE, false);
        } else {
          log.info("Wrestler {} LOST the chapter finale. Retry needed.", wrestler.getName());
        }
      }
    }

    state.setCurrentPhase(CampaignPhase.POST_MATCH);
    storylineDirectorService.evaluateProgress(campaign, won);

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

    if (campaignService.isChapterComplete(campaign)) {
      log.debug("Chapter {} complete! Ready to advance.", state.getCurrentChapterId());
    }
  }

  public void completePostMatch(@NonNull final Campaign campaignParam) {
    Campaign campaign =
        campaignRepository
            .findById(campaignParam.getId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

    CampaignState state = campaign.getState();
    state.setCurrentPhase(CampaignPhase.BACKSTAGE);
    state.setActionsTaken(0);
    state.setCurrentMatch(null);
    state.setMomentumBonus(0);

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
    final LocalDate finalDate = date;
    final Season finalSeason = season;
    return showRepository.findByName(finalShowName).stream()
        .filter(s -> s.getShowDate().equals(finalDate))
        .findFirst()
        .orElseGet(
            () -> {
              ShowType weekly =
                  showTypeRepository
                      .findByName("Weekly")
                      .orElseGet(
                          () -> {
                            var all = showTypeRepository.findAll();
                            if (!all.isEmpty()) {
                              return all.get(0);
                            }
                            ShowType st = new ShowType();
                            st.setName("Weekly");
                            st.setDescription("Default Weekly Show");
                            return showTypeRepository.save(st);
                          });

              Show show = new Show();
              show.setName(finalShowName);
              show.setDescription("Story matches for " + player.getName());
              show.setShowDate(finalDate);
              show.setCreationDate(java.time.Instant.now());
              show.setType(weekly);
              show.setSeason(finalSeason);
              if (finalTemplateId != null) {
                showTemplateRepository.findById(finalTemplateId).ifPresent(show::setTemplate);
              }
              return showRepository.saveAndFlush(show);
            });
  }

  public SegmentType getPromoSegmentType() {
    return segmentTypeRepository
        .findByName(SegmentTypeNames.PROMO)
        .orElseGet(
            () ->
                segmentTypeRepository
                    .findByName(SegmentTypeNames.ONE_ON_ONE)
                    .orElseGet(
                        () -> {
                          var all = segmentTypeRepository.findAll();
                          if (!all.isEmpty()) {
                            return all.get(0);
                          }
                          SegmentType st = new SegmentType();
                          st.setName(SegmentTypeNames.PROMO);
                          st.setDescription("Standard Promo");
                          return segmentTypeRepository.save(st);
                        }));
  }

  public void saveSegment(@NonNull final Segment segment) {
    segmentRepository.save(segment);
  }

  private List<Wrestler> freeAgents(final Wrestler... excluded) {
    List<Wrestler> free = new ArrayList<>(wrestlerRepository.findAll());
    for (Wrestler w : excluded) {
      if (w != null) {
        free.remove(w);
      }
    }
    return free;
  }

  private void addParticipant(@NonNull final Segment segment, @NonNull final Wrestler wrestler) {
    SegmentParticipant participant = new SegmentParticipant();
    participant.setSegment(segment);
    participant.setWrestler(wrestler);
    participant.setIsWinner(false);
    participantRepository.save(participant);
  }

  private void awardTitleToWinner(final Long winnerId) {
    Wrestler winner =
        wrestlerRepository
            .findById(winnerId)
            .orElseThrow(() -> new IllegalStateException("Winner not found: " + winnerId));

    Title title =
        titleRepository
            .findByName("ATW World")
            .orElseThrow(() -> new IllegalStateException("ATW World Championship not found"));

    titleReignRepository
        .findByTitleAndEndDateIsNull(title)
        .forEach(
            reign -> {
              reign.setEndDate(java.time.Instant.now());
              titleReignRepository.save(reign);
            });

    TitleReign newReign = new TitleReign();
    newReign.setTitle(title);
    newReign.getChampions().add(winner);
    newReign.setStartDate(java.time.Instant.now());
    titleReignRepository.save(newReign);

    log.info("Awarded {} to {}", title.getName(), winner.getName());
  }
}

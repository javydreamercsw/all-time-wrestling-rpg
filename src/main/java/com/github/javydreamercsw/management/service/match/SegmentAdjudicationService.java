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
package com.github.javydreamercsw.management.service.match;

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeNames;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.campaign.WrestlerStatusService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.ringside.RingsideAiService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.RetirementService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SegmentAdjudicationService {

  private final RivalryService rivalryService;
  private final WrestlerService wrestlerService;
  private final FeudResolutionService feudResolutionService;
  private final MultiWrestlerFeudService feudService;
  private final Random random;
  private final TitleService titleService;
  private final MatchFulfillmentRepository matchFulfillmentRepository;
  private final LeagueRosterRepository leagueRosterRepository;
  private final LeagueRepository leagueRepository;
  private final LegacyService legacyService;
  private final FactionService factionService;
  private final RingsideActionService ringsideActionService;
  private final RingsideAiService ringsideAiService;
  private final RetirementService retirementService;
  final GameSettingService gameSettingService;
  private final WrestlerRelationshipService relationshipService;
  private final WrestlerStatusService wrestlerStatusService;
  private final UniverseContextService universeContextService;
  @Autowired private ApplicationEventPublisher eventPublisher;

  @Setter(onMethod_ = {@Autowired, @org.springframework.context.annotation.Lazy})
  private ShowService showService;

  // Field-injected to avoid changing the constructor (unit tests build this service manually).
  // Null-safe: when null (unit tests), reattach() is a no-op.
  @Setter(onMethod_ = {@Autowired})
  private SegmentRepository segmentRepository;

  // Field-injected for the same reason — null-safe, falls back to wrestler.getAlignment().
  @Setter(onMethod_ = {@Autowired})
  private AlignmentService alignmentService;

  @Setter(onMethod_ = {@Autowired})
  private OutcomeMatrixService outcomeMatrixService;

  @Autowired
  public SegmentAdjudicationService(
      final RivalryService rivalryService,
      final WrestlerService wrestlerService,
      final FeudResolutionService feudResolutionService,
      final MultiWrestlerFeudService feudService,
      final TitleService titleService,
      final MatchFulfillmentRepository matchFulfillmentRepository,
      final LeagueRepository leagueRepository,
      final LeagueRosterRepository leagueRosterRepository,
      final LegacyService legacyService,
      final FactionService factionService,
      final RingsideActionService ringsideActionService,
      final RingsideAiService ringsideAiService,
      final RetirementService retirementService,
      final GameSettingService gameSettingService,
      final WrestlerRelationshipService relationshipService,
      final WrestlerStatusService wrestlerStatusService,
      final UniverseContextService universeContextService) {
    this(
        rivalryService,
        wrestlerService,
        feudResolutionService,
        feudService,
        titleService,
        matchFulfillmentRepository,
        leagueRepository,
        leagueRosterRepository,
        legacyService,
        factionService,
        ringsideActionService,
        ringsideAiService,
        retirementService,
        gameSettingService,
        relationshipService,
        wrestlerStatusService,
        universeContextService,
        new Random());
  }

  public SegmentAdjudicationService(
      final RivalryService rivalryService,
      final WrestlerService wrestlerService,
      final FeudResolutionService feudResolutionService,
      final MultiWrestlerFeudService feudService,
      final TitleService titleService,
      final MatchFulfillmentRepository matchFulfillmentRepository,
      final LeagueRepository leagueRepository,
      final LeagueRosterRepository leagueRosterRepository,
      final LegacyService legacyService,
      final FactionService factionService,
      final RingsideActionService ringsideActionService,
      final RingsideAiService ringsideAiService,
      final RetirementService retirementService,
      final GameSettingService gameSettingService,
      final WrestlerRelationshipService relationshipService,
      final WrestlerStatusService wrestlerStatusService,
      final UniverseContextService universeContextService,
      final Random random) {
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;
    this.feudResolutionService = feudResolutionService;
    this.feudService = feudService;
    this.titleService = titleService;
    this.matchFulfillmentRepository = matchFulfillmentRepository;
    this.leagueRepository = leagueRepository;
    this.leagueRosterRepository = leagueRosterRepository;
    this.legacyService = legacyService;
    this.factionService = factionService;
    this.ringsideActionService = ringsideActionService;
    this.ringsideAiService = ringsideAiService;
    this.retirementService = retirementService;
    this.gameSettingService = gameSettingService;
    this.relationshipService = relationshipService;
    this.wrestlerStatusService = wrestlerStatusService;
    this.universeContextService = universeContextService;
    this.random = random;
  }

  /**
   * Adjudicates a segment by ID. Preferred entry point from UI callers (e.g. MatchView) that hold a
   * detached {@link Segment} between Vaadin push events — passing the ID instead of the entity
   * avoids {@code LazyInitializationException} on any lazy proxy (show, wrestlers, statuses, …).
   * The segment is loaded fresh inside this {@code @Transactional} boundary.
   */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @Transactional
  public void adjudicateMatch(@NonNull final Long segmentId) {
    Segment segment =
        segmentRepository
            .findByIdWithDetails(segmentId)
            .orElseThrow(() -> new IllegalArgumentException("Segment not found: " + segmentId));
    adjudicateMatchInternal(segment, 1.0);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void adjudicateMatch(@NonNull final Segment segment) {
    adjudicateMatch(segment, 1.0);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @Transactional
  public void adjudicateMatch(@NonNull final Segment segment, final double multiplier) {
    adjudicateMatchInternal(segment, multiplier);
  }

  /** Called by CampaignService where the player security context cannot be elevated. */
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER', 'PLAYER')")
  @Transactional
  public void adjudicateMatchForCampaign(@NonNull final Segment segment, final double multiplier) {
    adjudicateMatchInternal(segment, multiplier);
  }

  @Transactional
  private void adjudicateMatchInternal(@NonNull final Segment segment, final double multiplier) {
    // Process match fulfillment first so that a player-reported winner is reflected
    // in the winners/losers lists used by every downstream method.
    matchFulfillmentRepository
        .findBySegment(segment)
        .ifPresent(
            fulfillment -> {
              if (fulfillment.getReportedWinner() != null && segment.getWinners().isEmpty()) {
                segment.setWinners(List.of(fulfillment.getReportedWinner()));
              }
              fulfillment.setStatus(MatchFulfillment.FulfillmentStatus.FINALIZED);
              matchFulfillmentRepository.save(fulfillment);
            });

    List<Wrestler> winners = segment.getWinners();
    List<Wrestler> losers = new ArrayList<>(segment.getWrestlers());
    losers.removeAll(winners);

    applyLeagueStats(segment, winners, losers);
    processRewards(segment, multiplier);
    applyOutcomeMatrix(segment, winners, losers);
    applyRatingAndNoise(segment);
    applyWearAndTear(segment);
    applyRingsideActions(segment);
    applyTitleChange(segment, winners, losers);

    Long universeId =
        segment.getShow().getUniverse() != null ? segment.getShow().getUniverse().getId() : 1L;

    applyFactionAffinity(segment, winners, universeId);
    applyHeatToRivaltiesAndFeuds(segment, segment.getWrestlers(), universeId);
    resolveRivalriesAndFeuds(segment, winners);
    triggerAchievements(segment, winners);

    if (showService != null) {
      showService.finalizeShowIfComplete(segment.getShow());
    }

    evaluateStatusCards(segment, winners, segment.getWrestlers());
  }

  private void applyLeagueStats(
      @NonNull final Segment segment,
      @NonNull final List<Wrestler> winners,
      @NonNull final List<Wrestler> losers) {
    // Update League Stats if applicable — check show.getLeague() first, then fall back to
    // the universe-based lookup for shows that were associated via universe rather than directly.
    com.github.javydreamercsw.management.domain.league.League effectiveLeague =
        segment.getShow().getLeague();
    if (effectiveLeague == null && segment.getShow().getUniverse() != null) {
      effectiveLeague =
          leagueRepository.findByUniverse(segment.getShow().getUniverse()).orElse(null);
    }
    if (effectiveLeague != null) {
      final com.github.javydreamercsw.management.domain.league.League league = effectiveLeague;
      Map<Long, LeagueRoster> rosterByWrestlerId =
          leagueRosterRepository.findByLeague(league).stream()
              .filter(r -> r.getWrestler() != null)
              .collect(Collectors.toMap(r -> r.getWrestler().getId(), r -> r));

      List<LeagueRoster> toSave = new ArrayList<>();
      if (winners.isEmpty()) {
        for (Wrestler w : segment.getWrestlers()) {
          LeagueRoster roster = rosterByWrestlerId.get(w.getId());
          if (roster != null) {
            roster.setDraws(roster.getDraws() + 1);
            toSave.add(roster);
          }
        }
      } else {
        for (Wrestler w : winners) {
          LeagueRoster roster = rosterByWrestlerId.get(w.getId());
          if (roster != null) {
            roster.setWins(roster.getWins() + 1);
            toSave.add(roster);
          }
        }
        for (Wrestler w : losers) {
          LeagueRoster roster = rosterByWrestlerId.get(w.getId());
          if (roster != null) {
            roster.setLosses(roster.getLosses() + 1);
            toSave.add(roster);
          }
        }
      }
      leagueRosterRepository.saveAll(toSave);
    }
  }

  private void applyOutcomeMatrix(
      @NonNull final Segment segment,
      @NonNull final List<Wrestler> winners,
      @NonNull final List<Wrestler> losers) {
    if (outcomeMatrixService == null || segment.getWrestlers().isEmpty()) {
      return;
    }
    OutcomeMatrixCategory category =
        SegmentTypeNames.PROMO.equals(segment.getSegmentType().getName())
            ? OutcomeMatrixCategory.PROMO
            : OutcomeMatrixCategory.MATCH_FLOW;
    Map<String, String> chartVars = new HashMap<>();
    Wrestler chartPrimary = winners.isEmpty() ? segment.getWrestlers().get(0) : winners.get(0);
    chartVars.put("{WRESTLER_1}", chartPrimary.getName());
    Wrestler chartOpponent = null;
    if (segment.getWrestlers().size() > 1) {
      // Pick the opponent: prefer the first loser, but if losers is empty or only contains the
      // primary (promo with no declared winner), fall back to the second wrestler in the list.
      chartOpponent =
          losers.stream()
              .filter(w -> !w.equals(chartPrimary))
              .findFirst()
              .orElse(segment.getWrestlers().get(1));
      chartVars.put("{WRESTLER_2}", chartOpponent.getName());
    }
    Long chartUniverseId =
        segment.getShow().getUniverse() != null
            ? segment.getShow().getUniverse().getId()
            : universeContextService.getCurrentUniverseId();
    if (chartUniverseId != null) {
      final Long chartPrimaryId = chartPrimary.getId();
      final Long chartSecondaryId = chartOpponent != null ? chartOpponent.getId() : null;
      final Long finalChartUniverseId = chartUniverseId;
      outcomeMatrixService
          .resolveRandomRoll(category, chartVars)
          .ifPresent(
              result ->
                  outcomeMatrixService.applyEffects(
                      result.entry(),
                      chartPrimaryId,
                      chartSecondaryId,
                      finalChartUniverseId,
                      result.renderedText()));
    }
  }

  private void applyRatingAndNoise(@NonNull final Segment segment) {
    // Update segment rating
    double chemistryBonus = relationshipService.calculateChemistryBonus(segment.getWrestlers());
    int baseRating = calculateBaseRating();
    int finalRating = (int) Math.min(100, baseRating * (1.0 + chemistryBonus));
    segment.setSegmentRating(finalRating);

    // Set crowd noise level (Rating with some random variance)
    int noiseVariance = random.nextInt(11) - 5; // -5 to +5
    segment.setCrowdNoiseLevel(Math.clamp(finalRating + noiseVariance, 0, 100));
  }

  private void applyRingsideActions(@NonNull final Segment segment) {
    // Automated Ringside Actions
    if (!SegmentTypeNames.PROMO.equals(segment.getSegmentType().getName())) {
      for (Wrestler w : segment.getWrestlers()) {
        Object supporter = ringsideActionService.getBestSupporter(segment, w);
        if (supporter != null) {
          ringsideAiService.evaluateRingsideAction(segment, supporter, w);
        }
      }
    }
  }

  private void applyTitleChange(
      @NonNull final Segment segment,
      @NonNull final List<Wrestler> winners,
      @NonNull final List<Wrestler> losers) {
    if (!SegmentTypeNames.PROMO.equals(segment.getSegmentType().getName())) {
      if (segment.getIsTitleSegment()) {
        for (Title title : segment.getTitles()) {
          List<Wrestler> currentChampions = title.getCurrentChampions();
          if (new HashSet<>(winners).containsAll(currentChampions)) {
            eventPublisher.publishEvent(
                new ChampionshipDefendedEvent(this, title, currentChampions, losers));
          } else {
            titleService.awardTitleTo(title, winners, segment);
            eventPublisher.publishEvent(
                new ChampionshipChangeEvent(this, title, currentChampions, winners));
          }
        }
      }
    }
  }

  private void applyFactionAffinity(
      @NonNull final Segment segment,
      @NonNull final List<Wrestler> winners,
      @NonNull final Long universeId) {
    // Faction Affinity Logic
    Map<Long, Integer> factionParticipants = new HashMap<>();
    Map<Long, Integer> factionWinners = new HashMap<>();

    for (Wrestler participant : segment.getWrestlers()) {
      Faction faction =
          participant
              .getState(universeId)
              .map(com.github.javydreamercsw.management.domain.wrestler.WrestlerState::getFaction)
              .orElse(null);
      if (faction != null) {
        Long factionId = faction.getId();
        factionParticipants.put(factionId, factionParticipants.getOrDefault(factionId, 0) + 1);
        if (winners.contains(participant)) {
          factionWinners.put(factionId, factionWinners.getOrDefault(factionId, 0) + 1);
        }
      }
    }

    // Reward factions for participation and victory
    factionParticipants.forEach(
        (factionId, count) -> {
          if (count > 1) {
            int affinityGain = count - 1; // Base participation gain

            // Victory bonus: +2 if multiple members from the same faction won
            if (factionWinners.getOrDefault(factionId, 0) > 1) {
              affinityGain += 2;
            }

            // Context multipliers
            if (segment.isMainEvent()) {
              affinityGain *= 2;
            }
            if (segment.getShow().isPremiumLiveEvent()) {
              affinityGain *= 2;
            }

            // Promo bonus: +1 for shared spotlight
            if (SegmentTypeNames.PROMO.equals(segment.getSegmentType().getName())) {
              affinityGain += 1;
            }

            if (affinityGain > 0) {
              factionService.addAffinity(factionId, affinityGain);
            }
          }
        });
  }

  private void applyHeatToRivaltiesAndFeuds(
      @NonNull final Segment segment,
      @NonNull final List<Wrestler> participants,
      @NonNull final Long universeId) {
    // If the AI tagged a specific rivalry, boost its heat directly before the generic pair-scan.
    if (segment.getRivalryId() != null) {
      int targetedHeat = segment.getShow().isPremiumLiveEvent() ? 3 : 2;
      rivalryService.addHeat(
          segment.getRivalryId(),
          targetedHeat,
          "AI-booked segment: " + segment.getSegmentType().getName());
      log.debug(
          "Added {} heat to rivalry {} from AI-booked segment {}",
          targetedHeat,
          segment.getRivalryId(),
          segment.getId());
    }

    // Add heat to rivalries
    String segmentTypeName = segment.getSegmentType().getName();
    boolean isPromo = SegmentTypeNames.PROMO.equals(segmentTypeName);
    boolean isAiTargeted = segment.getRivalryId() != null;
    final int heat = isPromo ? 4 : 1;

    // Skip all-pairs heat addition for Rumbles to avoid performance issues,
    // excessive rivalry creation, and because determining eliminations from
    // narration is complex. Bookers can manage these rivalries manually.
    if (!SegmentTypeNames.ABU_DHABI_RUMBLE.equals(segmentTypeName)) {
      for (int i = 0; i < participants.size(); i++) {
        for (int j = i + 1; j < participants.size(); j++) {
          Wrestler wi = participants.get(i);
          Wrestler wj = participants.get(j);
          Faction fi = wi.getState(universeId).map(WrestlerState::getFaction).orElse(null);
          Faction fj = wj.getState(universeId).map(WrestlerState::getFaction).orElse(null);
          if (fi != null && fi.equals(fj)) {
            log.debug(
                "Skipping heat between teammates {} and {} (faction: {})",
                wi.getName(),
                wj.getName(),
                fi.getName());
            continue;
          }
          if (isPromo || isAiTargeted) {
            // Promos and AI-targeted segments may create a new rivalry if none exists.
            rivalryService.addHeatBetweenWrestlers(
                wi.getId(), wj.getId(), heat, "From segment: " + segmentTypeName, universeId);
          } else {
            // Plain matches only add heat to an already-established rivalry; they do not
            // spawn new ones for every random pairing.
            rivalryService
                .getRivalryBetweenWrestlers(wi.getId(), wj.getId())
                .ifPresent(
                    r ->
                        rivalryService.addHeat(
                            r.getId(), heat, "From segment: " + segmentTypeName));
          }
        }
      }
    } else {
      log.info(
          "Skipping automatic rivalry processing for Rumble segment: {}",
          segment.getShow().getName());
    }

    // Add heat to feuds
    Set<MultiWrestlerFeud> feudsToUpdate = new HashSet<>();
    for (Wrestler participant : participants) {
      feudsToUpdate.addAll(feudService.getActiveFeudsForWrestler(participant.getId()));
    }

    for (MultiWrestlerFeud feud : feudsToUpdate) {
      List<Wrestler> feudParticipants = feud.getActiveWrestlers();
      long segmentParticipantsInFeud =
          participants.stream().filter(feudParticipants::contains).count();

      if (segmentParticipantsInFeud > 1) {
        feudService.addHeat(
            feud.getId(), heat, "From segment: " + segment.getSegmentType().getName());
      }
    }
  }

  private void resolveRivalriesAndFeuds(
      @NonNull final Segment segment, @NonNull final List<Wrestler> winners) {
    // Attempt to resolve feuds and rivalries after qualifying matches.
    // PLEs always trigger; regular shows only when the setting is enabled.
    boolean isPle = segment.getShow().isPremiumLiveEvent();
    boolean regularResolutionEnabled =
        gameSettingService.isRivalryResolutionOnRegularShowsEnabled();

    if (isPle || regularResolutionEnabled) {
      int threshold =
          isPle
              ? gameSettingService.getRivalryResolutionThresholdPle()
              : gameSettingService.getRivalryResolutionThresholdRegular();

      log.info(
          "Attempting rivalry/feud resolution after {} match: {} (threshold: {})",
          isPle ? "PLE" : "regular",
          segment.getShow().getName(),
          threshold);

      for (Wrestler wrestler : segment.getWrestlers()) {
        List<MultiWrestlerFeud> feuds = feudService.getActiveFeudsForWrestler(wrestler.getId());
        for (MultiWrestlerFeud feud : feuds) {
          feudResolutionService.attemptFeudResolution(feud);
        }
      }

      // If the AI tagged a specific rivalry, attempt resolution on it directly.
      if (segment.getRivalryId() != null) {
        DiceBag diceBag = new DiceBag(20);
        rivalryService.attemptResolution(
            segment.getRivalryId(), diceBag.roll(), diceBag.roll(), threshold);
        log.info(
            "Attempted resolution of AI-tagged rivalry {} after {} segment {}",
            segment.getRivalryId(),
            isPle ? "PLE" : "regular",
            segment.getId());
      } else {
        // Fall back to generic pair-scan when no rivalry was explicitly tagged.
        switch (segment.getSegmentType().getName()) {
          case SegmentTypeNames.TAG_TEAM:
            attemptRivalryResolution(
                segment.getWrestlers().get(0), segment.getWrestlers().get(2), threshold);
            attemptRivalryResolution(
                segment.getWrestlers().get(0), segment.getWrestlers().get(3), threshold);
            attemptRivalryResolution(
                segment.getWrestlers().get(1), segment.getWrestlers().get(2), threshold);
            attemptRivalryResolution(
                segment.getWrestlers().get(1), segment.getWrestlers().get(3), threshold);
            break;
          case SegmentTypeNames.ABU_DHABI_RUMBLE:
          case SegmentTypeNames.ONE_ON_ONE:
          case "Free-for-All":
          default:
            List<Wrestler> wrestlers = segment.getWrestlers();
            if (!wrestlers.isEmpty()) {
              Wrestler baseWrestler = winners.isEmpty() ? wrestlers.get(0) : winners.get(0);
              for (Wrestler other : wrestlers) {
                if (!baseWrestler.equals(other)) {
                  attemptRivalryResolution(baseWrestler, other, threshold);
                }
              }
            }
            break;
        }
      }
    }
  }

  private void triggerAchievements(
      @NonNull final Segment segment, @NonNull final List<Wrestler> winners) {
    // Trigger Achievements
    List<String> achievementKeys = new ArrayList<>();
    achievementKeys.add(segment.getSegmentType().getName());
    segment.getSegmentRules().forEach(rule -> achievementKeys.add(rule.getName()));

    for (Wrestler participant : segment.getWrestlers()) {
      if (participant.getAccount() != null) {
        for (String baseKey : achievementKeys) {
          String keySuffix =
              baseKey.toUpperCase().replaceAll("[^A-Z0-9 ]", "").trim().replace(" ", "_");
          legacyService.unlockAchievement(participant.getAccount(), "PARTICIPATE_" + keySuffix);
          if (winners.contains(participant)) {
            legacyService.unlockAchievement(participant.getAccount(), "WIN_" + keySuffix);
          }
        }

        if (winners.contains(participant)
            && SegmentTypeNames.ABU_DHABI_RUMBLE.equals(segment.getSegmentType().getName())) {
          legacyService.unlockAchievement(participant.getAccount(), "RUMBLE_WINNER");
        }

        if (segment.isMainEvent()) {
          legacyService.unlockAchievement(participant.getAccount(), "MAIN_EVENT");
          if (segment.getShow().isPremiumLiveEvent()) {
            legacyService.unlockAchievement(participant.getAccount(), "MAIN_EVENT_PLE");
          }
        }
      }
    }
  }

  private void evaluateStatusCards(
      @NonNull final Segment segment,
      @NonNull final List<Wrestler> winners,
      @NonNull final List<Wrestler> participants) {
    if (wrestlerStatusService.isStatusMechanicEnabled()) {
      Map<Long, Integer> recordedMomentum =
          segment.getParticipants().stream()
              .filter(p -> p.getFinalMomentum() != null)
              .collect(
                  Collectors.toMap(
                      p -> p.getWrestler().getId(), SegmentParticipant::getFinalMomentum));
      for (Wrestler participant : participants) {
        boolean lost = !winners.contains(participant);
        int finalMomentum =
            recordedMomentum.getOrDefault(
                participant.getId(), participant.getEffectiveStartingMomentum());
        participant
            .getStatuses()
            .forEach(
                status ->
                    wrestlerStatusService.evaluateTriggerConditions(status, finalMomentum, lost));
      }
    }
  }

  @Transactional
  public void processRewards(@NonNull final Segment segment, final double difficultyMultiplier) {
    List<Wrestler> winners = segment.getWinners();
    List<Wrestler> losers = new ArrayList<>(segment.getWrestlers());
    losers.removeAll(winners);

    DiceBag d20 = new DiceBag(random, new int[] {20});
    int roll = d20.roll();

    if (!SegmentTypeNames.PROMO.equals(segment.getSegmentType().getName())) {
      handleMatchRewards(segment, winners, losers, roll, difficultyMultiplier);
    } else {
      handlePromoRewards(segment, roll, difficultyMultiplier);
    }
  }

  private void handleMatchRewards(
      @NonNull final Segment segment,
      @NonNull final List<Wrestler> winners,
      @NonNull final List<Wrestler> losers,
      final int roll,
      final double difficultyMultiplier) {
    int matchQualityBonus = calculateMatchQualityBonus(segment, roll);

    Long universeId = universeContextService.getCurrentUniverseId();

    // Deduct fan fees for challengers in title segments
    if (segment.getIsTitleSegment() && !segment.getTitles().isEmpty()) {
      handleTitleContenderFees(segment);
    }

    // Award fans to winners
    for (Wrestler winner : winners) {
      if (winner.getId() != null) {
        DiceBag wdb = new DiceBag(random, new int[] {6, 6});
        // for winners 2d6 + 3 + (quality bonus) fans
        long baseAward = (wdb.roll() + 3) * 1_000L + matchQualityBonus;
        long finalAward = (long) (baseAward * difficultyMultiplier);

        // Apply Arena & Location Bonuses
        finalAward = applyVenueBonuses(segment, winner, finalAward);

        final long awardToGrant = finalAward;
        GeneralSecurityUtils.runAsAdmin(
                () -> wrestlerService.awardFans(winner.getId(), universeId, awardToGrant))
            .ifPresent(w -> log.debug("Awarded {} fans to winner {}", awardToGrant, w.getName()));
      }
    }

    // Award/deduct fans from losers
    for (Wrestler loser : losers) {
      if (loser.getId() != null) {
        DiceBag ldb = new DiceBag(random, new int[] {6});
        // for losers 1d6 - 4 + (quality bonus) fans. Can be negative
        long baseChange = (ldb.roll() - 4) * 1_000L + matchQualityBonus;
        long finalChange = (long) (baseChange * difficultyMultiplier);

        // Apply Arena & Location Bonuses (only if change is positive)
        if (finalChange > 0) {
          finalChange = applyVenueBonuses(segment, loser, finalChange);
        }

        final long changeToApply = finalChange;
        GeneralSecurityUtils.runAsAdmin(
                () -> wrestlerService.awardFans(loser.getId(), universeId, changeToApply))
            .ifPresent(
                w -> log.debug("Deducted/awarded {} fans to loser {}", changeToApply, w.getName()));
      }
    }

    assignBumps(segment, universeId);

    // Improve relationships between participants based on match quality
    if (roll >= 15) {
      relationshipService.improveGameplayRelationships(segment.getWrestlers(), roll >= 18 ? 2 : 1);
    }
  }

  private com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment resolveAlignment(
      final Wrestler wrestler, final Segment segment) {
    if (alignmentService != null && segment.getShow().getUniverse() != null) {
      return alignmentService.getOrCreateUniverseAlignment(
          wrestler, segment.getShow().getUniverse());
    }
    return wrestler.getAlignment();
  }

  private long applyVenueBonuses(
      final Segment segment, final Wrestler wrestler, final long amount) {
    double modifier = 1.0;

    // Arena Alignment Bias (+25%)
    if (segment.getShow().getArena() != null) {
      com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment effectiveAlignment =
          resolveAlignment(wrestler, segment);
      com.github.javydreamercsw.management.domain.world.Arena arena = segment.getShow().getArena();
      if (effectiveAlignment != null && effectiveAlignment.getAlignmentType() != null) {
        String wrestlerAlignment = effectiveAlignment.getAlignmentType().name();
        boolean matches = false;
        switch (arena.getAlignmentBias()) {
          case FACE_FAVORABLE -> matches = "FACE".equals(wrestlerAlignment);
          case HEEL_FAVORABLE -> matches = "HEEL".equals(wrestlerAlignment);
          case ANARCHIC -> matches = true; // Everyone gets a boost in anarchy
        }
        if (matches) {
          modifier += 0.25;
          log.debug(
              "Applying 25% Arena Bias bonus to {}. Arena: {}, Bias: {}",
              wrestler.getName(), arena.getName(), arena.getAlignmentBias());
        }
      }
    }

    // Home Territory Bonus (+10%)
    if (segment.getShow().getArena() != null
        && segment.getShow().getArena().getLocation() != null
        && wrestler.getHeritageTag() != null
        && !wrestler.getHeritageTag().isBlank()) {
      com.github.javydreamercsw.management.domain.world.Location location =
          segment.getShow().getArena().getLocation();
      String[] heritageTags = wrestler.getHeritageTag().toLowerCase().split(",");
      boolean matched = false;
      for (String heritage : heritageTags) {
        String trimmedHeritage = heritage.trim();
        if (trimmedHeritage.isEmpty()) {
          continue;
        }
        if (location.getName().toLowerCase().contains(trimmedHeritage)
            || location.getCulturalTags().stream()
                .anyMatch(tag -> tag.toLowerCase().contains(trimmedHeritage))) {
          matched = true;
          break;
        }
      }

      if (matched) {
        modifier += 0.10;
        log.info(
            "Applying 10% Home Territory bonus to {} in {}",
            wrestler.getName(), location.getName());
      }
    }

    return (long) (amount * modifier);
  }

  private void handlePromoRewards(
      @NonNull final Segment segment, final int roll, final double difficultyMultiplier) {
    int promoQualityBonus = calculatePromoQualityBonus(roll);

    // Assign fans to all participants
    for (Wrestler participant : segment.getWrestlers()) {
      if (participant.getId() != null) {
        long baseAward = promoQualityBonus * 1_000L;
        long finalAward = (long) (baseAward * difficultyMultiplier);

        // Apply Arena & Location Bonuses
        finalAward = applyVenueBonuses(segment, participant, finalAward);

        final long awardToGrant = finalAward;
        Long universeId =
            segment.getShow().getUniverse() != null ? segment.getShow().getUniverse().getId() : 1L;
        GeneralSecurityUtils.runAsAdmin(
                () -> wrestlerService.awardFans(participant.getId(), universeId, awardToGrant))
            .ifPresent(
                w ->
                    log.debug(
                        "Awarded {} fans to wrestler {} during promo", awardToGrant, w.getName()));
      }
    }

    // Improve relationships between participants based on promo quality
    if (roll >= 15) {
      relationshipService.improveGameplayRelationships(segment.getWrestlers(), roll >= 18 ? 2 : 1);
    }
  }

  private int calculateMatchQualityBonus(@NonNull final Segment segment, final int roll) {
    int bonus = 0;
    if (11 <= roll && roll <= 15) {
      bonus += 1_000;
    } else if (16 <= roll && roll <= 18) {
      bonus += 3_000;
    } else if (roll == 19) {
      bonus += 5_000;
    } else if (roll == 20) {
      bonus += 10_000;
      // Trigger 5-Star Classic achievement for all participants
      segment
          .getWrestlers()
          .forEach(
              w -> {
                if (w.getAccount() != null) {
                  legacyService.unlockAchievement(w.getAccount(), "FIVE_STAR_CLASSIC");
                }
              });
    }
    return bonus;
  }

  private int calculatePromoQualityBonus(final int roll) {
    int bonus = 0;
    if (2 <= roll && roll <= 3) {
      bonus += new DiceBag(random, new int[] {3}).roll();
    } else if (4 <= roll && roll <= 16) {
      bonus += new DiceBag(random, new int[] {6}).roll();
    } else if (17 <= roll && roll <= 19) {
      bonus += new DiceBag(random, new int[] {6, 6}).roll();
    } else if (roll == 20) {
      bonus += new DiceBag(random, new int[] {6, 6, 6}).roll();
    }
    return bonus;
  }

  private void handleTitleContenderFees(@NonNull final Segment segment) {
    Long universeId =
        segment.getShow().getUniverse() != null ? segment.getShow().getUniverse().getId() : 1L;
    for (Title title : segment.getTitles()) {
      List<Wrestler> currentChampions = title.getCurrentChampions();
      Long contenderEntryFee = titleService.getContenderEntryFee(title);

      for (Wrestler participant : segment.getWrestlers()) {
        if (!currentChampions.contains(participant)) {
          GeneralSecurityUtils.runAsAdmin(
                  () ->
                      wrestlerService.awardFans(
                          participant.getId(), universeId, -contenderEntryFee))
              .ifPresentOrElse(
                  w ->
                      log.info(
                          "Wrestler {} paid {} fans for contending in title segment {}",
                          w.getName(),
                          contenderEntryFee,
                          segment.getId()),
                  () ->
                      log.warn(
                          "Wrestler {} could not afford {} fans for contending in title segment {}",
                          participant.getName(),
                          contenderEntryFee,
                          segment.getId()));
        }
      }
    }
  }

  private void assignBumps(@NonNull final Segment segment, final Long universeId) {
    for (SegmentRule rule : segment.getSegmentRules()) {
      if (rule.getBumpAddition() == null) {
        continue;
      }
      switch (rule.getBumpAddition()) {
        case WINNERS:
          for (Wrestler winner : segment.getWinners()) {
            if (winner.getId() != null) {
              GeneralSecurityUtils.runAsAdmin(
                      () -> wrestlerService.addBump(winner.getId(), universeId))
                  .ifPresent(w -> log.debug("Added bump to winner {}", w.getName()));
            }
          }
          break;
        case LOSERS:
          for (Wrestler loser : segment.getLosers()) {
            if (loser.getId() != null) {
              GeneralSecurityUtils.runAsAdmin(
                      () -> wrestlerService.addBump(loser.getId(), universeId))
                  .ifPresent(w -> log.debug("Added bump to loser {}", w.getName()));
            }
          }
          break;
        case ALL:
          for (Wrestler participant : segment.getWrestlers()) {
            if (participant.getId() != null) {
              GeneralSecurityUtils.runAsAdmin(
                      () -> wrestlerService.addBump(participant.getId(), universeId))
                  .ifPresent(w -> log.debug("Added bump to participant {}", w.getName()));
            }
          }
          break;
        case NONE:
        default:
          break;
      }
    }
  }

  private void attemptRivalryResolution(
      @NonNull final Wrestler w1, @NonNull final Wrestler w2, final int threshold) {
    DiceBag diceBag = new DiceBag(20);
    Optional<Rivalry> rivalryBetweenWrestlers =
        rivalryService.getRivalryBetweenWrestlers(w1.getId(), w2.getId());
    rivalryBetweenWrestlers.ifPresent(
        rivalry ->
            rivalryService.attemptResolution(
                rivalry.getId(), diceBag.roll(), diceBag.roll(), threshold));
  }

  private void applyWearAndTear(@NonNull final Segment segment) {
    if (SegmentTypeNames.PROMO.equals(segment.getSegmentType().getName())
        || !gameSettingService.isWearAndTearEnabled()) {
      return;
    }

    int baseLoss = 1 + random.nextInt(3); // 1-3% base loss
    boolean isIntense =
        segment.getSegmentRules().stream()
            .anyMatch(
                r ->
                    r.getName() != null
                        && ("Extreme".equalsIgnoreCase(r.getName())
                            || "No DQ".equalsIgnoreCase(r.getName())
                            || "Cage".equalsIgnoreCase(r.getName())));

    if (isIntense) {
      baseLoss *= 2;
    }

    if (segment.isMainEvent()) {
      baseLoss += 1;
    }

    Long universeId = universeContextService.getCurrentUniverseId();

    for (Wrestler wrestler : segment.getWrestlers()) {
      WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), universeId);
      int current = state.getPhysicalCondition();
      int newCondition = Math.max(0, current - baseLoss);
      state.setPhysicalCondition(newCondition);
      log.info(
          "Applied {}% wear and tear to {} in league {}. New condition: {}%",
          baseLoss, wrestler.getName(), universeId, newCondition);

      int bumpChance = Math.max(0, 75 - newCondition);
      if (bumpChance > 0 && random.nextInt(100) < bumpChance) {
        log.info(
            "Wear-and-tear bump triggered for {} (condition: {}%, chance: {}%)",
            wrestler.getName(), newCondition, bumpChance);
        wrestlerService.addBump(wrestler.getId(), universeId);
      }

      // Check for retirement
      retirementService.checkRetirement(wrestler, universeId);
    }
  }

  private int calculateBaseRating() {
    DiceBag d20 = new DiceBag(random, new int[] {20});
    int roll = d20.roll();

    // 1-20 roll mapped to 0-100 base rating
    if (roll <= 5) {
      return roll * 4; // 1-5 -> 4-20
    }
    if (roll <= 10) {
      return 20 + (roll - 5) * 6; // 6-10 -> 26-50
    }
    if (roll <= 15) {
      return 50 + (roll - 10) * 6; // 11-15 -> 56-80
    }
    if (roll <= 18) {
      return 80 + (roll - 15) * 5; // 16-18 -> 85-95
    }
    if (roll == 19) {
      return 96 + random.nextInt(2); // 96-97
    }
    return 98 + random.nextInt(3); // 98-100
  }
}

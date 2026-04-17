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
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.ringside.RingsideAiService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.title.TitleService;
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
import lombok.NonNull;
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
  private final com.github.javydreamercsw.management.domain.league.LeagueRepository
      leagueRepository;
  private final com.github.javydreamercsw.management.domain.league.LeagueRosterRepository
      leagueRosterRepository;
  private final LegacyService legacyService;
  private final FactionService factionService;
  private final RingsideActionService ringsideActionService;
  private final RingsideAiService ringsideAiService;
  private final RetirementService retirementService;
  private final com.github.javydreamercsw.management.service.GameSettingService gameSettingService;
  private final com.github.javydreamercsw.management.service.relationship
          .WrestlerRelationshipService
      relationshipService;
  @Autowired private ApplicationEventPublisher eventPublisher;

  @Autowired
  public SegmentAdjudicationService(
      RivalryService rivalryService,
      WrestlerService wrestlerService,
      FeudResolutionService feudResolutionService,
      MultiWrestlerFeudService feudService,
      TitleService titleService,
      MatchFulfillmentRepository matchFulfillmentRepository,
      com.github.javydreamercsw.management.domain.league.LeagueRepository leagueRepository,
      LeagueRosterRepository leagueRosterRepository,
      LegacyService legacyService,
      FactionService factionService,
      RingsideActionService ringsideActionService,
      RingsideAiService ringsideAiService,
      RetirementService retirementService,
      GameSettingService gameSettingService,
      com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService
          relationshipService) {
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
        new Random());
  }

  public SegmentAdjudicationService(
      RivalryService rivalryService,
      WrestlerService wrestlerService,
      FeudResolutionService feudResolutionService,
      MultiWrestlerFeudService feudService,
      TitleService titleService,
      MatchFulfillmentRepository matchFulfillmentRepository,
      com.github.javydreamercsw.management.domain.league.LeagueRepository leagueRepository,
      LeagueRosterRepository leagueRosterRepository,
      LegacyService legacyService,
      FactionService factionService,
      RingsideActionService ringsideActionService,
      RingsideAiService ringsideAiService,
      RetirementService retirementService,
      GameSettingService gameSettingService,
      com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService
          relationshipService,
      Random random) {
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
    this.random = random;
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void adjudicateMatch(@NonNull Segment segment) {
    adjudicateMatch(segment, 1.0);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void adjudicateMatch(@NonNull Segment segment, double multiplier) {
    // Check for league fulfillment
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

    // Update League Stats if applicable
    if (segment.getShow().getUniverse() != null) {
      leagueRepository
          .findByUniverse(segment.getShow().getUniverse())
          .ifPresent(
              league -> {
                if (winners.isEmpty()) {
                  // Draw
                  for (Wrestler w : segment.getWrestlers()) {
                    leagueRosterRepository
                        .findByLeagueAndWrestler(league, w)
                        .ifPresent(
                            roster -> {
                              roster.setDraws(roster.getDraws() + 1);
                              leagueRosterRepository.save(roster);
                            });
                  }
                } else {
                  for (Wrestler w : winners) {
                    leagueRosterRepository
                        .findByLeagueAndWrestler(league, w)
                        .ifPresent(
                            roster -> {
                              roster.setWins(roster.getWins() + 1);
                              leagueRosterRepository.save(roster);
                            });
                  }
                  for (Wrestler w : losers) {
                    leagueRosterRepository
                        .findByLeagueAndWrestler(league, w)
                        .ifPresent(
                            roster -> {
                              roster.setLosses(roster.getLosses() + 1);
                              leagueRosterRepository.save(roster);
                            });
                  }
                }
              });
    }

    // Apply standard rewards (Multiplier 1.0 for normal league play)
    processRewards(segment, multiplier);

    // Update segment rating
    double chemistryBonus = relationshipService.calculateChemistryBonus(segment.getWrestlers());
    int baseRating = calculateBaseRating();
    int finalRating = (int) Math.min(100, baseRating * (1.0 + chemistryBonus));
    segment.setSegmentRating(finalRating);

    // Set crowd noise level (Rating with some random variance)
    int noiseVariance = random.nextInt(11) - 5; // -5 to +5
    segment.setCrowdNoiseLevel(Math.clamp(finalRating + noiseVariance, 0, 100));

    // Apply wear and tear
    applyWearAndTear(segment);

    // Automated Ringside Actions
    if (!segment.getSegmentType().getName().equals("Promo")) {
      for (Wrestler w : segment.getWrestlers()) {
        Object supporter = ringsideActionService.getBestSupporter(segment, w);
        if (supporter != null) {
          ringsideAiService.evaluateRingsideAction(segment, supporter, w);
        }
      }
    }

    if (!segment.getSegmentType().getName().equals("Promo")) {
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

    // Faction Affinity Logic
    Map<Long, Integer> factionParticipants = new HashMap<>();
    Map<Long, Integer> factionWinners = new HashMap<>();
    Long universeId =
        segment.getShow().getUniverse() != null ? segment.getShow().getUniverse().getId() : 1L;

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
            int affinityGain = (count - 1); // Base participation gain

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
            if (segment.getSegmentType().getName().equals("Promo")) {
              affinityGain += 1;
            }

            if (affinityGain > 0) {
              factionService.addAffinity(factionId, affinityGain);
            }
          }
        });

    // Add heat to rivalries
    int heat = 1;
    String segmentTypeName = segment.getSegmentType().getName();
    if (segmentTypeName.equals("Promo")) {
      heat = 4;
    }

    List<Wrestler> participants = segment.getWrestlers();
    // Skip all-pairs heat addition for Rumbles to avoid performance issues,
    // excessive rivalry creation, and because determining eliminations from
    // narration is complex. Bookers can manage these rivalries manually.
    if (!segment.getSegmentType().getName().equals("Abu Dhabi Rumble")) {
      for (int i = 0; i < participants.size(); i++) {
        for (int j = i + 1; j < participants.size(); j++) {
          rivalryService.addHeatBetweenWrestlers(
              participants.get(i).getId(),
              participants.get(j).getId(),
              heat,
              "From segment: " + segment.getSegmentType().getName());
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

    // Attempt to resolve feuds after PLE matches
    if (segment.getShow().isPremiumLiveEvent()) {
      log.info("Attempting to resolve feuds after PLE match: {}", segment.getShow().getName());
      for (Wrestler wrestler : segment.getWrestlers()) {
        List<MultiWrestlerFeud> feuds = feudService.getActiveFeudsForWrestler(wrestler.getId());
        for (MultiWrestlerFeud feud : feuds) {
          feudResolutionService.attemptFeudResolution(feud);
        }
      }
      // Check if feuds should be resolved.
      switch (segment.getSegmentType().getName()) {
        case "Tag Team":
          attemptRivalryResolution(segment.getWrestlers().get(0), segment.getWrestlers().get(2));
          attemptRivalryResolution(segment.getWrestlers().get(0), segment.getWrestlers().get(3));
          attemptRivalryResolution(segment.getWrestlers().get(1), segment.getWrestlers().get(2));
          attemptRivalryResolution(segment.getWrestlers().get(1), segment.getWrestlers().get(3));
          break;
        case "Abu Dhabi Rumble":
        case "One on One":
        case "Free-for-All":
          List<Wrestler> wrestlers = segment.getWrestlers();
          if (!wrestlers.isEmpty()) {
            Wrestler baseWrestler = winners.isEmpty() ? wrestlers.get(0) : winners.get(0);
            for (Wrestler other : wrestlers) {
              if (!baseWrestler.equals(other)) {
                attemptRivalryResolution(baseWrestler, other);
              }
            }
          }
          break;
      }
    }

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
            && segment.getSegmentType().getName().equals("Abu Dhabi Rumble")) {
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

  @Transactional
  public void processRewards(@NonNull Segment segment, double difficultyMultiplier) {
    List<Wrestler> winners = segment.getWinners();
    List<Wrestler> losers = new ArrayList<>(segment.getWrestlers());
    losers.removeAll(winners);

    DiceBag d20 = new DiceBag(random, new int[] {20});
    int roll = d20.roll();

    if (!segment.getSegmentType().getName().equals("Promo")) {
      handleMatchRewards(segment, winners, losers, roll, difficultyMultiplier);
    } else {
      handlePromoRewards(segment, roll, difficultyMultiplier);
    }
  }

  private void handleMatchRewards(
      Segment segment,
      List<Wrestler> winners,
      List<Wrestler> losers,
      int roll,
      double difficultyMultiplier) {
    int matchQualityBonus = calculateMatchQualityBonus(segment, roll);

    Long universeId =
        segment.getShow().getUniverse() != null ? segment.getShow().getUniverse().getId() : 1L;

    // Deduct fan fees for challengers in title segments
    if (segment.getIsTitleSegment() && !segment.getTitles().isEmpty()) {
      handleTitleContenderFees(segment, universeId);
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
            .ifPresent(
                w ->
                    log.debug(
                        "Awarded {} fans to winner {}", awardToGrant, w.getWrestler().getName()));
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
                w ->
                    log.debug(
                        "Deducted/awarded {} fans to loser {}",
                        changeToApply,
                        w.getWrestler().getName()));
      }
    }

    assignBumps(segment, universeId);

    // Improve relationships between participants based on match quality
    if (roll >= 15) {
      relationshipService.improveGameplayRelationships(segment.getWrestlers(), roll >= 18 ? 2 : 1);
    }
  }

  private long applyVenueBonuses(Segment segment, Wrestler wrestler, long amount) {
    double modifier = 1.0;

    // Arena Alignment Bias (+25%)
    if (segment.getShow().getArena() != null
        && wrestler.getAlignment() != null
        && wrestler.getAlignment().getAlignmentType() != null) {
      com.github.javydreamercsw.management.domain.world.Arena arena = segment.getShow().getArena();
      String wrestlerAlignment = wrestler.getAlignment().getAlignmentType().name();

      boolean matches = false;
      switch (arena.getAlignmentBias()) {
        case FACE_FAVORABLE -> matches = "FACE".equals(wrestlerAlignment);
        case HEEL_FAVORABLE -> matches = "HEEL".equals(wrestlerAlignment);
        case ANARCHIC -> matches = true; // Everyone gets a boost in anarchy
        case NEUTRAL -> matches = false;
      }

      if (matches) {
        modifier += 0.25;
        log.debug(
            "Applying 25% Arena Bias bonus to {}. Arena: {}, Bias: {}",
            wrestler.getName(), arena.getName(), arena.getAlignmentBias());
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

  private void handlePromoRewards(@NonNull Segment segment, int roll, double difficultyMultiplier) {
    int promoQualityBonus = calculatePromoQualityBonus(roll);

    Long universeId =
        segment.getShow().getUniverse() != null ? segment.getShow().getUniverse().getId() : 1L;

    // Assign fans to all participants
    for (Wrestler participant : segment.getWrestlers()) {
      if (participant.getId() != null) {
        long baseAward = promoQualityBonus * 1_000L;
        long finalAward = (long) (baseAward * difficultyMultiplier);

        // Apply Arena & Location Bonuses
        finalAward = applyVenueBonuses(segment, participant, finalAward);

        final long awardToGrant = finalAward;
        GeneralSecurityUtils.runAsAdmin(
                () -> wrestlerService.awardFans(participant.getId(), universeId, awardToGrant))
            .ifPresent(
                w ->
                    log.debug(
                        "Awarded {} fans to wrestler {} during promo",
                        awardToGrant,
                        w.getWrestler().getName()));
      }
    }

    // Improve relationships between participants based on promo quality
    if (roll >= 15) {
      relationshipService.improveGameplayRelationships(segment.getWrestlers(), roll >= 18 ? 2 : 1);
    }
  }

  private int calculateMatchQualityBonus(Segment segment, int roll) {
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

  private int calculatePromoQualityBonus(int roll) {
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

  private void handleTitleContenderFees(Segment segment, Long universeId) {
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
                          w.getWrestler().getName(),
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

  private void assignBumps(Segment segment, Long universeId) {
    for (com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule rule :
        segment.getSegmentRules()) {
      if (rule.getBumpAddition() == null) {
        continue;
      }
      switch (rule.getBumpAddition()) {
        case WINNERS:
          for (Wrestler winner : segment.getWinners()) {
            if (winner.getId() != null) {
              GeneralSecurityUtils.runAsAdmin(
                      () -> wrestlerService.addBump(winner.getId(), universeId))
                  .ifPresent(w -> log.debug("Added bump to winner {}", w.getWrestler().getName()));
            }
          }
          break;
        case LOSERS:
          for (Wrestler loser : segment.getLosers()) {
            if (loser.getId() != null) {
              GeneralSecurityUtils.runAsAdmin(
                      () -> wrestlerService.addBump(loser.getId(), universeId))
                  .ifPresent(w -> log.debug("Added bump to loser {}", w.getWrestler().getName()));
            }
          }
          break;
        case ALL:
          for (Wrestler participant : segment.getWrestlers()) {
            if (participant.getId() != null) {
              GeneralSecurityUtils.runAsAdmin(
                      () -> wrestlerService.addBump(participant.getId(), universeId))
                  .ifPresent(
                      w -> log.debug("Added bump to participant {}", w.getWrestler().getName()));
            }
          }
          break;
        case NONE:
        default:
          break;
      }
    }
  }

  private void attemptRivalryResolution(@NonNull Wrestler w1, @NonNull Wrestler w2) {
    DiceBag diceBag = new DiceBag(20);
    Optional<Rivalry> rivalryBetweenWrestlers =
        rivalryService.getRivalryBetweenWrestlers(w1.getId(), w2.getId());
    rivalryBetweenWrestlers.ifPresent(
        rivalry ->
            rivalryService.attemptResolution(rivalry.getId(), diceBag.roll(), diceBag.roll()));
  }

  private void applyWearAndTear(@NonNull Segment segment) {
    if (segment.getSegmentType().getName().equals("Promo")
        || !gameSettingService.isWearAndTearEnabled()
        || segment.getShow().getUniverse() == null) {
      return;
    }

    Long universeId = segment.getShow().getUniverse().getId();

    int baseLoss = 1 + random.nextInt(3); // 1-3% base loss
    boolean isIntense =
        segment.getSegmentRules().stream()
            .anyMatch(
                r ->
                    r.getName() != null
                        && (r.getName().equalsIgnoreCase("Extreme")
                            || r.getName().equalsIgnoreCase("No DQ")
                            || r.getName().equalsIgnoreCase("Cage")));

    if (isIntense) {
      baseLoss *= 2;
    }

    if (segment.isMainEvent()) {
      baseLoss += 1;
    }

    for (Wrestler wrestler : segment.getWrestlers()) {
      com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
          wrestlerService.getOrCreateState(wrestler.getId(), universeId);
      int current = state.getPhysicalCondition();
      state.setPhysicalCondition(Math.max(0, current - baseLoss));
      log.info(
          "Applied {}% wear and tear to {} in league {}. New condition: {}%",
          baseLoss, wrestler.getName(), universeId, state.getPhysicalCondition());

      // Check for retirement
      retirementService.checkRetirement(wrestler, universeId);
    }
  }

  private int calculateBaseRating() {
    DiceBag d20 = new DiceBag(random, new int[] {20});
    int roll = d20.roll();

    // 1-20 roll mapped to 0-100 base rating
    if (roll <= 5) return roll * 4; // 1-5 -> 4-20
    if (roll <= 10) return 20 + (roll - 5) * 6; // 6-10 -> 26-50
    if (roll <= 15) return 50 + (roll - 10) * 6; // 11-15 -> 56-80
    if (roll <= 18) return 80 + (roll - 15) * 5; // 16-18 -> 85-95
    if (roll == 19) return 96 + random.nextInt(2); // 96-97
    return 98 + random.nextInt(3); // 98-100
  }
}

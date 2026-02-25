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

import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
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

@Service
@Slf4j
public class SegmentAdjudicationService {

  private final RivalryService rivalryService;
  private final WrestlerService wrestlerService;
  private final FeudResolutionService feudResolutionService;
  private final MultiWrestlerFeudService feudService;
  private final Random random;
  private final TitleService titleService;
  private final MatchRewardService matchRewardService;
  private final MatchFulfillmentRepository matchFulfillmentRepository;
  private final com.github.javydreamercsw.management.domain.league.LeagueRosterRepository
      leagueRosterRepository;
  private final LegacyService legacyService;
  private final FactionService factionService;
  private final RingsideActionService ringsideActionService;
  private final RingsideAiService ringsideAiService;
  private final RetirementService retirementService;
  private final com.github.javydreamercsw.management.service.GameSettingService gameSettingService;
  private final com.github.javydreamercsw.management.service.world.LocationService locationService;
  private final com.github.javydreamercsw.management.service.world.ArenaService arenaService;
  @Autowired private ApplicationEventPublisher eventPublisher;

  @Autowired
  public SegmentAdjudicationService(
      RivalryService rivalryService,
      WrestlerService wrestlerService,
      FeudResolutionService feudResolutionService,
      MultiWrestlerFeudService feudService,
      TitleService titleService,
      MatchRewardService matchRewardService,
      MatchFulfillmentRepository matchFulfillmentRepository,
      com.github.javydreamercsw.management.domain.league.LeagueRosterRepository
          leagueRosterRepository,
      LegacyService legacyService,
      FactionService factionService,
      RingsideActionService ringsideActionService,
      RingsideAiService ringsideAiService,
      RetirementService retirementService,
      com.github.javydreamercsw.management.service.GameSettingService gameSettingService,
      com.github.javydreamercsw.management.service.world.LocationService locationService,
      com.github.javydreamercsw.management.service.world.ArenaService arenaService) {
    this(
        rivalryService,
        wrestlerService,
        feudResolutionService,
        feudService,
        titleService,
        matchRewardService,
        matchFulfillmentRepository,
        leagueRosterRepository,
        legacyService,
        factionService,
        ringsideActionService,
        ringsideAiService,
        retirementService,
        gameSettingService,
        locationService,
        arenaService,
        new Random());
  }

  public SegmentAdjudicationService(
      RivalryService rivalryService,
      WrestlerService wrestlerService,
      FeudResolutionService feudResolutionService,
      MultiWrestlerFeudService feudService,
      TitleService titleService,
      MatchRewardService matchRewardService,
      MatchFulfillmentRepository matchFulfillmentRepository,
      com.github.javydreamercsw.management.domain.league.LeagueRosterRepository
          leagueRosterRepository,
      LegacyService legacyService,
      FactionService factionService,
      RingsideActionService ringsideActionService,
      RingsideAiService ringsideAiService,
      RetirementService retirementService,
      com.github.javydreamercsw.management.service.GameSettingService gameSettingService,
      com.github.javydreamercsw.management.service.world.LocationService locationService,
      com.github.javydreamercsw.management.service.world.ArenaService arenaService,
      Random random) {
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;
    this.feudResolutionService = feudResolutionService;
    this.feudService = feudService;
    this.titleService = titleService;
    this.matchRewardService = matchRewardService;
    this.matchFulfillmentRepository = matchFulfillmentRepository;
    this.leagueRosterRepository = leagueRosterRepository;
    this.legacyService = legacyService;
    this.factionService = factionService;
    this.ringsideActionService = ringsideActionService;
    this.ringsideAiService = ringsideAiService;
    this.retirementService = retirementService;
    this.gameSettingService = gameSettingService;
    this.locationService = locationService;
    this.arenaService = arenaService;
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
    if (segment.getShow().getLeague() != null) {
      com.github.javydreamercsw.management.domain.league.League league =
          segment.getShow().getLeague();
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
    }

    // Apply standard rewards (Multiplier 1.0 for normal league play)
    matchRewardService.processRewards(segment, multiplier);

    // Apply Arena-specific fan adjustments
    if (segment.getShow() != null && segment.getShow().getArena() != null) {
      Arena arena = segment.getShow().getArena();
      int arenaCapacity = arena.getCapacity();
      Arena.AlignmentBias bias = arena.getAlignmentBias();

      for (Wrestler wrestler : segment.getWrestlers()) {
        long fanGain =
            wrestler.getFans()
                - wrestlerService
                    .getWrestlerStats(wrestler.getId())
                    .map(s -> s.getWins() * 1000L)
                    .orElse(0L); // Placeholder for actual fan gain calculation

        // Apply alignment bias
        if (bias != Arena.AlignmentBias.NEUTRAL && wrestler.getAlignment() != null) {
          boolean isFace =
              wrestler.getAlignment().getAlignmentType()
                  == com.github.javydreamercsw.management.domain.campaign.AlignmentType.FACE;
          if (bias == Arena.AlignmentBias.FACE_FAVORABLE && isFace) {
            fanGain *= 1.25; // 25% bonus for Faces
          } else if (bias == Arena.AlignmentBias.HEEL_FAVORABLE && !isFace) {
            fanGain *= 1.25; // 25% bonus for Heels
          } else if (bias == Arena.AlignmentBias.ANARCHIC) {
            // Anarchic crowds reward winners with more momentum/fans, punish losers
            if (winners.contains(wrestler)) {
              fanGain *= 1.10; // 10% bonus for winners
            } else {
              fanGain *= 0.90; // 10% penalty for losers
            }
          }
        }

        // Apply heritage bonus
        if (wrestler.getHeritageTag() != null && arena.getLocation() != null) {
          if (arena
              .getLocation()
              .getCulturalTags()
              .contains(wrestler.getHeritageTag().toLowerCase())) {
            fanGain *= 1.10; // 10% bonus for home territory
          }
        }

        // Capacity capping (simplified: ensure fanGain doesn't lead to exceeding capacity too
        // quickly)
        // A more complex system would check total show attendance vs. capacity
        fanGain =
            Math.min(
                fanGain,
                arenaCapacity / 10); // Example: max 10% of capacity per match, can be refined

        wrestlerService.awardFans(wrestler.getId(), fanGain);
      }
    }

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

    for (Wrestler participant : segment.getWrestlers()) {
      if (participant.getFaction() != null) {
        Long factionId = participant.getFaction().getId();
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
          int size = segment.getParticipants().size();
          for (int i = 1; i < size; i++) {
            attemptRivalryResolution(segment.getWrestlers().get(0), segment.getWrestlers().get(i));
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
        || !gameSettingService.isWearAndTearEnabled()) {
      return;
    }

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
      int current = wrestler.getPhysicalCondition();
      wrestler.setPhysicalCondition(Math.max(0, current - baseLoss));
      wrestlerService.save(wrestler);
      log.info(
          "Applied {}% wear and tear to {}. New condition: {}%",
          baseLoss, wrestler.getName(), wrestler.getPhysicalCondition());

      // Check for retirement
      retirementService.checkRetirement(wrestler);
    }
  }
}

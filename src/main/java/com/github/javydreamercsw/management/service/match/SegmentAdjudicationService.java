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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  @Autowired private ApplicationEventPublisher eventPublisher;

  @Autowired
  public SegmentAdjudicationService(
      WrestlerService wrestlerService,
      RivalryService rivalryService,
      FeudResolutionService feudResolutionService,
      MultiWrestlerFeudService feudService,
      TitleService titleService,
      MatchRewardService matchRewardService,
      MatchFulfillmentRepository matchFulfillmentRepository,
      com.github.javydreamercsw.management.domain.league.LeagueRosterRepository
          leagueRosterRepository) {
    this(
        rivalryService,
        wrestlerService,
        feudResolutionService,
        feudService,
        titleService,
        matchRewardService,
        matchFulfillmentRepository,
        leagueRosterRepository,
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
      Random random) {
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;
    this.feudResolutionService = feudResolutionService;
    this.feudService = feudService;
    this.titleService = titleService;
    this.matchRewardService = matchRewardService;
    this.matchFulfillmentRepository = matchFulfillmentRepository;
    this.leagueRosterRepository = leagueRosterRepository;
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

    // Add heat to rivalries
    int heat = 1;
    String segmentTypeName = segment.getSegmentType().getName();
    if (segmentTypeName.equals("Promo")) {
      heat = 4;
    }

    List<Wrestler> participants = segment.getWrestlers();
    for (int i = 0; i < participants.size(); i++) {
      for (int j = i + 1; j < participants.size(); j++) {
        rivalryService.addHeatBetweenWrestlers(
            participants.get(i).getId(),
            participants.get(j).getId(),
            heat,
            "From segment: " + segment.getSegmentType().getName());
      }
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
  }

  private void attemptRivalryResolution(@NonNull Wrestler w1, @NonNull Wrestler w2) {
    DiceBag diceBag = new DiceBag(20);
    Optional<Rivalry> rivalryBetweenWrestlers =
        rivalryService.getRivalryBetweenWrestlers(w1.getId(), w2.getId());
    rivalryBetweenWrestlers.ifPresent(
        rivalry ->
            rivalryService.attemptResolution(rivalry.getId(), diceBag.roll(), diceBag.roll()));
  }
}

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
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
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
  @Autowired private ApplicationEventPublisher eventPublisher;

  @Autowired
  public SegmentAdjudicationService(
      WrestlerService wrestlerService,
      RivalryService rivalryService,
      FeudResolutionService feudResolutionService,
      MultiWrestlerFeudService feudService,
      TitleService titleService) {
    this(
        rivalryService,
        wrestlerService,
        feudResolutionService,
        feudService,
        titleService,
        new Random());
  }

  public SegmentAdjudicationService(
      RivalryService rivalryService,
      WrestlerService wrestlerService,
      FeudResolutionService feudResolutionService,
      MultiWrestlerFeudService feudService,
      TitleService titleService,
      Random random) {
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;
    this.feudResolutionService = feudResolutionService;
    this.feudService = feudService;
    this.titleService = titleService;
    this.random = random;
  }

  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public void adjudicateMatch(@NonNull Segment segment) {
    List<Wrestler> winners = segment.getWinners();
    List<Wrestler> losers = new ArrayList<>(segment.getWrestlers());
    losers.removeAll(winners);

    DiceBag d20 = new DiceBag(random, new int[] {20});
    int roll = d20.roll();
    if (!segment.getSegmentType().getName().equals("Promo")) {
      int matchQualityBonus = 0;
      if (11 <= roll && roll <= 15) {
        matchQualityBonus += 1_000;
      } else if (16 <= roll && roll <= 18) {
        matchQualityBonus += 3_000;
      } else if (roll == 19) {
        matchQualityBonus += 5_000;
      } else if (roll == 20) {
        matchQualityBonus += 10_000;
      }

      // Deduct fan fees for challengers in title segments
      if (segment.getIsTitleSegment() && !segment.getTitles().isEmpty()) {
        for (Title title : segment.getTitles()) {
          List<Wrestler> currentChampions = title.getCurrentChampions();
          Long contenderEntryFee = titleService.getContenderEntryFee(title);

          for (Wrestler participant : segment.getWrestlers()) {
            // If a participant is not a current champion for this title, they are a challenger
            if (!currentChampions.contains(participant)) {
              if (wrestlerService.awardFans(participant.getId(), -contenderEntryFee).isPresent()) {
                log.info(
                    "Wrestler {} paid {} fans for contending in title segment {}",
                    participant.getName(),
                    contenderEntryFee,
                    segment.getId());
              } else {
                log.warn(
                    "Wrestler {} could not afford {} fans for contending in title segment {}",
                    participant.getName(),
                    contenderEntryFee,
                    segment.getId());
              }
            }
          }
        }
      }

      // Award fans to winners
      for (Wrestler winner : winners) {
        Long id = winner.getId();
        if (id != null) {
          DiceBag wdb = new DiceBag(random, new int[] {6, 6});
          // for winners 2d6 + 3 + (quality bonus) fans
          Long fanAward = (wdb.roll() + 3) * 1_000L + matchQualityBonus;
          wrestlerService
              .awardFans(id, fanAward)
              .ifPresent(w -> log.debug("Awarded {} fans to wrestler {}", fanAward, w.getName()));
        }
      }
      if (segment.getIsTitleSegment()) {
        for (Title title : segment.getTitles()) {
          List<Wrestler> currentChampions = title.getCurrentChampions();
          if (new HashSet<>(winners).containsAll(currentChampions)) {
            eventPublisher.publishEvent(
                new ChampionshipDefendedEvent(this, title, currentChampions, losers));
          } else {
            titleService.awardTitleTo(title, winners);
            eventPublisher.publishEvent(
                new ChampionshipChangeEvent(this, title, currentChampions, winners));
          }
        }
      }

      // Award/deduct fans from losers
      for (Wrestler loser : losers) {
        Long id = loser.getId();
        if (id != null) {
          DiceBag ldb = new DiceBag(random, new int[] {6});
          // for losers 1d6 - 4 + (quality bonus) fans. Can be negative
          Long fanChange = (ldb.roll() - 4) * 1_000L + matchQualityBonus;
          wrestlerService
              .awardFans(loser.getId(), fanChange)
              .ifPresent(
                  w ->
                      log.debug("Deducted/awarded {} fans to wrestler {}", fanChange, w.getName()));
        }
      }

      // Assign bumps based on segment rules
      for (SegmentRule rule : segment.getSegmentRules()) {
        switch (rule.getBumpAddition()) {
          case WINNERS:
            for (Wrestler winner : segment.getWinners()) {
              Long id = winner.getId();
              if (id != null) {
                wrestlerService
                    .addBump(id)
                    .ifPresent(w -> log.debug("Added bump to wrestler {}", w.getName()));
              }
            }
            break;
          case LOSERS:
            for (Wrestler loser : segment.getLosers()) {
              Long id = loser.getId();
              if (id != null) {
                wrestlerService
                    .addBump(id)
                    .ifPresent(w -> log.debug("Added bump to wrestler {}", w.getName()));
              }
            }
            break;
          case ALL:
            for (Wrestler participant : segment.getWrestlers()) {
              Long id = participant.getId();
              if (id != null) {
                wrestlerService
                    .addBump(id)
                    .ifPresent(w -> log.debug("Added bump to wrestler {}", w.getName()));
              }
            }
            break;
          case NONE:
          default:
            // Do nothing
            break;
        }
      }
    } else {
      int promoQualityBonus = 0;
      if (2 <= roll && roll <= 3) {
        promoQualityBonus += new DiceBag(random, new int[] {3}).roll();
      } else if (4 <= roll && roll <= 16) {
        promoQualityBonus += new DiceBag(random, new int[] {6}).roll();
      } else if (17 <= roll && roll <= 19) {
        promoQualityBonus += new DiceBag(random, new int[] {6, 6}).roll();
      } else if (roll == 20) {
        promoQualityBonus += new DiceBag(random, new int[] {6, 6, 6}).roll();
      }
      // Assign bumps to all participants
      for (Wrestler participant : segment.getWrestlers()) {
        Long id = participant.getId();
        if (id != null) {
          Long fanAward = promoQualityBonus * 1_000L;
          wrestlerService
              .awardFans(id, fanAward)
              .ifPresent(
                  w ->
                      log.debug(
                          "Awarded {} fans to wrestler {} during promo", fanAward, w.getName()));
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

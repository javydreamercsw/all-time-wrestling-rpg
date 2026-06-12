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
package com.github.javydreamercsw.management.service.tutorial;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeagueTutorialDefinition implements TutorialDefinition {

  private final WrestlerService wrestlerService;
  private final LeagueService leagueService;
  private final LeagueRepository leagueRepository;
  private final AccountRepository accountRepository;
  private final SegmentRepository segmentRepository;

  @Override
  public Universe.UniverseType getMode() {
    return Universe.UniverseType.LEAGUE;
  }

  @Override
  public List<TutorialStep> getSteps() {
    return List.of(step1(), step2(), step3());
  }

  private TutorialStep step1() {
    return new TutorialStep() {
      @Override
      public int getStepNumber() {
        return 1;
      }

      @Override
      public String getTitle() {
        return "Assign Your Wrestler";
      }

      @Override
      public String getInstructions() {
        return "Head to your Player Dashboard and select a wrestler to represent you in this"
            + " league. Click the wrestler selector at the top of the dashboard and choose one"
            + " from the list.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that you have an active wrestler assigned to your account.";
      }

      @Override
      public String getTargetRoute() {
        return "player";
      }

      @Override
      public String getTargetViewLabel() {
        return "Player Dashboard";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/league/step1.png";
      }

      @Override
      public String validate(final Account account) {
        return account.getActiveWrestlerId() != null
            ? null
            : "You haven't selected an active wrestler yet. Go to the Player Dashboard and choose"
                + " a wrestler to represent you.";
      }

      @Override
      public void beforeStep(final Account account) {
        if (wrestlerService.getAllWrestlers().isEmpty()) {
          wrestlerService.createWrestler("Tutorial Wrestler", true, "A starter wrestler.");
        }
      }
    };
  }

  private TutorialStep step2() {
    return new TutorialStep() {
      @Override
      public int getStepNumber() {
        return 2;
      }

      @Override
      public String getTitle() {
        return "Join a League";
      }

      @Override
      public String getInstructions() {
        return "Navigate to Leagues and join an existing league. If you don't see any leagues,"
            + " ask your commissioner to create one or create your own from the Leagues page.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that you are a member of at least one league.";
      }

      @Override
      public String getTargetRoute() {
        return "leagues";
      }

      @Override
      public String getTargetViewLabel() {
        return "Leagues";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/league/step2.png";
      }

      @Override
      public String validate(final Account account) {
        boolean isMember = !leagueService.getLeaguesForUser(account).isEmpty();
        return isMember
            ? null
            : "You haven't joined a league yet. Go to the Leagues page and join or create one.";
      }

      @Override
      public void beforeStep(final Account account) {
        if (leagueRepository.count() == 0) {
          // Seed a tutorial league using the admin account as commissioner so the player
          // has something to join.
          accountRepository
              .findByUsername("admin")
              .ifPresent(
                  admin ->
                      leagueService.createLeague(
                          "Tutorial League", admin, 10, Collections.emptySet(), false));
        }
      }
    };
  }

  private TutorialStep step3() {
    return new TutorialStep() {
      @Override
      public int getStepNumber() {
        return 3;
      }

      @Override
      public String getTitle() {
        return "Enter a Match";
      }

      @Override
      public String getInstructions() {
        return "In your league, sign up for a scheduled match or accept a challenge from another"
            + " player. Your wrestler needs to be listed in at least one upcoming segment.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that your wrestler is scheduled for at least one league segment.";
      }

      @Override
      public String getTargetRoute() {
        return "leagues";
      }

      @Override
      public String getTargetViewLabel() {
        return "Leagues";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/league/step3.png";
      }

      @Override
      public String validate(final Account account) {
        Long wrestlerId = account.getActiveWrestlerId();
        if (wrestlerId == null) {
          return "You need to select a wrestler first (complete Step 1).";
        }
        boolean hasSegment =
            wrestlerService
                .findByIdWithDetails(wrestlerId)
                .map(
                    wrestler -> {
                      List<Segment> segments =
                          segmentRepository.findByWrestlerParticipationWithShow(wrestler);
                      return !segments.isEmpty();
                    })
                .orElse(false);
        return hasSegment
            ? null
            : "Your wrestler isn't scheduled for any league segments yet. Go to your league and"
                + " enter a match.";
      }
    };
  }
}

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
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.league.DraftPickRepository;
import com.github.javydreamercsw.management.domain.league.DraftRepository;
import com.github.javydreamercsw.management.domain.league.LeagueMembership;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseMembershipRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.service.universe.InviteService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeagueTutorialDefinition implements TutorialDefinition {

  private final AccountService accountService;
  private final LeagueService leagueService;
  private final LeagueMembershipRepository leagueMembershipRepository;
  private final DraftRepository draftRepository;
  private final DraftPickRepository draftPickRepository;
  private final InviteService inviteService;
  private final UniverseMembershipRepository universeMembershipRepository;
  private final UniverseContextService universeContextService;
  private final UniverseService universeService;

  @Override
  public Universe.UniverseType getMode() {
    return Universe.UniverseType.LEAGUE;
  }

  @Override
  public List<TutorialStep> getSteps() {
    return List.of(step1(), step2(), step3(), step4());
  }

  private TutorialStep step1() {
    return new TutorialStep() {
      @Override
      public int getStepNumber() {
        return 1;
      }

      @Override
      public String getTitle() {
        return "Your Tutorial League";
      }

      @Override
      public String getInstructions() {
        return "We've created a tutorial league for you and made you the commissioner."
            + " Head to the Leagues page to see it. As commissioner you control the draft rules,"
            + " who joins, and when the season starts.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that you are the commissioner of at least one league.";
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
        return "/images/tutorial/league/step1.png";
      }

      @Override
      public String validate(final Account account) {
        boolean isCommissioner =
            leagueMembershipRepository.findByMember(account).stream()
                .anyMatch(
                    m ->
                        m.getRole() == LeagueMembership.LeagueRole.COMMISSIONER
                            || m.getRole() == LeagueMembership.LeagueRole.COMMISSIONER_PLAYER);
        return isCommissioner
            ? null
            : "You haven't created a league yet. Go to the Leagues page and create one.";
      }

      @Override
      public void beforeStep(final Account account) {
        // Elevate to BOOKER so the player can manage a league.
        accountService.grantRole(account, RoleName.BOOKER);
        // Seed a tutorial league owned by the player as commissioner (idempotent).
        boolean alreadyCommissioner =
            leagueMembershipRepository.findByMember(account).stream()
                .anyMatch(
                    m ->
                        m.getRole() == LeagueMembership.LeagueRole.COMMISSIONER
                            || m.getRole() == LeagueMembership.LeagueRole.COMMISSIONER_PLAYER);
        if (!alreadyCommissioner) {
          leagueService.createLeague(
              "Tutorial League – " + account.getUsername(),
              account,
              10,
              Collections.emptySet(),
              false);
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
        return "Invite Players";
      }

      @Override
      public String getInstructions() {
        return "A league needs players! Open your league, go to the Members section, and generate"
            + " an invite link. Share it with other players so they can join. Wait until at least"
            + " one other player has accepted before continuing.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that you've sent an invite and at least one other player has joined"
            + " your universe.";
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
        Universe tutorialUniverse =
            universeService.findByName("Tutorial – " + account.getUsername()).orElse(null);
        if (tutorialUniverse == null) {
          return "Tutorial universe not found. Please restart the tutorial.";
        }
        boolean inviteSent = !inviteService.listActiveInvites(tutorialUniverse).isEmpty();
        if (!inviteSent) {
          return "No invite has been sent yet. Open your league, go to Members, and generate an"
              + " invite link.";
        }
        long memberCount = universeMembershipRepository.findByUniverse(tutorialUniverse).size();
        if (memberCount < 2) {
          return "Waiting for a player to accept your invite. Share the link and check back once"
              + " someone has joined.";
        }
        return null;
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
        return "Start the Draft";
      }

      @Override
      public String getInstructions() {
        return "Now that your players are in, it's time to draft wrestlers! Open your league and"
            + " click \"Start Draft\". The system will use a snake-draft order. You'll be up"
            + " first as commissioner.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that a draft has been started for your league.";
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
        boolean draftStarted = draftRepository.existsByLeague_Commissioner(account);
        return draftStarted
            ? null
            : "No draft found for your league. Open your league and click \"Start Draft\".";
      }
    };
  }

  private TutorialStep step4() {
    return new TutorialStep() {
      @Override
      public int getStepNumber() {
        return 4;
      }

      @Override
      public String getTitle() {
        return "Make Your First Draft Pick";
      }

      @Override
      public String getInstructions() {
        return "The draft is live! Navigate to the Draft page, wait for your turn, and select a"
            + " wrestler to add to your roster. Once all picks are done the draft closes and your"
            + " league season begins.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that you've made at least one draft pick.";
      }

      @Override
      public String getTargetRoute() {
        return "draft";
      }

      @Override
      public String getTargetViewLabel() {
        return "Draft";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/league/step4.png";
      }

      @Override
      public String validate(final Account account) {
        boolean hasPick = draftPickRepository.existsByUser(account);
        return hasPick
            ? null
            : "You haven't made a pick yet. Go to the Draft page and select a wrestler.";
      }

      @Override
      public void afterStep(final Account account) {
        // Idempotent — ensures commissioner retains BOOKER after tutorial completes.
        accountService.grantRole(account, RoleName.BOOKER);
      }
    };
  }
}

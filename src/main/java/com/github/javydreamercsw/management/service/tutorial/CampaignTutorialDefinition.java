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
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CampaignTutorialDefinition implements TutorialDefinition {

  private final WrestlerService wrestlerService;
  private final CampaignService campaignService;
  private final BackstageActionHistoryRepository backstageActionHistoryRepository;

  @Override
  public Universe.UniverseType getMode() {
    return Universe.UniverseType.CAMPAIGN;
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
      public InteractionMode getInteractionMode() {
        return InteractionMode.INLINE;
      }

      @Override
      public String getTitle() {
        return "Assign Your Wrestler";
      }

      @Override
      public String getInstructions() {
        return "Head to your Player Dashboard and select a wrestler to represent you in this"
            + " campaign. Click the wrestler selector at the top of the dashboard and choose one"
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
        return "/images/tutorial/campaign/step1.png";
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
        return "Start Your Campaign";
      }

      @Override
      public String getInstructions() {
        return "A campaign has been started for your wrestler. Head over to the Campaign view to"
            + " see your current chapter and the story-driven choices that await you.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that your wrestler has an active campaign.";
      }

      @Override
      public String getTargetRoute() {
        return "campaign";
      }

      @Override
      public String getTargetViewLabel() {
        return "Campaign";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/campaign/step2.png";
      }

      @Override
      public String validate(final Account account) {
        Long wrestlerId = account.getActiveWrestlerId();
        if (wrestlerId == null) {
          return "You need to select a wrestler first (complete Step 1).";
        }
        return wrestlerService
            .findByIdWithDetails(wrestlerId)
            .flatMap(campaignService::getCampaignForWrestler)
            .map(campaign -> (String) null)
            .orElse(
                "Your wrestler doesn't have an active campaign yet. Please wait a moment and try"
                    + " again.");
      }

      @Override
      public void beforeStep(final Account account) {
        Long wrestlerId = account.getActiveWrestlerId();
        if (wrestlerId == null) {
          return;
        }
        wrestlerService
            .findByIdWithDetails(wrestlerId)
            .ifPresent(
                wrestler -> {
                  if (!campaignService.hasActiveCampaign(wrestler)) {
                    campaignService.startCampaign(wrestler);
                  }
                });
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
        return "Make Your First Decision";
      }

      @Override
      public String getInstructions() {
        return "In your active campaign, navigate to the backstage area and make your first"
            + " decision. Your choices shape your wrestler's story — choose wisely!";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that you've made at least one backstage decision in your campaign.";
      }

      @Override
      public String getTargetRoute() {
        return "campaign/actions";
      }

      @Override
      public String getTargetViewLabel() {
        return "Backstage Actions";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/campaign/step3.png";
      }

      @Override
      public String validate(final Account account) {
        Long wrestlerId = account.getActiveWrestlerId();
        if (wrestlerId == null) {
          return "You need to select a wrestler first (complete Step 1).";
        }
        boolean hasDecision =
            wrestlerService
                .findByIdWithDetails(wrestlerId)
                .flatMap(campaignService::getCampaignForWrestler)
                .map(backstageActionHistoryRepository::existsByCampaign)
                .orElse(false);
        return hasDecision
            ? null
            : "You haven't made a backstage decision yet. Go to Backstage Actions and make your"
                + " first choice.";
      }
    };
  }

  private Wrestler requireWrestler(final Account account) {
    Long id = account.getActiveWrestlerId();
    if (id == null) return null;
    return wrestlerService.findByIdWithDetails(id).orElse(null);
  }
}

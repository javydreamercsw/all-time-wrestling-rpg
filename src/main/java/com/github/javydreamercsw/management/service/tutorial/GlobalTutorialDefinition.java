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
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GlobalTutorialDefinition implements TutorialDefinition {

  private final ShowService showService;
  private final ShowTypeService showTypeService;
  private final UniverseContextService universeContextService;
  private final AccountService accountService;

  @Override
  public Universe.UniverseType getMode() {
    return Universe.UniverseType.GLOBAL;
  }

  @Override
  public boolean isAdvanced() {
    return true;
  }

  @Override
  public String getWarning() {
    return "This tutorial gives you full creative control over your wrestling universe. You will be"
        + " granted admin access to manage all settings, wrestlers, shows, and titles.";
  }

  @Override
  public List<TutorialStep> getSteps() {
    return List.of(step1(), step2(), step3(), step4(), step5());
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
        return "Pick Your Featured Wrestler";
      }

      @Override
      public String getInstructions() {
        return "Choose the wrestler you want to follow in your universe. This is the star whose"
            + " story you'll be telling. You can manage all wrestlers later from Entities >"
            + " Wrestlers.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that you have an active wrestler assigned to your account.";
      }

      @Override
      public String getTargetRoute() {
        return "wrestler-list";
      }

      @Override
      public String getTargetViewLabel() {
        return "Wrestlers";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/global/step1.png";
      }

      @Override
      public void beforeStep(final Account account) {
        // Grant admin so the player can manage all universe resources.
        accountService.grantRole(account, RoleName.ADMIN);
      }

      @Override
      public String validate(final Account account) {
        return account.getActiveWrestlerId() != null
            ? null
            : "You haven't selected a wrestler yet. Pick one from the list below.";
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
        return "Create Your First Show";
      }

      @Override
      public String getInstructions() {
        return "Head over to Shows — we've created an empty show called \"Tutorial Night\" for you."
            + " Open it and start building your card. You can create your own shows from the Shows"
            + " page at any time.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that at least one show exists.";
      }

      @Override
      public String getTargetRoute() {
        return "show-list";
      }

      @Override
      public String getTargetViewLabel() {
        return "Shows";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/global/step2.png";
      }

      @Override
      public String validate(final Account account) {
        return showService.existsAnyShow()
            ? null
            : "No show found yet. Head to Shows and create your first one.";
      }

      @Override
      public void beforeStep(final Account account) {
        if (showService.existsAnyShow()) {
          return;
        }
        Long showTypeId =
            showTypeService.findAll().stream().findFirst().map(ShowType::getId).orElse(null);
        if (showTypeId == null) {
          return;
        }
        showService.createShow(
            "Tutorial Night",
            "Your first show as a universe booker.",
            showTypeId,
            LocalDate.now(),
            null,
            null,
            universeContextService.getCurrentUniverseId(),
            null,
            null,
            null);
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
        return "Plan Your Card";
      }

      @Override
      public String getInstructions() {
        return "Open your show from the Shows list and add matches or segments to fill the card."
            + " Click \"Add Segment\" inside the show, choose a segment type (e.g. Singles Match),"
            + " and pick participants. Add at least one segment to continue.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that your show has at least one segment planned.";
      }

      @Override
      public String getTargetRoute() {
        return "show-list";
      }

      @Override
      public String getTargetViewLabel() {
        return "Shows";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/global/step3.png";
      }

      @Override
      public String validate(final Account account) {
        return showService.existsShowWithSegments()
            ? null
            : "No segments found yet. Open your show and add at least one match or segment.";
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
        return "Run Your Show";
      }

      @Override
      public String getInstructions() {
        return "Open your show from the Shows list, then click Adjudicate to simulate the results."
            + " The system will roll outcomes and optionally generate AI narration for each"
            + " segment.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that at least one segment has been adjudicated.";
      }

      @Override
      public String getTargetRoute() {
        return "show-list";
      }

      @Override
      public String getTargetViewLabel() {
        return "Shows";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/global/step4.png";
      }

      @Override
      public String validate(final Account account) {
        return showService.existsAdjudicatedSegment()
            ? null
            : "No adjudicated segments found yet. Open your show, click Adjudicate, and simulate"
                + " the results.";
      }
    };
  }

  private TutorialStep step5() {
    return new TutorialStep() {
      @Override
      public int getStepNumber() {
        return 5;
      }

      @Override
      public String getTitle() {
        return "Enhance with AI";
      }

      @Override
      public String getInstructions() {
        return "You can configure AI providers to have AI assist on planning and narrating your"
            + " matches.";
      }

      @Override
      public String getValidationHint() {
        return "N/A";
      }

      @Override
      public String getTargetRoute() {
        return "show-list";
      }

      @Override
      public String getTargetViewLabel() {
        return "Shows";
      }

      @Override
      public String getImagePath() {
        return "/images/tutorial/global/step5.png";
      }

      @Override
      public String validate(final Account account) {
        return null;
      }
    };
  }
}

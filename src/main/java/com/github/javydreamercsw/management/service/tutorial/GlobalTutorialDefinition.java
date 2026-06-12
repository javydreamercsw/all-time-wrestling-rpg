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
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GlobalTutorialDefinition implements TutorialDefinition {

  private final WrestlerService wrestlerService;
  private final ShowService showService;

  @Override
  public Universe.UniverseType getMode() {
    return Universe.UniverseType.GLOBAL;
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
        return "Create a Show";
      }

      @Override
      public String getInstructions() {
        return "Navigate to Shows and create your first event. Add at least one segment (match or"
            + " promo) and assign wrestlers to it. This is the foundation of your wrestling"
            + " universe.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that you have at least one show with at least one segment.";
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
        boolean hasShowWithSegment =
            showService.findAll().stream().anyMatch(show -> !show.getSegments().isEmpty());
        return hasShowWithSegment
            ? null
            : "No shows with segments found yet. Go to Shows, create a show, and add at least one"
                + " segment.";
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
        return "Run Your Show";
      }

      @Override
      public String getInstructions() {
        return "Open one of your shows, go to the show detail page, and click Adjudicate to"
            + " simulate the results. The system will roll outcomes and optionally generate AI"
            + " narration for each segment.";
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
        return "/images/tutorial/global/step3.png";
      }

      @Override
      public String validate(final Account account) {
        boolean hasAdjudicated =
            showService.findAll().stream()
                .flatMap(show -> show.getSegments().stream())
                .anyMatch(
                    segment ->
                        AdjudicationStatus.ADJUDICATED.equals(segment.getAdjudicationStatus()));
        return hasAdjudicated
            ? null
            : "No adjudicated segments found yet. Open a show, click Adjudicate, and simulate the"
                + " results.";
      }
    };
  }
}

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
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GlobalTutorialDefinition implements TutorialDefinition {

  private final WrestlerService wrestlerService;
  private final ShowService showService;
  private final ShowTypeService showTypeService;
  private final SegmentTypeService segmentTypeService;
  private final SegmentService segmentService;
  private final UniverseContextService universeContextService;

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
        return "Your First Show";
      }

      @Override
      public String getInstructions() {
        return "We've booked a show for you featuring your wrestler. Head over to Shows to see it."
            + " Your wrestler has been given a main event match against a random opponent, plus two"
            + " supporting bouts to fill out the card.";
      }

      @Override
      public String getValidationHint() {
        return "We'll check that your show is ready to go.";
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
            : "Your show isn't ready yet. Please wait a moment and try again.";
      }

      @Override
      public void beforeStep(final Account account) {
        // Idempotent: only seed the show if none exists yet.
        if (showService.findAll().stream().anyMatch(show -> !show.getSegments().isEmpty())) {
          return;
        }

        Long wrestlerId = account.getActiveWrestlerId();
        if (wrestlerId == null) {
          return;
        }

        Wrestler hero = wrestlerService.findByIdWithDetails(wrestlerId).orElse(null);
        if (hero == null) {
          return;
        }

        // Resolve show type — use first available, or null (the service tolerates a missing type
        // only when null is passed; use findAll and grab the first if present).
        Long showTypeId =
            showTypeService.findAll().stream().findFirst().map(st -> st.getId()).orElse(null);
        if (showTypeId == null) {
          return;
        }

        Long universeId = universeContextService.getCurrentUniverseId();

        Show show =
            showService.createShow(
                "Tutorial Night",
                "Your first show as a universe booker.",
                showTypeId,
                LocalDate.now(),
                null,
                null,
                universeId,
                null,
                null,
                null);

        // Resolve a simple "match" segment type
        SegmentType matchType =
            segmentTypeService.findAll().stream()
                .filter(
                    st ->
                        st.getName() != null
                            && (st.getName().toLowerCase().contains("match")
                                || st.getName().toLowerCase().contains("singles")))
                .findFirst()
                .orElseGet(() -> segmentTypeService.findAll().stream().findFirst().orElse(null));

        if (matchType == null) {
          return;
        }

        // Pick random opponents matching hero's gender
        Gender heroGender = hero.getGender();
        List<Wrestler> pool =
            new ArrayList<>(
                wrestlerService.findAllFiltered(null, heroGender, universeId).stream()
                    .filter(w -> !w.getId().equals(wrestlerId))
                    .filter(w -> Boolean.TRUE.equals(w.getActive()))
                    .toList());

        if (pool.isEmpty()) {
          // Fall back to any gender if no same-gender opponents exist
          pool =
              new ArrayList<>(
                  wrestlerService.findAllFiltered(null, null, universeId).stream()
                      .filter(w -> !w.getId().equals(wrestlerId))
                      .filter(w -> Boolean.TRUE.equals(w.getActive()))
                      .toList());
        }

        if (pool.isEmpty()) {
          return;
        }

        Collections.shuffle(pool);

        // Main event — hero vs first opponent
        Segment mainEvent = segmentService.createSegment(show, matchType, Instant.now());
        segmentService.addParticipant(mainEvent, hero);
        segmentService.addParticipant(mainEvent, pool.get(0));

        // Two supporting matches from the remaining pool (need at least 4 more wrestlers)
        for (int i = 1; i + 1 < pool.size() && i < 5; i += 2) {
          Segment support = segmentService.createSegment(show, matchType, Instant.now());
          segmentService.addParticipant(support, pool.get(i));
          segmentService.addParticipant(support, pool.get(i + 1));
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
            : "No adjudicated segments found yet. Open your show, click Adjudicate, and simulate"
                + " the results.";
      }
    };
  }
}

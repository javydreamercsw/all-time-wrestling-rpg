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
package com.github.javydreamercsw.management.ui.component;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Karibu tests for the tutorial page's Quick Tour card (ATW-p0sz, stepper ATW-eqj5). */
class TournamentQuickTourCardTest extends AbstractViewTest {

  private TournamentQuickTourCard buildCard() {
    TournamentQuickTourCard card = new TournamentQuickTourCard();
    UI.getCurrent().add(card);
    return card;
  }

  @Test
  @DisplayName("Card renders heading, intro, and the step indicator")
  void rendersHeadingAndStepIndicator() {
    TournamentQuickTourCard card = buildCard();

    H3 heading =
        _find(card, H3.class, spec -> spec.withPredicate(c -> c.getText().contains("Quick Tour")))
            .getFirst();
    assertThat(heading.getText()).contains("Run Your First Tournament");

    Span indicator = _get(card, Span.class, spec -> spec.withId("quick-tour-step-indicator"));
    assertThat(indicator.getText()).contains("Step 1 of 5");
  }

  @Test
  @DisplayName("Only the current step is visible at a time")
  void onlyCurrentStepVisible() {
    TournamentQuickTourCard card = buildCard();

    List<Paragraph> visible =
        _find(
            card,
            Paragraph.class,
            spec ->
                spec.withPredicate(
                    p ->
                        p.isVisible()
                            && p.getText() != null
                            && (p.getText().contains("New Tournament")
                                || p.getText().contains("assign each player")
                                || p.getText().contains("Randomize")
                                || p.getText().contains("winner of each match")
                                || p.getText().contains("granted automatically"))));
    assertThat(visible).hasSize(1);
  }

  @Test
  @DisplayName("All five step titles are reachable via Next, ending at step 5")
  void nextWalksAllFiveSteps() {
    TournamentQuickTourCard card = buildCard();

    List<String> seen = new ArrayList<>();
    for (int i = 1; i < 5 + 1; i++) {
      Span indicator = _get(card, Span.class, spec -> spec.withId("quick-tour-step-indicator"));
      assertThat(indicator.getText()).contains("Step " + i + " of 5");
      seen.add(currentStepText(card));
      if (i < 5) {
        _get(card, Button.class, spec -> spec.withId("next-button")).click();
      }
    }
    assertThat(seen)
        .containsExactly(
            "1. Create a Tournament",
            "2. Add Players",
            "3. Seed and Start",
            "4. Enter Results",
            "5. Crown the Winner");
  }

  @Test
  @DisplayName("Back returns to the previous step; step 1 has no Back")
  void backNavigation() {
    TournamentQuickTourCard card = buildCard();

    assertThat(_find(card, Button.class, spec -> spec.withId("back-button"))).isEmpty();

    _get(card, Button.class, spec -> spec.withId("next-button")).click();
    Span indicator = _get(card, Span.class, spec -> spec.withId("quick-tour-step-indicator"));
    assertThat(indicator.getText()).contains("Step 2 of 5");

    _get(card, Button.class, spec -> spec.withId("back-button")).click();
    indicator = _get(card, Span.class, spec -> spec.withId("quick-tour-step-indicator"));
    assertThat(indicator.getText()).contains("Step 1 of 5");
  }

  @Test
  @DisplayName("Next is hidden on the last step; Go click handler runs")
  void lastStepHidesNext_goRuns() {
    TournamentQuickTourCard card = buildCard();
    for (int i = 1; i < 5; i++) {
      _get(card, Button.class, spec -> spec.withId("next-button")).click();
    }

    assertThat(_get(card, Span.class, spec -> spec.withId("quick-tour-step-indicator")).getText())
        .contains("Step 5 of 5");
    assertThat(_find(card, Button.class, spec -> spec.withId("next-button"))).isEmpty();

    Button go = _get(card, Button.class, spec -> spec.withId("go-button"));
    go.click();
    // Navigation is attempted against the mocked route registry; the handler ran without error.
    assertThat(go.isEnabled()).isTrue();
  }

  private String currentStepText(TournamentQuickTourCard card) {
    // The step title is the bold Span inside the step area (ids: quick-tour-step-title)
    return _get(card, Span.class, spec -> spec.withId("quick-tour-step-title")).getText();
  }
}

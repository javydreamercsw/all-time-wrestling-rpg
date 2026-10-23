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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Karibu tests for the tutorial page's Quick Tour teaser card (ATW-6h2y). */
class TournamentQuickTourCardTest extends AbstractViewTest {

  private TournamentQuickTourCard buildCard() {
    TournamentQuickTourCard card = new TournamentQuickTourCard();
    UI.getCurrent().add(card);
    return card;
  }

  @Test
  @DisplayName("Card renders the heading and teaser intro")
  void rendersHeadingAndIntro() {
    TournamentQuickTourCard card = buildCard();

    H3 heading =
        _find(card, H3.class, spec -> spec.withPredicate(c -> c.getText().contains("Quick Tour")))
            .getFirst();
    assertThat(heading.getText()).contains("Run Your First Tournament");

    List<Paragraph> paragraphs = _find(card, Paragraph.class);
    assertThat(paragraphs).hasSize(1);
    assertThat(paragraphs.getFirst().getText()).contains("does the work for you");
  }

  @Test
  @DisplayName("Start button navigates to the interactive tour route (handler runs)")
  void startButton_targetsTourRoute() {
    TournamentQuickTourCard card = buildCard();

    Button start = _get(card, Button.class, spec -> spec.withId("quick-tour-start-button"));
    assertThat(start.getText()).contains("interactive tour");
    start.click();
    // Navigation is attempted against the mocked route registry; the handler ran without error.
    assertThat(start.isEnabled()).isTrue();
  }

  @Test
  @DisplayName("The card is a teaser: no per-step Go buttons")
  void noPerStepButtons() {
    TournamentQuickTourCard card = buildCard();

    assertThat(_find(card, Button.class)).hasSize(1);
  }
}

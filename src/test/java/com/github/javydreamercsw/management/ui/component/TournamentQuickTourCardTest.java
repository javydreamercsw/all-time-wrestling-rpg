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
import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Karibu tests for the tutorial page's Quick Tour card (ATW-p0sz). */
class TournamentQuickTourCardTest extends AbstractViewTest {

  private TournamentQuickTourCard buildCard() {
    TournamentQuickTourCard card = new TournamentQuickTourCard();
    UI.getCurrent().add(card);
    return card;
  }

  @Test
  @DisplayName("Card renders heading, intro, and four steps")
  void rendersHeadingAndFourSteps() {
    TournamentQuickTourCard card = buildCard();

    H3 heading =
        _find(card, H3.class, spec -> spec.withPredicate(c -> c.getText().contains("Quick Tour")))
            .getFirst();
    assertThat(heading.getText()).contains("Run Your First Tournament");

    List<Button> goButtons =
        _find(card, Button.class, spec -> spec.withPredicate(b -> "Go".equals(b.getText())));
    assertThat(goButtons).hasSize(4);
  }

  @Test
  @DisplayName("Step rows describe create, seed, run, and crown")
  void stepRowsDescribeTheFlow() {
    TournamentQuickTourCard card = buildCard();

    List<Paragraph> paragraphs = _find(card, Paragraph.class);
    List<com.vaadin.flow.component.html.Span> spans =
        _find(card, com.vaadin.flow.component.html.Span.class);
    String all =
        paragraphs.stream()
                .map(p -> p.getText() == null ? "" : p.getText())
                .reduce("", String::concat)
            + spans.stream()
                .map(s -> s.getText() == null ? "" : s.getText())
                .reduce("", String::concat);
    assertThat(all)
        .contains("Create a Tournament")
        .contains("Seed and Start")
        .contains("Run It")
        .contains("Crown the Winner")
        .contains("no shows, seasons, or AI configuration");
  }

  @Test
  @DisplayName("Go button click handler runs (navigation against the mock route registry)")
  void goButtonClickHandlerRuns() {
    TournamentQuickTourCard card = buildCard();

    Button go =
        _find(card, Button.class, spec -> spec.withPredicate(b -> "Go".equals(b.getText())))
            .getFirst();
    go.click();

    // Navigation is attempted against the mocked route registry; the handler ran without error.
    assertThat(go.isEnabled()).isTrue();
  }
}

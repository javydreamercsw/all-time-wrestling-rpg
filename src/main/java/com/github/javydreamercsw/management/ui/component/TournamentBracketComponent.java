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

import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO.TournamentMatch;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import java.util.stream.Collectors;

public class TournamentBracketComponent extends HorizontalLayout {

  public TournamentBracketComponent(TournamentDTO tournament) {
    addClassName("tournament-bracket");
    setSpacing(false);
    setAlignItems(Alignment.CENTER);
    setWidthFull();
    getStyle().set("overflow-x", "auto");

    addRound(tournament, 1, "Round of 16");
    addRound(tournament, 2, "Quarter-Finals");
    addRound(tournament, 3, "Semi-Finals");
    addRound(tournament, 4, "Finals");
    addWinner(tournament);
  }

  private void addRound(TournamentDTO tournament, int round, String title) {
    VerticalLayout roundCol = new VerticalLayout();
    roundCol.setPadding(false);
    roundCol.setSpacing(true);
    roundCol.setWidth("200px");
    roundCol.addClassNames(LumoUtility.Margin.Horizontal.SMALL);

    Span roundTitle = new Span(title);
    roundTitle.addClassNames(
        LumoUtility.FontSize.SMALL,
        LumoUtility.FontWeight.BOLD,
        LumoUtility.TextColor.SECONDARY,
        LumoUtility.TextAlignment.CENTER,
        LumoUtility.Width.FULL);
    roundCol.add(roundTitle);

    List<TournamentMatch> matches =
        tournament.getMatches().stream()
            .filter(m -> m.getRound() == round)
            .collect(Collectors.toList());

    for (TournamentMatch match : matches) {
      roundCol.add(createMatchCard(match));
      // Add spacer for visual tree structure
      if (round < 4) {
        Div spacer = new Div();
        spacer.setHeight(getSpacerHeight(round));
        roundCol.add(spacer);
      }
    }

    add(roundCol);
  }

  private void addWinner(TournamentDTO tournament) {
    TournamentMatch finals =
        tournament.getMatches().stream().filter(m -> m.getRound() == 4).findFirst().orElse(null);

    if (finals != null && finals.getWinnerId() != null) {
      VerticalLayout winnerCol = new VerticalLayout();
      winnerCol.setPadding(false);
      winnerCol.setAlignItems(Alignment.CENTER);
      winnerCol.setJustifyContentMode(JustifyContentMode.CENTER);

      Span title = new Span("CHAMPION");
      title.addClassNames(LumoUtility.TextColor.SUCCESS, LumoUtility.FontWeight.BOLD);

      String winnerName =
          finals.getWinnerId().equals(finals.getWrestler1Id())
              ? finals.getWrestler1Name()
              : finals.getWrestler2Name();

      Div winnerBox = new Div();
      winnerBox.setText(winnerName);
      winnerBox.addClassNames(
          LumoUtility.Padding.MEDIUM,
          LumoUtility.Background.PRIMARY,
          LumoUtility.TextColor.PRIMARY_CONTRAST,
          LumoUtility.BorderRadius.MEDIUM,
          LumoUtility.FontWeight.BOLD);

      winnerCol.add(title, winnerBox);
      add(winnerCol);
    }
  }

  private Div createMatchCard(TournamentMatch match) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.SMALL,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Padding.XSMALL,
        LumoUtility.FontSize.XSMALL);
    card.setWidthFull();

    // Wrestler 1
    Div w1 = createWrestlerLine(match.getWrestler1Name(), match.getWrestler1Id(), match);
    // Wrestler 2
    Div w2 = createWrestlerLine(match.getWrestler2Name(), match.getWrestler2Id(), match);

    card.add(w1, w2);

    if (match.isPlayerMatch()) {
      card.getStyle().set("border-color", "var(--lumo-primary-color)");
      card.getStyle().set("border-width", "2px");
    }

    return card;
  }

  private Div createWrestlerLine(String name, Long id, TournamentMatch match) {
    Div line = new Div();
    line.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.JustifyContent.BETWEEN,
        LumoUtility.AlignItems.CENTER);
    line.getStyle().set("min-height", "20px");

    Span nameSpan = new Span(name != null ? name : "?");
    if (id != null && match.getWinnerId() != null && id.equals(match.getWinnerId())) {
      nameSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SUCCESS);
    } else if (match.getWinnerId() != null && id != null) {
      nameSpan.addClassNames(LumoUtility.TextColor.DISABLED, "strikethrough");
      nameSpan.getStyle().set("text-decoration", "line-through");
    }

    line.add(nameSpan);
    return line;
  }

  private String getSpacerHeight(int round) {
    switch (round) {
      case 1:
        return "10px";
      case 2:
        return "40px";
      case 3:
        return "100px";
      default:
        return "0px";
    }
  }
}

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
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import com.github.javydreamercsw.management.ui.component.TournamentBracketModel.MatchModel;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TournamentBracketComponent extends HorizontalLayout {

  /**
   * Campaign convenience constructor — wraps {@link TournamentDTO} in a {@link
   * TournamentDTOAdapter}.
   */
  public TournamentBracketComponent(final TournamentDTO tournament) {
    this(new TournamentDTOAdapter(tournament));
  }

  public TournamentBracketComponent(final TournamentBracketModel model) {
    addClassName("tournament-bracket");
    setSpacing(false);
    setAlignItems(Alignment.CENTER);
    setWidthFull();
    getStyle().set("overflow-x", "auto");

    if (model.getRenderMode() == RenderMode.ROUND_ROBIN_GRID) {
      buildRoundRobinGrid(model);
    } else {
      buildTree(model);
    }
  }

  // ── Tree (Single Elimination) ─────────────────────────────────────────────

  private void buildTree(TournamentBracketModel model) {
    int totalRounds = model.getTotalRounds();
    if (totalRounds <= 0) {
      totalRounds = 4;
    }

    for (int i = 1; i <= totalRounds; i++) {
      addTreeRound(model, i, roundLabel(i, totalRounds), totalRounds);
    }
    addWinner(model, totalRounds);
  }

  private void addTreeRound(
      TournamentBracketModel model, int round, String title, int totalRounds) {
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

    List<MatchModel> matches =
        model.getMatches().stream().filter(m -> m.getRound() == round).collect(Collectors.toList());

    for (MatchModel match : matches) {
      roundCol.add(createMatchCard(match));
      if (round < totalRounds) {
        Div spacer = new Div();
        spacer.setHeight("10px");
        roundCol.add(spacer);
      }
    }

    add(roundCol);
  }

  private void addWinner(TournamentBracketModel model, int totalRounds) {
    MatchModel finals =
        model.getMatches().stream()
            .filter(m -> m.getRound() == totalRounds)
            .findFirst()
            .orElse(null);

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

  // ── Round Robin grid ──────────────────────────────────────────────────────

  private void buildRoundRobinGrid(TournamentBracketModel model) {
    VerticalLayout grid = new VerticalLayout();
    grid.setPadding(false);
    grid.setSpacing(true);
    grid.setWidthFull();

    // Group matches by round, preserving round order
    Map<Integer, List<MatchModel>> byRound =
        model.getMatches().stream()
            .sorted(Comparator.comparingInt(MatchModel::getRound))
            .collect(
                Collectors.groupingBy(
                    MatchModel::getRound, LinkedHashMap::new, Collectors.toList()));

    for (Map.Entry<Integer, List<MatchModel>> entry : byRound.entrySet()) {
      Span roundHeader = new Span("Round " + entry.getKey());
      roundHeader.addClassNames(
          LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
      grid.add(roundHeader);

      HorizontalLayout row = new HorizontalLayout();
      row.setSpacing(true);
      row.setAlignItems(Alignment.CENTER);
      for (MatchModel match : entry.getValue()) {
        row.add(createMatchCard(match));
      }
      grid.add(row);
    }

    add(grid);
  }

  // ── Shared match card ─────────────────────────────────────────────────────

  private Div createMatchCard(final MatchModel match) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.SMALL,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Padding.XSMALL,
        LumoUtility.FontSize.XSMALL);
    card.setWidth("180px");

    card.add(createWrestlerLine(match.getWrestler1Name(), match.getWrestler1Id(), match));
    card.add(createWrestlerLine(match.getWrestler2Name(), match.getWrestler2Id(), match));

    if (match.isPlayerMatch()) {
      card.getStyle().set("border-color", "var(--lumo-primary-color)");
      card.getStyle().set("border-width", "2px");
    }

    return card;
  }

  private Div createWrestlerLine(final String name, final Long id, final MatchModel match) {
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
      nameSpan.addClassNames(LumoUtility.TextColor.DISABLED);
      nameSpan.getStyle().set("text-decoration", "line-through");
    }

    line.add(nameSpan);
    return line;
  }

  private static String roundLabel(int round, int totalRounds) {
    int remaining = totalRounds - round;
    return switch (remaining) {
      case 0 -> "Finals";
      case 1 -> "Semi-Finals";
      case 2 -> "Quarter-Finals";
      default -> "Round " + round;
    };
  }
}

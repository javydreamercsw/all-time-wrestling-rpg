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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The tutorial page's "Quick Tour: Run Your First Tournament" card — a stateless booker
 * walkthrough, not a {@code TutorialDefinition}. The mode tutorials (Campaign/League/Universe)
 * create tutorial universes and track per-account completion; this card just points a brand-new
 * player at the tournament flow they can run in a few clicks with no shows, seasons, or AI setup.
 */
public class TournamentQuickTourCard extends Div {

  public static final String TOURNAMENT_LIST_ROUTE = "tournament-list";

  public TournamentQuickTourCard() {
    addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.Padding.LARGE,
        LumoUtility.BoxShadow.SMALL);
    getStyle().set("border", "2px solid transparent");
    getStyle().set("flex", "1");
    getStyle().set("transition", "box-shadow 0.2s");
    getElement()
        .addEventListener(
            "mouseenter", e -> getStyle().set("border-color", "var(--lumo-primary-color)"))
        .synchronizeProperty("style");
    getElement()
        .addEventListener("mouseleave", e -> getStyle().set("border-color", "transparent"))
        .synchronizeProperty("style");

    Span iconSpan = new Span(VaadinIcon.TROPHY.create());
    iconSpan.getStyle().set("font-size", "2rem").set("color", "var(--lumo-primary-color)");

    HorizontalLayout titleRow = new HorizontalLayout();
    titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
    titleRow.setSpacing(true);
    titleRow.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Margin.Bottom.XSMALL);

    H3 cardTitle = new H3("Quick Tour: Run Your First Tournament");
    cardTitle.getStyle().set("margin", "0");
    titleRow.add(cardTitle);

    Span badge = new Span("No setup needed");
    badge.getElement().getThemeList().add("badge");
    badge.getElement().getThemeList().add("success");
    badge.getStyle().set("font-size", "var(--lumo-font-size-xs)");
    titleRow.add(badge);

    Paragraph intro =
        new Paragraph(
            "Skip the universe modes for now — you can run a full tournament in a few clicks,"
                + " with no shows, seasons, or AI configuration. The matches are real: health,"
                + " stamina, fans and injuries all apply.");
    intro.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
    intro.getStyle().set("margin", "0");

    add(iconSpan, titleRow, intro, buildSteps());
  }

  private VerticalLayout buildSteps() {
    Map<String, String> steps = new LinkedHashMap<>();
    steps.put(
        "1. Create a Tournament",
        "Open Tournaments and use New Tournament. Pick a format and a few entrants — your"
            + " roster is ready.");
    steps.put(
        "2. Seed and Start",
        "Randomize the seeds if you like, then click Start Tournament to generate the bracket.");
    steps.put(
        "3. Run It",
        "On the tournament page, Run Round or Run Tournament. No shows? A '<tournament>"
            + " Night' show is created for you automatically.");
    steps.put(
        "4. Crown the Winner",
        "Trophies, briefcases and title opportunities are granted automatically when the"
            + " bracket completes.");

    VerticalLayout stepsLayout = new VerticalLayout();
    stepsLayout.setPadding(false);
    stepsLayout.setSpacing(false);

    steps.forEach(
        (title, description) -> {
          Span stepTitle = new Span(title);
          stepTitle.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
          Paragraph stepDesc = new Paragraph(description);
          stepDesc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
          stepDesc.getStyle().set("margin", "0 0 0.25rem 0");

          Button go = new Button("Go", e -> navigateToTournaments());
          go.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

          HorizontalLayout row = new HorizontalLayout(stepTitle, go);
          row.setAlignItems(FlexComponent.Alignment.CENTER);
          row.setWidthFull();
          stepsLayout.add(row, stepDesc);
        });

    stepsLayout.addClassNames(LumoUtility.Margin.Top.SMALL);
    return stepsLayout;
  }

  private void navigateToTournaments() {
    getUI().ifPresent(ui -> ui.navigate(TOURNAMENT_LIST_ROUTE));
  }
}

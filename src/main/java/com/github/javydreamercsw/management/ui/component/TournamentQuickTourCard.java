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
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * The tutorial page's "Quick Tour: Run Your First Tournament" teaser card. The interactive tour — a
 * five-step wizard that performs each step for the user (create, add players, seed+start, record
 * results, crown) — lives at {@code tournament-tour} (ATW-6h2y); this card points a brand-new
 * player at it. Unlike the mode tutorials it tracks no per-account completion.
 */
public class TournamentQuickTourCard extends Div {

  public static final String TOURNAMENT_LIST_ROUTE = "tournament-list";
  public static final String TOUR_ROUTE = "tournament-tour";

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
            "A five-step guided tour that does the work for you: creates the tournament, walks"
                + " player setup, seeds and starts the bracket, records results, and crowns the"
                + " winner. No shows, seasons, or AI configuration needed — and the matches are"
                + " real: health, stamina, fans and injuries all apply.");
    intro.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
    intro.getStyle().set("margin", "0");

    Button startTour = new Button("Start the interactive tour", e -> navigateTo(TOUR_ROUTE));
    startTour.setId("quick-tour-start-button");
    startTour.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    startTour.addClassNames(LumoUtility.Margin.Top.SMALL);

    add(iconSpan, titleRow, intro, startTour);
  }

  private void navigateTo(String route) {
    getUI().ifPresent(ui -> ui.navigate(route));
  }
}

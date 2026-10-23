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
import java.util.List;

/**
 * The tutorial page's "Quick Tour: Run Your First Tournament" card — a stateless booker
 * walkthrough, not a {@code TutorialDefinition}. The mode tutorials (Campaign/League/Universe)
 * create tutorial universes and track per-account completion; this card just points a brand-new
 * player at the tournament flow they can run in a few clicks with no shows, seasons, or AI setup.
 *
 * <p>The five steps render one at a time as an in-card stepper (ATW-eqj5): a "Step N of 5"
 * indicator with progress dots, the current step's title and description, and Back / Go / Next.
 * Progress is session-only (a field on the component instance) — a reload restarts the tour,
 * matching the card's stateless design.
 */
public class TournamentQuickTourCard extends Div {

  public static final String TOURNAMENT_LIST_ROUTE = "tournament-list";

  private final List<Step> steps =
      List.of(
          new Step(
              "1. Create a Tournament",
              "Open Tournaments and use New Tournament. Pick a format and a few entrants —"
                  + " your roster is ready.",
              TOURNAMENT_LIST_ROUTE),
          new Step(
              "2. Add Players",
              "Create player accounts under Accounts and assign each player their wrestler —"
                  + " their wrestler's results then count for their account.",
              "account-list"),
          new Step(
              "3. Seed and Start",
              "Randomize the seeds if you like, then click Start Tournament to generate the"
                  + " bracket.",
              TOURNAMENT_LIST_ROUTE),
          new Step(
              "4. Enter Results",
              "On the tournament page, pick the winner of each match yourself — or let the"
                  + " engine decide a single match. No shows? A '<tournament> Night' show is"
                  + " created for you automatically.",
              TOURNAMENT_LIST_ROUTE),
          new Step(
              "5. Crown the Winner",
              "Trophies, briefcases and title opportunities are granted automatically when"
                  + " the bracket completes.",
              TOURNAMENT_LIST_ROUTE));

  private final Span stepIndicator = new Span();
  private final Span stepTitle = new Span();
  private final Paragraph stepDescription = new Paragraph();
  private final Button backButton = new Button("← Back");
  private final Button goButton = new Button("Go");
  private final Button nextButton = new Button("Next →");

  private int currentStepIndex;

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

    add(iconSpan, titleRow, intro, buildStepper());
    renderStep();
  }

  private VerticalLayout buildStepper() {
    stepIndicator.setId("quick-tour-step-indicator");
    stepIndicator.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

    HorizontalLayout indicatorRow = new HorizontalLayout(stepIndicator, buildProgressDots());
    indicatorRow.setAlignItems(FlexComponent.Alignment.CENTER);
    indicatorRow.setSpacing(true);
    indicatorRow.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Margin.Bottom.XSMALL);

    stepTitle.setId("quick-tour-step-title");
    stepTitle.addClassNames(LumoUtility.FontWeight.BOLD);

    stepDescription.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
    stepDescription.getStyle().set("margin", "0");

    backButton.setId("back-button");
    backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    backButton.addClickListener(
        e -> {
          if (currentStepIndex > 0) {
            currentStepIndex--;
            renderStep();
          }
        });

    goButton.setId("go-button");
    goButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
    goButton.addClickListener(e -> navigateTo(steps.get(currentStepIndex).route()));

    nextButton.setId("next-button");
    nextButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    nextButton.addClickListener(
        e -> {
          if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
            renderStep();
          }
        });

    HorizontalLayout navRow = new HorizontalLayout(backButton, goButton, nextButton);
    navRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    navRow.setWidthFull();
    navRow.setSpacing(true);
    navRow.addClassNames(LumoUtility.Margin.Top.SMALL);

    VerticalLayout stepper = new VerticalLayout(indicatorRow, stepTitle, stepDescription, navRow);
    stepper.setPadding(false);
    stepper.setSpacing(false);
    stepper.addClassNames(LumoUtility.Margin.Top.SMALL);
    return stepper;
  }

  private HorizontalLayout buildProgressDots() {
    HorizontalLayout dots = new HorizontalLayout();
    dots.setSpacing(true);
    dots.getStyle().set("line-height", "1");
    for (int i = 0; i < steps.size(); i++) {
      Span dot = new Span();
      dot.setWidth("8px");
      dot.setHeight("8px");
      dot.getStyle()
          .set("border-radius", "50%")
          .set(
              "background",
              i == currentStepIndex ? "var(--lumo-primary-color)" : "var(--lumo-contrast-20pct)");
      dots.add(dot);
    }
    return dots;
  }

  private void renderStep() {
    Step step = steps.get(currentStepIndex);
    stepIndicator.setText("Step " + (currentStepIndex + 1) + " of " + steps.size());
    stepTitle.setText(step.title());
    stepDescription.setText(step.description());

    backButton.setVisible(currentStepIndex > 0);
    nextButton.setVisible(currentStepIndex < steps.size() - 1);

    // Re-render the dots: simplest is to rebuild the indicator row's dot layout.
    HorizontalLayout indicatorRow = (HorizontalLayout) stepIndicator.getParent().orElseThrow();
    indicatorRow.removeAll();
    indicatorRow.add(stepIndicator, buildProgressDots());
  }

  private void navigateTo(String route) {
    getUI().ifPresent(ui -> ui.navigate(route));
  }

  /** One Quick Tour step: what to do, how, and where the Go button lands. */
  private record Step(String title, String description, String route) {}
}

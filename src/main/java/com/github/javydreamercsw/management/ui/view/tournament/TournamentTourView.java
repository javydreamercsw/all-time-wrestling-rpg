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
package com.github.javydreamercsw.management.ui.view.tournament;

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.tournament.TournamentRunnerService;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 * The interactive tournament tour (ATW-6h2y): a five-step wizard that performs each step for the
 * user, mirroring the mode tutorials' inline pattern — real inputs, real service calls, real
 * advance. Step 1 creates the tournament, step 2 assigns players, step 3 seeds and starts, step 4
 * records results, step 5 crowns the winner. Progress is session-only (view fields): a reload
 * restarts the tour; the underlying work persists.
 */
@Route(value = "tournament-tour", layout = MainLayout.class)
@PageTitle("Tournament Tour")
@RolesAllowed({"ADMIN", "BOOKER", "PLAYER", "VIEWER"})
@Slf4j
public class TournamentTourView extends VerticalLayout implements BeforeEnterObserver {

  private final TournamentService tournamentService;
  private final TournamentRunnerService tournamentRunnerService;
  private final WrestlerFacade wrestlerFacade;
  private final ShowFacade showFacade;
  private final UniverseContextService universeContextService;

  private final H2 stepTitle = new H2();
  private final VerticalLayout stepContent = new VerticalLayout();
  private final Span statusLine = new Span();
  private final Button backButton = new Button("← Back");
  private final Button nextButton = new Button("Next →");

  private int currentStep;
  private Long tournamentId;
  private IntegerField entrantCount;

  public TournamentTourView(
      final TournamentService tournamentService,
      final TournamentRunnerService tournamentRunnerService,
      final WrestlerFacade wrestlerFacade,
      final ShowFacade showFacade,
      final UniverseContextService universeContextService) {
    this.tournamentService = tournamentService;
    this.tournamentRunnerService = tournamentRunnerService;
    this.wrestlerFacade = wrestlerFacade;
    this.showFacade = showFacade;
    this.universeContextService = universeContextService;

    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassNames(LumoUtility.MaxWidth.SCREEN_MEDIUM, LumoUtility.Margin.Horizontal.AUTO);

    stepTitle.setId("tour-step-title");
    add(stepTitle);

    stepContent.setPadding(false);
    add(stepContent);

    statusLine.setId("tour-status");
    statusLine.setWidthFull();
    add(statusLine);

    backButton.setId("back-button");
    backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    backButton.addClickListener(
        e -> {
          if (currentStep > 1) {
            currentStep--;
            renderStep();
          }
        });
    nextButton.setId("next-button");
    nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    nextButton.addClickListener(
        e -> {
          if (currentStep < 5) {
            currentStep++;
            renderStep();
          }
        });

    HorizontalLayout nav = new HorizontalLayout(backButton, nextButton);
    nav.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    nav.setWidthFull();
    add(nav);
  }

  @Override
  public void beforeEnter(final BeforeEnterEvent event) {
    if (currentStep == 0) {
      currentStep = 1;
    }
    renderStep();
  }

  void renderStep() {
    stepContent.removeAll();
    statusLine.setText("");
    switch (currentStep) {
      case 1 -> renderCreateStep();
      case 2 -> renderPlayersStep();
      case 3 -> renderStartStep();
      case 4 -> renderResultsStep();
      case 5 -> renderCrownStep();
      default -> {
        /* unreachable */
      }
    }
    backButton.setVisible(currentStep > 1);
    nextButton.setVisible(currentStep < 5 && currentStep != 1);
    stepTitle.setText("Step " + currentStep + " of 5 — " + stepName());
  }

  private String stepName() {
    return switch (currentStep) {
      case 1 -> "Create a Tournament";
      case 2 -> "Add Players";
      case 3 -> "Seed and Start";
      case 4 -> "Enter Results";
      default -> "Crown the Winner";
    };
  }

  // ── Step 1: Create a Tournament ───────────────────────────────────────────

  private void renderCreateStep() {
    stepContent.add(
        new Paragraph(
            "Open the New Tournament wizard and configure your bracket — format, payoff,"
                + " entrants and seeding are all yours to choose. Nothing is decided for you."));

    Button create = new Button("Open the Creation Wizard", e -> openWizard());
    create.setId("tour-create-button");
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    stepContent.add(create);
  }

  private void openWizard() {
    TournamentCreationWizardDialog wizard =
        new TournamentCreationWizardDialog(
            tournamentService,
            wrestlerFacade,
            showFacade,
            universeContextService,
            wrestlerFacade.getTeamService(),
            created -> {
              tournamentId = created.getId();
              currentStep = 2;
              renderStep();
              showStatus("✓ \"" + created.getName() + "\" created. Next: add players.", true);
            },
            // The tour elevates every wizard write through the admin bridge, like all tour
            // actions — PLAYERs can complete this step without write grants of their own.
            action -> runUnderAdmin(action));
    wizard.open();
  }

  // ── Step 2: Add Players ───────────────────────────────────────────────────

  private void renderPlayersStep() {
    stepContent.add(
        new Paragraph(
            "Players get an account with an assigned wrestler, so match results count toward"
                + " their record. Create accounts under Accounts — or, if you're playing solo,"
                + " skip ahead: the engine can decide every match."));
    Paragraph hint = new Paragraph("(Full account management lives under Accounts.)");
    hint.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
    stepContent.add(hint);
  }

  // ── Step 3: Seed and Start ────────────────────────────────────────────────

  private void renderStartStep() {
    Tournament t = tournamentService.findByIdWithDetails(tournamentId).orElse(null);
    if (t == null) {
      showStatus("Tournament not found — go back and create it.", false);
      return;
    }
    boolean alreadySeeded = !t.getEntries().isEmpty();
    if (alreadySeeded) {
      stepContent.add(
          new Paragraph(
              "Your wizard seeding is in place ("
                  + t.getEntries().size()
                  + " entrants). Starting the tournament generates round 1 — ready to record."));
    } else {
      stepContent.add(
          new Paragraph(
              "Choose how many wrestlers to seed — the top entrants by fan count fill the"
                  + " bracket, then the tournament starts and round 1 is ready to record."));
      entrantCount = new IntegerField("Number of wrestlers");
      entrantCount.setId("tour-entrant-count");
      entrantCount.setValue(8);
      entrantCount.setMin(3);
      entrantCount.setMax(64);
      entrantCount.setWidthFull();
      stepContent.add(entrantCount);
    }
    Button start =
        new Button(alreadySeeded ? "Start Tournament" : "Seed & Start", e -> seedAndStart());
    start.setId("tour-start-button");
    start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    stepContent.add(start);
  }

  private void seedAndStart() {
    tournamentService
        .findByIdWithDetails(tournamentId)
        .ifPresentOrElse(
            t -> {
              try {
                runUnderAdmin(
                    () -> {
                      if (entrantCount != null && entrantCount.isVisible()) {
                        tournamentService.seedAuto(
                            t,
                            entrantCount.getValue(),
                            universeContextService.getCurrentUniverseId());
                      }
                      tournamentService.startTournament(t);
                    });
                showStatus("✓ Bracket generated. Next: record results.", true);
                currentStep = 4;
                renderStep();
              } catch (RuntimeException e) {
                log.warn("Tour seed/start failed", e);
                showStatus("Could not start: " + e.getMessage(), false);
              }
            },
            () -> showStatus("Tournament not found — go back and create it.", false));
  }

  // ── Step 4: Enter Results ─────────────────────────────────────────────────

  private void renderResultsStep() {
    Tournament t = tournamentService.findByIdWithDetails(tournamentId).orElse(null);
    if (t == null) {
      showStatus("Tournament not found — go back and create it.", false);
      return;
    }
    boolean anyOpen = false;
    for (var round : t.getRounds()) {
      for (TournamentMatch match : round.getMatches()) {
        if (match.getWinner() == null) {
          anyOpen = true;
          stepContent.add(buildMatchRow(t, round.getRoundName(), match));
        }
      }
    }
    if (!anyOpen) {
      showStatus("All matches decided — continue to crown the winner.", true);
    }
  }

  private HorizontalLayout buildMatchRow(Tournament t, String roundName, TournamentMatch match) {
    Span label =
        new Span(
            roundName
                + ": "
                + match.getEntrant1().entrantName()
                + " vs "
                + match.getEntrant2().entrantName());
    label.getStyle().set("flex", "1");

    Button record =
        new Button(
            "Winner: " + match.getEntrant1().entrantName(),
            e -> recordManual(t, match, match.getEntrant1()));
    record.setId("tour-record-button-" + match.getId());
    record.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

    Button engine = new Button("Decide by Engine", e -> decideByEngine(t, match));
    engine.setId("tour-engine-button-" + match.getId());
    engine.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);

    HorizontalLayout row = new HorizontalLayout(label, record, engine);
    row.setAlignItems(FlexComponent.Alignment.CENTER);
    row.setWidthFull();
    return row;
  }

  private void recordManual(Tournament t, TournamentMatch match, TournamentEntry winner) {
    try {
      runUnderAdmin(
          () -> tournamentRunnerService.recordManualResult(t, match.getId(), winner.getId(), null));
      showStatus("✓ " + winner.entrantName() + " advances.", true);
      renderStep();
    } catch (RuntimeException e) {
      log.warn("Tour manual result failed", e);
      showStatus("Could not record: " + e.getMessage(), false);
    }
  }

  private void decideByEngine(Tournament t, TournamentMatch match) {
    try {
      runUnderAdmin(() -> tournamentRunnerService.runEngineMatch(t, match.getId(), null));
      showStatus("✓ Engine decided the match.", true);
      renderStep();
    } catch (RuntimeException e) {
      log.warn("Tour engine match failed", e);
      showStatus("Could not decide: " + e.getMessage(), false);
    }
  }

  // ── Step 5: Crown the Winner ──────────────────────────────────────────────

  private void renderCrownStep() {
    Tournament t = tournamentService.findByIdWithDetails(tournamentId).orElse(null);
    if (t != null && t.getStatus() == TournamentStatus.COMPLETE) {
      stepContent.add(
          new Paragraph(
              "🏆 The tournament is complete! Trophies, briefcases and title opportunities were"
                  + " granted automatically — open the tournament page to see the final bracket"
                  + " and who took the crown."));
    } else {
      stepContent.add(
          new Paragraph(
              "Your tournament is set up. Record the remaining results (Step 4) and the winner"
                  + " is crowned automatically — trophies, briefcases and title opportunities"
                  + " grant themselves."));
    }
    Button openList =
        new Button(
            "Open Tournaments", e -> getUI().ifPresent(ui -> ui.navigate("tournament-list")));
    openList.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    stepContent.add(openList);
  }

  // ── Shared helpers ────────────────────────────────────────────────────────

  private void showStatus(String message, boolean success) {
    statusLine.setText(message);
    statusLine
        .getStyle()
        .set("color", success ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
    if (success) {
      Notification.show(message, 3000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
  }

  /** Tour actions run under the admin bridge, like every mode-tutorial step action. */
  private void runUnderAdmin(Runnable action) {
    GeneralSecurityUtils.runAsAdmin(action);
  }
}

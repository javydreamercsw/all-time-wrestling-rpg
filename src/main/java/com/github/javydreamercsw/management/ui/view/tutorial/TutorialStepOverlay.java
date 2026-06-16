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
package com.github.javydreamercsw.management.ui.view.tutorial;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.service.tutorial.TutorialStep;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Floating non-modal overlay that guides the user through a tutorial step while they navigate the
 * app. Attach once to the UI via MainLayout; call {@link #updateStep} on each navigation to keep
 * the panel in sync with the current step.
 */
public class TutorialStepOverlay extends Dialog {

  private final TutorialService tutorialService;
  private final AccountService accountService;

  private Account account;
  private Universe.UniverseType universeType;
  private int currentStepIndex;
  private int totalSteps;

  private final H4 titleLabel = new H4();
  private final Span stepCountLabel = new Span();
  private final ProgressBar progressBar = new ProgressBar();
  private final Paragraph instructionsLabel = new Paragraph();
  private final Div errorSlot = new Div();
  private final VerticalLayout body = new VerticalLayout();
  private final HorizontalLayout footer = new HorizontalLayout();
  private final Button goBtn = new Button();
  private final Button nextBtn = new Button();
  private boolean minimized = false;

  public TutorialStepOverlay(
      final TutorialService tutorialService, final AccountService accountService) {
    this.tutorialService = tutorialService;
    this.accountService = accountService;

    setModal(false);
    setDraggable(true);
    setResizable(false);
    setCloseOnEsc(false);
    setCloseOnOutsideClick(false);
    setWidth("340px");

    getElement()
        .getStyle()
        .set("position", "fixed")
        .set("bottom", "24px")
        .set("right", "24px")
        .set("z-index", "1000")
        .set("box-shadow", "0 4px 20px rgba(0,0,0,0.25)");

    buildHeader();
    buildBody();
    buildFooter();
  }

  private void buildHeader() {
    stepCountLabel.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

    progressBar.setWidth("120px");
    progressBar.setHeight("6px");

    Button minimizeBtn = new Button(VaadinIcon.MINUS.create());
    minimizeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    minimizeBtn.setTooltipText("Minimize");
    minimizeBtn.addClickListener(e -> toggleMinimize());

    Button skipBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
    skipBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    skipBtn.setTooltipText("Skip tutorial");
    skipBtn.addClickListener(
        e -> {
          if (account != null && universeType != null) {
            tutorialService.markSkipped(account.getId(), universeType, totalSteps);
          }
          close();
        });

    HorizontalLayout controls = new HorizontalLayout(minimizeBtn, skipBtn);
    controls.setSpacing(false);
    controls.addClassNames(LumoUtility.Margin.Left.AUTO);

    titleLabel.addClassNames(
        LumoUtility.Margin.NONE, LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.SEMIBOLD);

    HorizontalLayout headerRow = new HorizontalLayout(titleLabel, controls);
    headerRow.setWidthFull();
    headerRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
    headerRow.setSpacing(false);

    HorizontalLayout progressRow = new HorizontalLayout(stepCountLabel, progressBar);
    progressRow.setAlignItems(
        com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
    progressRow.setSpacing(true);

    VerticalLayout header = new VerticalLayout(headerRow, progressRow);
    header.setPadding(false);
    header.setSpacing(false);
    getHeader().add(header);
  }

  private void buildBody() {
    instructionsLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.Margin.NONE);

    errorSlot.setVisible(false);
    errorSlot.addClassNames(
        LumoUtility.TextColor.ERROR, LumoUtility.FontSize.XSMALL, LumoUtility.Margin.Top.SMALL);

    body.setPadding(false);
    body.setSpacing(true);
    body.add(instructionsLabel, errorSlot);
    add(body);
  }

  private void buildFooter() {
    goBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);

    nextBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    nextBtn.addClickListener(e -> handleNext());

    footer.add(goBtn, nextBtn);
    footer.setSpacing(true);
    footer.setWidthFull();
    getFooter().add(footer);
  }

  /**
   * Update the overlay to reflect the given step. Call this from {@code afterNavigation} whenever
   * the tutorial state may have changed.
   */
  public void updateStep(
      final Account account,
      final Universe.UniverseType universeType,
      final int stepIndex,
      final int totalSteps) {
    this.account = account;
    this.universeType = universeType;
    this.currentStepIndex = stepIndex;
    this.totalSteps = totalSteps;

    TutorialStep step = tutorialService.getDefinition(universeType).getSteps().get(stepIndex);

    titleLabel.setText(step.getTitle());
    stepCountLabel.setText("Step " + (stepIndex + 1) + " of " + totalSteps);
    progressBar.setMin(0);
    progressBar.setMax(totalSteps);
    progressBar.setValue(stepIndex + 1);

    instructionsLabel.setText(step.getInstructions());
    errorSlot.setVisible(false);

    goBtn.setText("Go to " + step.getTargetViewLabel() + " ↗");
    goBtn.addClickListener(ev -> UI.getCurrent().navigate(step.getTargetRoute()));

    boolean isLast = stepIndex == totalSteps - 1;
    nextBtn.setText(isLast ? "Complete ✓" : "Next →");

    if (minimized) {
      body.setVisible(false);
      footer.setVisible(false);
    }

    if (!isOpened()) {
      open();
    }
  }

  private void handleNext() {
    account = accountService.get(account.getId()).orElse(account);
    String error = tutorialService.validateStep(account, universeType, currentStepIndex);
    if (error != null) {
      errorSlot.setText("⚠ " + error);
      errorSlot.setVisible(true);
      return;
    }

    errorSlot.setVisible(false);
    tutorialService.runAfterStep(account, universeType, currentStepIndex);
    int nextStep = currentStepIndex + 1;
    tutorialService.advanceStep(account.getId(), universeType, nextStep, totalSteps);

    if (nextStep >= totalSteps) {
      close();
      Notification.show("🏆 Tutorial complete! Great work.", 4000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      String completionRoute = tutorialService.getDefinition(universeType).getCompletionRoute();
      UI.getCurrent().navigate(completionRoute);
    } else {
      // Refresh account and move to next step
      account = accountService.get(account.getId()).orElse(account);
      tutorialService.runBeforeStep(account, universeType, nextStep);

      TutorialStep next = tutorialService.getDefinition(universeType).getSteps().get(nextStep);

      if (next.getInteractionMode() == TutorialStep.InteractionMode.INLINE) {
        // Hand back to TutorialView for inline steps (e.g. wrestler picker)
        close();
        UI.getCurrent().navigate("tutorial");
      } else {
        updateStep(account, universeType, nextStep, totalSteps);
        UI.getCurrent().navigate(next.getTargetRoute());
      }
    }
  }

  private void toggleMinimize() {
    minimized = !minimized;
    body.setVisible(!minimized);
    footer.setVisible(!minimized);
  }
}

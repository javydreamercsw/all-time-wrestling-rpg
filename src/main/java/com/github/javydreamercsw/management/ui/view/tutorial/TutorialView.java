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

import static com.github.javydreamercsw.base.domain.account.RoleName.ADMIN_ROLE;
import static com.github.javydreamercsw.base.domain.account.RoleName.BOOKER_ROLE;
import static com.github.javydreamercsw.base.domain.account.RoleName.PLAYER_ROLE;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.tutorial.TutorialDefinition;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.service.tutorial.TutorialStep;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "tutorial", layout = MainLayout.class)
@PageTitle("Tutorial")
@RolesAllowed({ADMIN_ROLE, BOOKER_ROLE, PLAYER_ROLE})
public class TutorialView extends VerticalLayout implements BeforeEnterObserver {

  private final TutorialService tutorialService;
  private final SecurityUtils securityUtils;
  private final UniverseContextService universeContextService;
  private final AccountService accountService;

  private Account account;
  private Universe.UniverseType universeType;
  private TutorialDefinition definition;
  private int currentStepIndex;

  public TutorialView(
      final TutorialService tutorialService,
      final SecurityUtils securityUtils,
      final UniverseContextService universeContextService,
      final AccountService accountService) {
    this.tutorialService = tutorialService;
    this.securityUtils = securityUtils;
    this.universeContextService = universeContextService;
    this.accountService = accountService;
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassNames(LumoUtility.MaxWidth.SCREEN_MEDIUM, LumoUtility.Margin.Horizontal.AUTO);
  }

  @Override
  public void beforeEnter(final BeforeEnterEvent event) {
    universeType =
        universeContextService
            .getCurrentUniverse()
            .map(Universe::getType)
            .orElse(Universe.UniverseType.GLOBAL);

    account = securityUtils.getCurrentAccountId().flatMap(accountService::get).orElse(null);

    if (account == null) {
      event.rerouteTo("");
      return;
    }

    definition = tutorialService.getDefinition(universeType);
    currentStepIndex = tutorialService.getCurrentStep(account.getId(), universeType);

    if (currentStepIndex >= definition.getSteps().size()) {
      currentStepIndex = definition.getSteps().size() - 1;
    }

    renderStep();
  }

  private void renderStep() {
    removeAll();

    TutorialStep step = definition.getSteps().get(currentStepIndex);
    int totalSteps = definition.getSteps().size();

    tutorialService.runBeforeStep(account, universeType, currentStepIndex);

    // ── Header row ────────────────────────────────────────────────────────
    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setAlignItems(Alignment.CENTER);

    ProgressBar progress = new ProgressBar(0, totalSteps, currentStepIndex + 1);
    progress.setWidth("200px");

    Span stepLabel = new Span("Step " + (currentStepIndex + 1) + " of " + totalSteps);
    stepLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

    Button skipButton = new Button("Skip Tutorial");
    skipButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    skipButton.addClickListener(
        e -> {
          tutorialService.markSkipped(account.getId(), universeType, totalSteps);
          navigateToDashboard();
        });
    skipButton.addClassNames(LumoUtility.Margin.Left.AUTO);

    header.add(stepLabel, progress, skipButton);
    add(header);

    // ── Step title ────────────────────────────────────────────────────────
    H2 title = new H2(step.getTitle());
    title.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.SMALL);
    add(title);

    // ── Instructions ──────────────────────────────────────────────────────
    Paragraph instructions = new Paragraph(step.getInstructions());
    instructions.addClassNames(LumoUtility.FontSize.MEDIUM);
    add(instructions);

    // ── Screenshot image ─────────────────────────────────────────────────
    if (step.getImagePath() != null) {
      Div imageContainer = new Div();
      imageContainer.addClassNames(
          LumoUtility.Margin.Vertical.MEDIUM,
          LumoUtility.BorderRadius.MEDIUM,
          LumoUtility.Background.CONTRAST_5);
      imageContainer.setHeight("220px");
      imageContainer
          .getStyle()
          .set("overflow", "hidden")
          .set("display", "flex")
          .set("align-items", "center")
          .set("justify-content", "center");

      Image img = new Image(step.getImagePath(), step.getTitle());
      img.setMaxHeight("220px");
      img.setMaxWidth("100%");
      imageContainer.add(img);
      add(imageContainer);
    }

    // ── Validation hint ───────────────────────────────────────────────────
    Span hint = new Span(step.getValidationHint());
    hint.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
    hint.getStyle().set("font-style", "italic");
    add(hint);

    // ── Error slot ────────────────────────────────────────────────────────
    Div errorSlot = new Div();
    errorSlot.setVisible(false);
    errorSlot.addClassNames(
        LumoUtility.TextColor.ERROR, LumoUtility.Margin.Top.SMALL, LumoUtility.FontSize.SMALL);
    add(errorSlot);

    // ── Button row ────────────────────────────────────────────────────────
    HorizontalLayout buttons = new HorizontalLayout();
    buttons.addClassNames(LumoUtility.Margin.Top.MEDIUM);
    buttons.setSpacing(true);

    if (currentStepIndex > 0) {
      Button prevButton = new Button("◀ Previous");
      prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
      prevButton.addClickListener(
          e -> {
            tutorialService.advanceStep(
                account.getId(), universeType, currentStepIndex - 1, totalSteps);
            currentStepIndex--;
            renderStep();
          });
      buttons.add(prevButton);
    }

    Button goButton = new Button("Go to " + step.getTargetViewLabel() + " ↗");
    goButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    goButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(step.getTargetRoute())));
    buttons.add(goButton);

    boolean isLastStep = (currentStepIndex == totalSteps - 1);
    Button validateButton = new Button(isLastStep ? "✓ Complete Tutorial" : "✓ Validate");
    validateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    validateButton.addClickListener(
        e -> {
          String error = step.validate(account);
          if (error != null) {
            errorSlot.setText("⚠ " + error);
            errorSlot.setVisible(true);
          } else {
            errorSlot.setVisible(false);
            tutorialService.runAfterStep(account, universeType, currentStepIndex);
            int nextStep = currentStepIndex + 1;
            tutorialService.advanceStep(account.getId(), universeType, nextStep, totalSteps);
            if (nextStep >= totalSteps) {
              showCompletionScreen();
            } else {
              currentStepIndex = nextStep;
              Notification.show("✅ Step complete! Keep going.")
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              renderStep();
            }
          }
        });
    buttons.add(validateButton);

    add(buttons);
  }

  private void showCompletionScreen() {
    removeAll();

    VerticalLayout completionCard = new VerticalLayout();
    completionCard.addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.Padding.LARGE,
        LumoUtility.BoxShadow.SMALL);
    completionCard.setAlignItems(Alignment.CENTER);

    Span trophy = new Span("🏆");
    trophy.getStyle().set("font-size", "4rem");

    H2 congrats = new H2("Tutorial Complete!");
    congrats.addClassNames(LumoUtility.Margin.NONE);

    Paragraph message =
        new Paragraph(
            "You've completed the "
                + universeType.name().charAt(0)
                + universeType.name().substring(1).toLowerCase()
                + " tutorial. You're ready to start playing!");
    message.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.TextAlignment.CENTER);

    Button dashboardButton = new Button("Go to Dashboard");
    dashboardButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
    dashboardButton.addClickListener(e -> navigateToDashboard());

    completionCard.add(trophy, congrats, message, dashboardButton);
    add(completionCard);

    // Auto-navigate after 3 seconds
    UI ui = UI.getCurrent();
    if (ui != null) {
      ui.getPage()
          .executeJs(
              "setTimeout(() => { window.location.href = '" + getDashboardRoute() + "'; }, 3000);");
    }
  }

  private void navigateToDashboard() {
    getUI().ifPresent(ui -> ui.navigate(getDashboardRoute()));
  }

  private String getDashboardRoute() {
    if (securityUtils.isPlayer()) return "player";
    if (securityUtils.isBooker()) return "booker-dashboard";
    return "";
  }
}

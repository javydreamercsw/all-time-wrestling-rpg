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

import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.tutorial.TutorialDefinition;
import com.github.javydreamercsw.management.service.tutorial.TutorialService;
import com.github.javydreamercsw.management.service.tutorial.TutorialStep;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "tutorial", layout = MainLayout.class)
@PageTitle("Tutorial")
@RolesAllowed({ADMIN_ROLE, BOOKER_ROLE, PLAYER_ROLE})
public class TutorialView extends VerticalLayout implements BeforeEnterObserver {

  private enum ViewPhase {
    SETUP_MODE,
    SETUP_FEATURES,
    WIZARD
  }

  private final TutorialService tutorialService;
  private final SecurityUtils securityUtils;
  private final UniverseContextService universeContextService;
  private final AccountService accountService;
  private final WrestlerService wrestlerService;
  private final AiSettingsService aiSettingsService;

  private Account account;
  private Universe.UniverseType universeType;
  private Universe.UniverseType selectedMode;
  private TutorialDefinition definition;
  private int currentStepIndex;
  private ViewPhase currentPhase;

  public TutorialView(
      final TutorialService tutorialService,
      final SecurityUtils securityUtils,
      final UniverseContextService universeContextService,
      final AccountService accountService,
      final WrestlerService wrestlerService,
      final AiSettingsService aiSettingsService) {
    this.tutorialService = tutorialService;
    this.securityUtils = securityUtils;
    this.universeContextService = universeContextService;
    this.accountService = accountService;
    this.wrestlerService = wrestlerService;
    this.aiSettingsService = aiSettingsService;
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassNames(LumoUtility.MaxWidth.SCREEN_MEDIUM, LumoUtility.Margin.Horizontal.AUTO);
  }

  @Override
  public void beforeEnter(final BeforeEnterEvent event) {
    account = securityUtils.getCurrentAccountId().flatMap(accountService::get).orElse(null);
    if (account == null) {
      event.rerouteTo("");
      return;
    }

    // Only jump straight to the wizard if a tutorial universe specifically for this player
    // was already created. The default universe (always present) must NOT bypass mode selection.
    Universe tutorialUniverse =
        tutorialService.findTutorialUniverse(account.getUsername()).orElse(null);

    if (tutorialUniverse != null) {
      universeType = tutorialUniverse.getType();
      universeContextService.setCurrentUniverse(tutorialUniverse);
      definition = tutorialService.getDefinition(universeType);
      currentStepIndex = tutorialService.getCurrentStep(account.getId(), universeType);
      if (currentStepIndex >= definition.getSteps().size()) {
        currentStepIndex = definition.getSteps().size() - 1;
      }
      currentPhase = ViewPhase.WIZARD;
    } else {
      currentPhase = ViewPhase.SETUP_MODE;
    }

    renderCurrentPhase();
  }

  private void renderCurrentPhase() {
    removeAll();
    switch (currentPhase) {
      case SETUP_MODE -> renderModeSelection();
      case SETUP_FEATURES -> renderFeatureConfig();
      case WIZARD -> renderStep();
    }
  }

  // ── Phase 1: mode selection ───────────────────────────────────────────────

  private void renderModeSelection() {
    H2 heading = new H2("Welcome! Choose Your Play Style");
    heading.addClassNames(LumoUtility.Margin.Bottom.SMALL);
    add(heading);

    Paragraph sub =
        new Paragraph(
            "Each mode gives you a different experience. You can always create more universes"
                + " later.");
    sub.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.LARGE);
    add(sub);

    HorizontalLayout cards = new HorizontalLayout();
    cards.setWidthFull();
    cards.setSpacing(true);

    cards.add(
        modeCard(
            Universe.UniverseType.CAMPAIGN,
            VaadinIcon.BOOK,
            "Campaign",
            "Follow your wrestler's career through story chapters. Make backstage decisions that"
                + " shape the narrative."));
    cards.add(
        modeCard(
            Universe.UniverseType.LEAGUE,
            VaadinIcon.TROPHY,
            "League",
            "Join a competitive fantasy wrestling league. Draft wrestlers, book shows, and"
                + " compete against other players."));
    cards.add(
        modeCard(
            Universe.UniverseType.GLOBAL,
            VaadinIcon.GLOBE,
            "Universe",
            "Full creative control. Build your entire wrestling world from scratch with custom"
                + " wrestlers, titles, and shows."));

    add(cards);
  }

  private Div modeCard(
      final Universe.UniverseType mode,
      final VaadinIcon icon,
      final String title,
      final String description) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.Padding.LARGE,
        LumoUtility.BoxShadow.SMALL);
    card.getStyle()
        .set("cursor", "pointer")
        .set("flex", "1")
        .set("transition", "box-shadow 0.2s")
        .set("border", "2px solid transparent");
    card.getElement()
        .addEventListener(
            "mouseenter", e -> card.getStyle().set("border-color", "var(--lumo-primary-color)"))
        .synchronizeProperty("style");
    card.getElement()
        .addEventListener("mouseleave", e -> card.getStyle().set("border-color", "transparent"))
        .synchronizeProperty("style");

    Span iconSpan = new Span(icon.create());
    iconSpan.getStyle().set("font-size", "2rem").set("color", "var(--lumo-primary-color)");

    H3 cardTitle = new H3(title);
    cardTitle.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Margin.Bottom.XSMALL);

    Paragraph desc = new Paragraph(description);
    desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
    desc.getStyle().set("margin", "0");

    Button selectBtn = new Button("Choose " + title);
    selectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    selectBtn.setWidthFull();
    selectBtn.addClassNames(LumoUtility.Margin.Top.MEDIUM);
    selectBtn.addClickListener(
        e -> {
          selectedMode = mode;
          currentPhase = ViewPhase.SETUP_FEATURES;
          renderCurrentPhase();
        });

    card.add(iconSpan, cardTitle, desc, selectBtn);
    return card;
  }

  // ── Phase 2: feature configuration ───────────────────────────────────────

  private void renderFeatureConfig() {
    Button back =
        new Button(
            "← Back",
            e -> {
              currentPhase = ViewPhase.SETUP_MODE;
              renderCurrentPhase();
            });
    back.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    add(back);

    H2 heading = new H2("Configure Your Universe");
    heading.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Margin.Bottom.XSMALL);
    add(heading);

    Paragraph sub =
        new Paragraph(
            "Choose which features to enable in your tutorial universe. All are on by default —"
                + " you can change them later in Game Settings.");
    sub.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.MEDIUM);
    add(sub);

    // Build feature map with defaults
    Map<String, Boolean> features = new LinkedHashMap<>();
    features.put(GameSettingService.WEAR_AND_TEAR_ENABLED_KEY, true);
    features.put(GameSettingService.AI_NEWS_ENABLED_KEY, true);
    features.put(GameSettingService.STATUS_CARDS_ENABLED_KEY, true);

    Map<String, String> labels = new LinkedHashMap<>();
    labels.put(
        GameSettingService.WEAR_AND_TEAR_ENABLED_KEY,
        "Persistent Wear & Tear — wrestlers accumulate fatigue and injuries across shows");
    labels.put(
        GameSettingService.AI_NEWS_ENABLED_KEY,
        "AI News Feed — automatically generates storyline news articles after each show");
    labels.put(
        GameSettingService.STATUS_CARDS_ENABLED_KEY,
        "Status Cards Mechanic — card-based combat system adds strategic buffs and debuffs");

    VerticalLayout featureList = new VerticalLayout();
    featureList.setPadding(false);
    featureList.setSpacing(false);

    boolean aiConfigured =
        aiSettingsService.isClaudeEnabled()
            || aiSettingsService.isOpenAIEnabled()
            || aiSettingsService.isGeminiEnabled()
            || aiSettingsService.isPollinationsEnabled();

    labels.forEach(
        (key, label) -> {
          Checkbox cb = new Checkbox(label, features.get(key));
          boolean isAiFeature = GameSettingService.AI_NEWS_ENABLED_KEY.equals(key);
          if (isAiFeature && !aiConfigured) {
            cb.setValue(false);
            cb.setEnabled(false);
            features.put(key, false);
            cb.setHelperText("Requires an AI provider configured in Game Settings");
          } else {
            cb.addValueChangeListener(e -> features.put(key, e.getValue()));
          }
          featureList.add(cb);
        });

    add(featureList);

    Button startBtn =
        new Button(
            "Create My Universe & Start Tutorial",
            VaadinIcon.PLAY.create(),
            e -> {
              Universe created =
                  tutorialService.createTutorialUniverse(account, selectedMode, features);
              universeType = selectedMode;
              definition = tutorialService.getDefinition(universeType);
              currentStepIndex = 0;
              currentPhase = ViewPhase.WIZARD;
              Notification.show("✅ Your tutorial universe '" + created.getName() + "' is ready!")
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
              renderCurrentPhase();
            });
    startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
    startBtn.addClassNames(LumoUtility.Margin.Top.LARGE);
    add(startBtn);
  }

  // ── Phase 3: step wizard ──────────────────────────────────────────────────

  private void renderStep() {
    TutorialStep step = definition.getSteps().get(currentStepIndex);
    int totalSteps = definition.getSteps().size();

    // Refresh account first so beforeStep hooks see the latest DB state (e.g. activeWrestlerId
    // set in the previous step is visible when the next step's hook runs).
    account = accountService.get(account.getId()).orElse(account);

    tutorialService.runBeforeStep(account, universeType, currentStepIndex);

    // ── Header ────────────────────────────────────────────────────────────
    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setAlignItems(Alignment.CENTER);

    Span stepLabel = new Span("Step " + (currentStepIndex + 1) + " of " + totalSteps);
    stepLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

    ProgressBar progress = new ProgressBar(0, totalSteps, currentStepIndex + 1);
    progress.setWidth("200px");

    Button skipButton = new Button("Skip Tutorial");
    skipButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    skipButton.addClassNames(LumoUtility.Margin.Left.AUTO);
    skipButton.addClickListener(
        e -> {
          tutorialService.markSkipped(account.getId(), universeType, totalSteps);
          navigateToDashboard();
        });

    header.add(stepLabel, progress, skipButton);
    add(header);

    // ── Title + instructions ──────────────────────────────────────────────
    H2 title = new H2(step.getTitle());
    title.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.XSMALL);
    add(title);

    Paragraph instructions = new Paragraph(step.getInstructions());
    add(instructions);

    // ── Screenshot ────────────────────────────────────────────────────────
    if (step.getImagePath() != null) {
      Div imgContainer = new Div();
      imgContainer.addClassNames(
          LumoUtility.Margin.Vertical.MEDIUM,
          LumoUtility.BorderRadius.MEDIUM,
          LumoUtility.Background.CONTRAST_5);
      imgContainer.setHeight("220px");
      imgContainer
          .getStyle()
          .set("overflow", "hidden")
          .set("display", "flex")
          .set("align-items", "center")
          .set("justify-content", "center");
      Image img = new Image(step.getImagePath(), step.getTitle());
      img.setMaxHeight("220px");
      img.setMaxWidth("100%");
      imgContainer.add(img);
      add(imgContainer);
    }

    // ── Interaction area ──────────────────────────────────────────────────
    if (step.getInteractionMode() == TutorialStep.InteractionMode.INLINE) {
      add(buildWrestlerPicker(totalSteps, step.getAllowedWrestlerNames()));
    } else {
      // Hint line
      Span hint = new Span(step.getValidationHint());
      hint.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
      hint.getStyle().set("font-style", "italic");
      add(hint);

      // Error feedback slot (hidden until Next is clicked and validation fails)
      Div errorSlot = new Div();
      errorSlot.setVisible(false);
      errorSlot.addClassNames(
          LumoUtility.TextColor.ERROR, LumoUtility.Margin.Top.SMALL, LumoUtility.FontSize.SMALL);
      add(errorSlot);

      HorizontalLayout buttons = new HorizontalLayout();
      buttons.addClassNames(LumoUtility.Margin.Top.MEDIUM);

      if (currentStepIndex > 0) {
        Button prevBtn = new Button("◀ Previous");
        prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevBtn.addClickListener(
            e -> {
              tutorialService.advanceStep(
                  account.getId(), universeType, currentStepIndex - 1, totalSteps);
              currentStepIndex--;
              renderStep();
            });
        buttons.add(prevBtn);
      }

      // "Go to X" lets the player navigate to the relevant view and come back
      Button goBtn = new Button("Go to " + step.getTargetViewLabel() + " ↗");
      goBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
      goBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(step.getTargetRoute())));
      buttons.add(goBtn);

      // Next / Complete — validates on click, shows error inline if not ready
      boolean isLast = currentStepIndex == totalSteps - 1;
      Button nextBtn = new Button(isLast ? "Complete Tutorial ✓" : "Next →");
      nextBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      nextBtn.addClickListener(
          e -> {
            account = accountService.get(account.getId()).orElse(account);
            String error = tutorialService.validateStep(account, universeType, currentStepIndex);
            if (error != null) {
              errorSlot.setText("⚠ " + error);
              errorSlot.setVisible(true);
            } else {
              errorSlot.setVisible(false);
              advanceAfterSuccess(totalSteps);
            }
          });
      buttons.add(nextBtn);
      add(buttons);
    }
  }

  // ── Inline wrestler picker ────────────────────────────────────────────────

  /** Snapshot of wrestler display data extracted while the Hibernate session is open. */
  private record WrestlerSnapshot(
      Long id, String name, String alignmentLabel, String description, String imageUrl) {}

  private VerticalLayout buildWrestlerPicker(
      final int totalSteps, final java.util.List<String> allowedNames) {
    VerticalLayout picker = new VerticalLayout();
    picker.setPadding(false);
    picker.setSpacing(true);

    // findAllActiveWithAlignments() is @Transactional(readOnly=true) and uses JOIN FETCH, so
    // the alignments collection is fully loaded before the session closes. runAsAdmin handles
    // the security elevation needed to call the service as a PLAYER.
    List<WrestlerSnapshot> snapshots =
        GeneralSecurityUtils.runAsAdmin(
            () ->
                wrestlerService.findAllActiveWithAlignments().stream()
                    .filter(
                        w ->
                            allowedNames == null
                                || allowedNames.isEmpty()
                                || allowedNames.contains(w.getName()))
                    .map(
                        w -> {
                          String alignment =
                              w.getAlignment() != null
                                      && w.getAlignment().getAlignmentType() != null
                                  ? w.getAlignment().getAlignmentType().name()
                                  : null;
                          String imageUrl = wrestlerService.resolveWrestlerImage(w).url();
                          return new WrestlerSnapshot(
                              w.getId(), w.getName(), alignment, w.getDescription(), imageUrl);
                        })
                    .toList());

    if (snapshots.isEmpty()) {
      Span empty =
          new Span(
              "No wrestlers are available yet. The system is setting one up — please wait a"
                  + " moment and try again.");
      empty.addClassNames(LumoUtility.TextColor.SECONDARY);
      picker.add(empty);
      return picker;
    }

    Span prompt = new Span("Select your wrestler:");
    prompt.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
    picker.add(prompt);

    for (WrestlerSnapshot snapshot : snapshots) {
      picker.add(wrestlerCard(snapshot, totalSteps));
    }
    return picker;
  }

  private HorizontalLayout wrestlerCard(final WrestlerSnapshot wrestler, final int totalSteps) {
    HorizontalLayout card = new HorizontalLayout();
    card.setWidthFull();
    card.setAlignItems(FlexComponent.Alignment.CENTER);
    card.addClassNames(
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Padding.MEDIUM);
    card.getStyle()
        .set("cursor", "pointer")
        .set("border", "2px solid transparent")
        .set("transition", "border-color 0.15s");
    card.getElement()
        .addEventListener(
            "mouseenter", e -> card.getStyle().set("border-color", "var(--lumo-primary-color)"))
        .synchronizeProperty("style");
    card.getElement()
        .addEventListener("mouseleave", e -> card.getStyle().set("border-color", "transparent"))
        .synchronizeProperty("style");

    if (wrestler.imageUrl() != null && !wrestler.imageUrl().isBlank()) {
      Image portrait = new Image(wrestler.imageUrl(), wrestler.name());
      portrait.setHeight("72px");
      portrait.setWidth("56px");
      portrait.getStyle().set("object-fit", "cover").set("border-radius", "4px");
      card.add(portrait);
    } else {
      Div placeholder = new Div(VaadinIcon.USER.create());
      placeholder.setHeight("72px");
      placeholder.setWidth("56px");
      placeholder
          .getStyle()
          .set("display", "flex")
          .set("align-items", "center")
          .set("justify-content", "center")
          .set("background", "var(--lumo-contrast-10pct)")
          .set("border-radius", "4px");
      card.add(placeholder);
    }

    VerticalLayout info = new VerticalLayout();
    info.setPadding(false);
    info.setSpacing(false);
    info.getStyle().set("flex", "1");

    Span name = new Span(wrestler.name());
    name.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
    info.add(name);

    if (wrestler.alignmentLabel() != null) {
      Span alignment = new Span(wrestler.alignmentLabel());
      alignment.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
      info.add(alignment);
    }

    if (wrestler.description() != null && !wrestler.description().isBlank()) {
      Span desc = new Span(wrestler.description());
      desc.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
      desc.getStyle()
          .set("display", "-webkit-box")
          .set("-webkit-line-clamp", "2")
          .set("-webkit-box-orient", "vertical")
          .set("overflow", "hidden");
      info.add(desc);
    }

    card.add(info);

    Button selectBtn = new Button("Select");
    selectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    selectBtn.addClickListener(
        e -> {
          // setActiveWrestlerId handles both persisting and refreshing the SecurityContext
          // so subsequent calls to account.getActiveWrestlerId() reflect the new value.
          GeneralSecurityUtils.runAsAdmin(
              () -> accountService.setActiveWrestlerId(account.getId(), wrestler.id()));
          account = accountService.get(account.getId()).orElse(account);
          String error = tutorialService.validateStep(account, universeType, currentStepIndex);
          if (error == null) {
            Notification.show("✅ " + wrestler.name() + " is now your wrestler!")
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            advanceAfterSuccess(totalSteps);
          } else {
            Notification.show("⚠ " + error).addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });
    card.add(selectBtn);

    return card;
  }

  // ── Shared helpers ────────────────────────────────────────────────────────

  private void advanceAfterSuccess(final int totalSteps) {
    tutorialService.runAfterStep(account, universeType, currentStepIndex);
    int nextStep = currentStepIndex + 1;
    tutorialService.advanceStep(account.getId(), universeType, nextStep, totalSteps);
    if (nextStep >= totalSteps) {
      showCompletionScreen();
    } else {
      currentStepIndex = nextStep;
      renderStep();
    }
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

    String modeName =
        universeType.name().charAt(0) + universeType.name().substring(1).toLowerCase();
    Paragraph message =
        new Paragraph(
            "You've completed the "
                + modeName
                + " tutorial. Your universe is set up and ready — let's get started!");
    message.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.TextAlignment.CENTER);

    Button dashBtn = new Button("Go to Dashboard");
    dashBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
    dashBtn.addClickListener(e -> navigateToDashboard());

    completionCard.add(trophy, congrats, message, dashBtn);
    add(completionCard);

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
    if (securityUtils.isPlayer()) {
      return "player";
    }
    if (securityUtils.isBooker()) {
      return "booker-dashboard";
    }
    return "";
  }
}

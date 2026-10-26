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

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.ai.AiOnboardingService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import lombok.extern.slf4j.Slf4j;

/**
 * The AI onboarding wizard (ATW-pq0s): opens at every "No AI providers" dead end and walks the user
 * through Gemini's free tier. The configurable path is a two-step flow matching the app's other
 * wizards (FeudScriptWizardDialog): step 1 gets the key page open (browser + copyable fallback),
 * step 2 pastes and verifies. Informational gate states (already-configured, pre-universe,
 * non-owner) render as a single panel with no wizard chrome. Rendering is driven by {@link
 * AiOnboardingService#evaluateGate()}.
 */
@Slf4j
public class AiOnboardingDialog extends Dialog {

  public static final String GEMINI_KEY_URL = "https://aistudio.google.com/app/apikey";

  private final AiOnboardingService service;
  private final SecurityUtils securityUtils;

  private final H3 stepTitle = new H3();
  private final VerticalLayout content = new VerticalLayout();
  private final Button backButton = new Button("Back");
  private final Button nextButton = new Button("Next");
  private final Span statusLine = new Span();
  private PasswordField keyField;
  private Button saveButton;
  private boolean saved;

  private boolean twoStepFlow;

  public AiOnboardingDialog(AiOnboardingService service, SecurityUtils securityUtils) {
    this.service = service;
    this.securityUtils = securityUtils;

    setId("ai-onboarding-dialog");
    setWidth("420px");

    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(false);

    stepTitle.setId("wizard-step-title");
    layout.add(stepTitle);

    content.setPadding(false);
    content.setSpacing(false);
    layout.add(content);

    statusLine.setId("status-line");
    statusLine.setWidthFull();
    statusLine.setVisible(false);
    layout.add(statusLine);

    HorizontalLayout buttons = new HorizontalLayout();
    buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttons.setWidthFull();
    if (securityUtils.isAdmin()) {
      Button advanced =
          new Button(
              "Advanced…",
              e -> {
                close();
                UI.getCurrent().navigate("admin");
              });
      advanced.setId("advanced-button");
      advanced.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
      buttons.add(advanced);
    }
    backButton.setId("back-button");
    backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    backButton.addClickListener(e -> navigateBack());
    buttons.add(backButton);
    nextButton.setId("next-button");
    nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    nextButton.addClickListener(e -> navigateForward());
    buttons.add(nextButton);
    Button cancel = new Button("Cancel", e -> close());
    cancel.setId("cancel-button");
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    buttons.add(cancel);
    layout.add(buttons);

    add(layout);
    renderForGate(service.evaluateGate());
  }

  private void renderForGate(AiOnboardingService.GateState gate) {
    switch (gate) {
      case ALREADY_CONFIGURED -> {
        twoStepFlow = false;
        stepTitle.setText("AI is already set up");
        content.add(
            new Paragraph(
                "AI narration, news and planning are ready to use. Provider settings live in"
                    + " Admin → AI Settings."));
        statusLine.setText("✓ AI is already set up.");
        statusLine.getStyle().set("color", "var(--lumo-success-color)");
        statusLine.setVisible(true);
        backButton.setVisible(false);
        nextButton.setVisible(false);
      }
      case NO_UNIVERSE_SELECTED -> {
        twoStepFlow = false;
        stepTitle.setText("Set up AI features");
        content.add(
            new Paragraph(
                "AI narration, news and planning need an AI provider. Google Gemini's free tier"
                    + " covers this game's usage."));
        statusLine.setText("Create your universe first — the AI key is stored per universe.");
        statusLine.getStyle().set("color", "var(--lumo-secondary-text-color)");
        statusLine.setVisible(true);
        backButton.setVisible(false);
        nextButton.setVisible(false);
      }
      case NOT_UNIVERSE_OWNER -> {
        twoStepFlow = false;
        stepTitle.setText("Set up AI features");
        content.add(
            new Paragraph(
                "AI narration, news and planning need an AI provider. Google Gemini's free tier"
                    + " covers this game's usage."));
        statusLine.setText("Ask your universe owner to configure AI.");
        statusLine.getStyle().set("color", "var(--lumo-secondary-text-color)");
        statusLine.setVisible(true);
        backButton.setVisible(false);
        nextButton.setVisible(false);
      }
      case CAN_CONFIGURE -> {
        twoStepFlow = true;
        renderStep1();
      }
    }
  }

  // ── Two-step flow (CAN_CONFIGURE) ─────────────────────────────────────────

  private void renderStep1() {
    content.removeAll();
    stepTitle.setText("Step 1 of 2 — Get a free API key");
    backButton.setVisible(false);
    nextButton.setText("Next");
    nextButton.setId("next-button");

    content.add(
        new Paragraph(
            "AI narration, news and planning need an AI provider. Google Gemini's free tier"
                + " covers this game's usage."));

    Button openKeyPage = new Button("Get a free API key", VaadinIcon.EXTERNAL_LINK.create());
    openKeyPage.setId("open-key-page-button");
    openKeyPage.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    openKeyPage.addClickListener(e -> openKeyPageInBrowser());
    content.add(openKeyPage);

    TextField linkField = new TextField("Or paste this link into your browser");
    linkField.setId("key-link-field");
    linkField.setValue(GEMINI_KEY_URL);
    linkField.setReadOnly(true);
    linkField.setHelperText("Use this if the button didn't open a new tab.");
    linkField.setWidthFull();
    content.add(linkField);
  }

  private void renderStep2() {
    content.removeAll();
    stepTitle.setText("Step 2 of 2 — Connect");
    backButton.setVisible(true);
    nextButton.setText("Save & Finish");

    keyField = new PasswordField("Gemini API key");
    keyField.setId("gemini-key-field");
    keyField.setPlaceholder("AIza…");
    keyField.setWidthFull();
    content.add(keyField);

    // The primary button's role changes per step (Next → Save & Finish → Close);
    // its id follows the role so lookups stay stable.
    nextButton.setId("save-button");
    saveButton = nextButton;
    statusLine.setVisible(false);
  }

  private void navigateForward() {
    if (!twoStepFlow) {
      return;
    }
    if (saved) {
      close();
      return;
    }
    if (stepTitle.getText().startsWith("Step 1")) {
      renderStep2();
    } else {
      save();
    }
  }

  private void navigateBack() {
    if (twoStepFlow && stepTitle.getText().startsWith("Step 2")) {
      renderStep1();
    }
  }

  private void openKeyPageInBrowser() {
    try {
      UI.getCurrent().getPage().open(GEMINI_KEY_URL);
    } catch (RuntimeException e) {
      log.warn("Could not open {} programmatically: {}", GEMINI_KEY_URL, e.getMessage());
      Notification.show("Open " + GEMINI_KEY_URL + " in your browser");
    }
  }

  private void save() {
    statusLine.setVisible(true);
    try {
      if (service.saveGeminiKey(keyField.getValue())) {
        saved = true;
        statusLine.setText("✓ AI is set up.");
        statusLine.getStyle().set("color", "var(--lumo-success-color)");
        keyField.clear();
        nextButton.setText("Close");
      } else {
        statusLine.setText("Could not verify — check the key and try again.");
        statusLine.getStyle().set("color", "var(--lumo-error-color)");
      }
    } catch (IllegalArgumentException e) {
      statusLine.setText(e.getMessage());
      statusLine.getStyle().set("color", "var(--lumo-error-color)");
    }
  }
}

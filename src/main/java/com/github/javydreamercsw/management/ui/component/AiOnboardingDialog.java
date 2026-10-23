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
 * The AI onboarding wizard (ATW-pq0s): opens at every "No AI providers" dead end, walks the user
 * through Gemini's free tier (browser-open of the key page, paste, save), and confirms with the
 * cheap configured check. Rendering is driven by {@link AiOnboardingService#evaluateGate()} —
 * already-configured, pre-universe, and non-owner states are informational; only CAN_CONFIGURE
 * offers the key field.
 */
@Slf4j
public class AiOnboardingDialog extends Dialog {

  public static final String GEMINI_KEY_URL = "https://aistudio.google.com/app/apikey";

  private final AiOnboardingService service;
  private final SecurityUtils securityUtils;

  private final Span statusLine = new Span();
  private final PasswordField keyField = new PasswordField("Gemini API key");
  private final Button saveButton = new Button("Save & Finish");
  private final VerticalLayout configureSection = new VerticalLayout();

  public AiOnboardingDialog(AiOnboardingService service, SecurityUtils securityUtils) {
    this.service = service;
    this.securityUtils = securityUtils;

    setId("ai-onboarding-dialog");
    setModal(true);
    setWidth("420px");

    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(false);

    layout.add(new H3("Set up AI features"));
    layout.add(
        new Paragraph(
            "AI narration, news and planning need an AI provider. Google Gemini's free tier"
                + " covers this game's usage."));

    statusLine.setId("status-line");
    statusLine.setWidthFull();
    statusLine.setVisible(false);

    buildConfigureSection();
    layout.add(configureSection);
    layout.add(statusLine);

    HorizontalLayout buttons = new HorizontalLayout();
    buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    buttons.setWidthFull();
    if (securityUtils.isAdmin()) {
      Button advanced = new Button("Advanced…", e -> close());
      advanced.setId("advanced-button");
      advanced.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
      buttons.add(advanced);
    }
    Button cancel = new Button("Cancel", e -> close());
    cancel.setId("cancel-button");
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    buttons.add(cancel);
    layout.add(buttons);

    add(layout);
    renderForGate(service.evaluateGate());
  }

  private void buildConfigureSection() {
    configureSection.setPadding(false);
    configureSection.setSpacing(false);

    Button openKeyPage = new Button("Get a free API key", VaadinIcon.EXTERNAL_LINK.create());
    openKeyPage.setId("open-key-page-button");
    openKeyPage.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    openKeyPage.addClickListener(e -> openKeyPageInBrowser());
    configureSection.add(openKeyPage);

    TextField linkField = new TextField("Or paste this link into your browser");
    linkField.setId("key-link-field");
    linkField.setValue(GEMINI_KEY_URL);
    linkField.setReadOnly(true);
    linkField.setHelperText("Use this if the button didn't open a new tab.");
    linkField.setWidthFull();
    configureSection.add(linkField);

    keyField.setId("gemini-key-field");
    keyField.setPlaceholder("AIza…");
    keyField.setWidthFull();
    configureSection.add(keyField);

    saveButton.setId("save-button");
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveButton.addClickListener(e -> save());
    configureSection.add(saveButton);
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
        statusLine.setText("✓ AI is set up.");
        statusLine.getStyle().set("color", "var(--lumo-success-color)");
        keyField.clear();
        saveButton.setText("Close");
        saveButton.addClickListener(e -> close());
      } else {
        statusLine.setText("Could not verify — check the key and try again.");
        statusLine.getStyle().set("color", "var(--lumo-error-color)");
      }
    } catch (IllegalArgumentException e) {
      statusLine.setText(e.getMessage());
      statusLine.getStyle().set("color", "var(--lumo-error-color)");
    }
  }

  private void renderForGate(AiOnboardingService.GateState gate) {
    switch (gate) {
      case ALREADY_CONFIGURED -> {
        configureSection.setVisible(false);
        statusLine.setText("✓ AI is already set up.");
        statusLine.getStyle().set("color", "var(--lumo-success-color)");
        statusLine.setVisible(true);
      }
      case NO_UNIVERSE_SELECTED -> {
        keyField.setVisible(false);
        saveButton.setVisible(false);
        statusLine.setText("Create your universe first — the AI key is stored per universe.");
        statusLine.getStyle().set("color", "var(--lumo-secondary-text-color)");
        statusLine.setVisible(true);
      }
      case NOT_UNIVERSE_OWNER -> {
        keyField.setVisible(false);
        saveButton.setVisible(false);
        statusLine.setText("Ask your universe owner to configure AI.");
        statusLine.getStyle().set("color", "var(--lumo-secondary-text-color)");
        statusLine.setVisible(true);
      }
      case CAN_CONFIGURE -> {
        // Full layout, already built.
      }
    }
  }
}

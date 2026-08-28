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
package com.github.javydreamercsw.management.ui.view.feud;

import com.github.javydreamercsw.management.domain.feud.FeudLength;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Three-step wizard dialog for creating a feud story arc script.
 *
 * <p>Step 1 — Wrestlers (2 for a rivalry, 3+ for a multi-wrestler feud)<br>
 * Step 2 — Arc name and PLE appearance cap<br>
 * Step 3 — Ordered beat list (match type, stipulation, winner control, culmination)
 */
public class FeudScriptWizardDialog extends Dialog {

  private static final String BOOKER_PICKS = "Booker Picks";
  private static final String AI_PICKS = "AI Picks";
  private static final String SYSTEM_ROLL = "System Roll";

  private final List<Wrestler> allWrestlers;
  private final List<String> segmentTypeNames;
  private final List<String> segmentRuleNames;
  private final FeudLength defaultLength;
  private final FeudScriptService feudScriptService;
  private final Runnable onComplete;

  private int currentStep = 1;
  private final VerticalLayout content = new VerticalLayout();

  // Step 1
  private MultiSelectComboBox<Wrestler> wrestlerPicker;

  // Step 2
  private TextField nameField;
  private RadioButtonGroup<FeudLength> lengthGroup;

  // Step 3
  private final List<BeatRow> beatRows = new ArrayList<>();
  private VerticalLayout beatContainer;

  // Nav
  private Button backButton;
  private Button nextButton;
  private Button cancelButton;
  private H3 stepTitle;

  public FeudScriptWizardDialog(
      List<Wrestler> allWrestlers,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      int defaultMaxPle,
      FeudScriptService feudScriptService,
      Runnable onComplete) {
    this.allWrestlers = allWrestlers;
    this.segmentTypeNames = segmentTypeNames;
    this.segmentRuleNames = segmentRuleNames;
    this.defaultLength = FeudLength.fromPleCount(defaultMaxPle);
    this.feudScriptService = feudScriptService;
    this.onComplete = onComplete;

    setWidth("min(1400px, 98vw)");
    setHeight("min(90vh, 90vh)");
    setDraggable(true);
    setResizable(true);
    setModal(false);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(false);

    stepTitle = new H3("Step 1 of 3 — Select Wrestlers");
    backButton = new Button("Back", e -> navigateBack());
    nextButton = new Button("Next", e -> navigateForward());
    nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    backButton.setVisible(false);
    cancelButton = new Button("Cancel", e -> close());
    cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    content.setPadding(false);
    content.setSpacing(true);
    content.setSizeFull();

    HorizontalLayout navBar = new HorizontalLayout(cancelButton, backButton, nextButton);
    navBar.setWidthFull();
    navBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    add(stepTitle, content, navBar);
    renderStep1();
  }

  public void preSelectWrestlers(List<Wrestler> wrestlers) {
    if (wrestlerPicker != null) {
      wrestlerPicker.setValue(Set.copyOf(wrestlers));
    }
  }

  // ── Step rendering ────────────────────────────────────────────────────────

  private void renderStep1() {
    content.removeAll();
    stepTitle.setText("Step 1 of 3 — Select Wrestlers");
    backButton.setVisible(false);
    nextButton.setText("Next");

    wrestlerPicker = new MultiSelectComboBox<>("Wrestlers (2 for rivalry, 3+ for multi-feud)");
    wrestlerPicker.setItems(allWrestlers);
    wrestlerPicker.setItemLabelGenerator(Wrestler::getName);
    wrestlerPicker.setWidthFull();
    wrestlerPicker.setRequired(true);
    content.add(wrestlerPicker);
  }

  private void renderStep2() {
    content.removeAll();
    stepTitle.setText("Step 2 of 3 — Arc Details");
    backButton.setVisible(true);
    nextButton.setText("Next");

    Set<Wrestler> selected = wrestlerPicker.getValue();
    String defaultName =
        selected.stream().map(Wrestler::getName).collect(Collectors.joining(" vs ")) + " Arc";

    nameField = new TextField("Arc Name");
    nameField.setValue(defaultName);
    nameField.setWidthFull();
    nameField.setRequired(true);

    lengthGroup = new RadioButtonGroup<>("Feud Length");
    lengthGroup.setItems(FeudLength.values());
    lengthGroup.setItemLabelGenerator(FeudLength::toString);
    lengthGroup.setValue(defaultLength);
    lengthGroup.setHelperText(
        "Short = 1 PLE · Medium = 2 PLEs · Long = 3 PLEs (hard ceiling enforced by the system)");

    content.add(nameField, lengthGroup);
  }

  private void renderStep3() {
    content.removeAll();
    stepTitle.setText("Step 3 of 3 — Beats");
    backButton.setVisible(true);
    nextButton.setText("Finish");

    beatContainer = new VerticalLayout();
    beatContainer.setPadding(false);
    beatContainer.setSpacing(false);
    beatContainer.setSizeFull();

    Button addBeatButton = new Button("+ Add Beat", e -> addBeatRow());
    addBeatButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    content.add(new Span("Define the match sequence for this arc:"), beatContainer, addBeatButton);

    if (beatRows.isEmpty()) {
      addBeatRow();
    } else {
      beatRows.forEach(row -> beatContainer.add(row.layout()));
    }
  }

  // ── Navigation ────────────────────────────────────────────────────────────

  private void navigateForward() {
    if (currentStep == 1) {
      if (wrestlerPicker.getValue().size() < 2) {
        Notification.show("Select at least 2 wrestlers", 3000, Notification.Position.BOTTOM_END)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
        return;
      }
      currentStep = 2;
      renderStep2();
    } else if (currentStep == 2) {
      if (nameField.isEmpty()) {
        nameField.setInvalid(true);
        return;
      }
      currentStep = 3;
      renderStep3();
    } else {
      finish();
    }
  }

  private void navigateBack() {
    if (currentStep == 2) {
      currentStep = 1;
      renderStep1();
    } else if (currentStep == 3) {
      currentStep = 2;
      renderStep2();
    }
  }

  // ── Finish ────────────────────────────────────────────────────────────────

  private void finish() {
    List<Wrestler> wrestlers = new ArrayList<>(wrestlerPicker.getValue());
    String name = nameField.getValue().trim();
    FeudLength length = lengthGroup.getValue() != null ? lengthGroup.getValue() : defaultLength;
    int maxPle = length.getPleCount();

    try {
      FeudScript script = feudScriptService.createFromWizard(name, wrestlers, maxPle);
      for (BeatRow row : beatRows) {
        FeudScriptBeat beat = row.toBeat();
        feudScriptService.addBeat(script, beat);
      }
      Notification.show("Story arc created!", 3000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
      close();
      if (onComplete != null) {
        onComplete.run();
      }
    } catch (Exception ex) {
      Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  // ── Beat row ──────────────────────────────────────────────────────────────

  private void addBeatRow() {
    List<Wrestler> participants = new ArrayList<>(wrestlerPicker.getValue());
    BeatRow row = new BeatRow(participants, segmentTypeNames, segmentRuleNames);
    beatRows.add(row);
    beatContainer.add(row.layout());
  }

  /** One row in the beats editor — holds all fields for a single FeudScriptBeat. */
  private class BeatRow {
    private final ComboBox<String> segmentTypeCombo;
    private final ComboBox<String> segmentRuleCombo;
    private final RadioButtonGroup<String> winnerControlRadio;
    private final ComboBox<Wrestler> plannedWinnerCombo;
    private final Checkbox culminationCheck;
    private final TextArea notesField;
    private final VerticalLayout rowLayout;

    BeatRow(List<Wrestler> participants, List<String> types, List<String> rules) {
      segmentTypeCombo = new ComboBox<>("Match Type");
      segmentTypeCombo.setItems(types);
      segmentTypeCombo.setRequired(true);
      segmentTypeCombo.setWidth("250px");

      segmentRuleCombo = new ComboBox<>("Stipulation");
      segmentRuleCombo.setItems(rules);
      segmentRuleCombo.setPlaceholder("None");
      segmentRuleCombo.setWidth("220px");
      segmentRuleCombo.setClearButtonVisible(true);

      winnerControlRadio = new RadioButtonGroup<>("Winner");
      winnerControlRadio.setItems(BOOKER_PICKS, AI_PICKS, SYSTEM_ROLL);
      winnerControlRadio.setValue(AI_PICKS);

      plannedWinnerCombo = new ComboBox<>("Planned Winner");
      plannedWinnerCombo.setItems(participants);
      plannedWinnerCombo.setItemLabelGenerator(Wrestler::getName);
      plannedWinnerCombo.setVisible(false);
      plannedWinnerCombo.setWidth("180px");

      winnerControlRadio.addValueChangeListener(
          e -> plannedWinnerCombo.setVisible(BOOKER_PICKS.equals(e.getValue())));

      culminationCheck = new Checkbox("Culmination / Blowoff");
      notesField = new TextArea("Story Notes");
      notesField.setPlaceholder("Context for the AI narrator…");
      notesField.setWidthFull();
      notesField.setMaxHeight("120px");

      Button removeBtn = new Button("Remove", ev -> removeRow(this));
      removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
      removeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

      HorizontalLayout topRow =
          new HorizontalLayout(
              segmentTypeCombo,
              segmentRuleCombo,
              winnerControlRadio,
              plannedWinnerCombo,
              culminationCheck,
              removeBtn);
      topRow.setAlignItems(FlexComponent.Alignment.END);
      topRow.setWidthFull();

      rowLayout = new VerticalLayout(topRow, notesField);
      rowLayout.setPadding(true);
      rowLayout.setSpacing(false);
      rowLayout.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
      rowLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
      rowLayout.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    }

    VerticalLayout layout() {
      return rowLayout;
    }

    FeudScriptBeat toBeat() {
      FeudScriptBeat beat = new FeudScriptBeat();
      beat.setSegmentType(segmentTypeCombo.getValue());
      beat.setSegmentRule(segmentRuleCombo.getValue());
      beat.setWinnerControl(toWinnerControl(winnerControlRadio.getValue()));
      if (BOOKER_PICKS.equals(winnerControlRadio.getValue())) {
        beat.setPlannedWinner(plannedWinnerCombo.getValue());
      }
      beat.setCulmination(culminationCheck.getValue());
      beat.setNotes(notesField.getValue());
      beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
      return beat;
    }

    private FeudScriptWinnerControl toWinnerControl(String label) {
      return switch (label) {
        case BOOKER_PICKS -> FeudScriptWinnerControl.BOOKER_PICKS;
        case SYSTEM_ROLL -> FeudScriptWinnerControl.SYSTEM_ROLL;
        default -> FeudScriptWinnerControl.AI_PICKS;
      };
    }
  }

  private void removeRow(BeatRow row) {
    beatRows.remove(row);
    beatContainer.remove(row.layout());
  }
}

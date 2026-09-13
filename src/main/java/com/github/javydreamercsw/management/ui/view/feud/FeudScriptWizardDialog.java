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
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudBeatAssistantService;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

  private final List<Wrestler> allWrestlers;
  private final List<String> segmentTypeNames;
  private final List<String> segmentRuleNames;
  private final FeudLength defaultLength;
  private final FeudScriptService feudScriptService;
  private final FeudBeatAssistantService opponentAssistant;
  private final List<Show> upcomingShows;
  private final List<Title> activeTitles;
  private final Runnable onComplete;

  private int currentStep = 1;
  private final VerticalLayout content = new VerticalLayout();

  // Step 1
  private MultiSelectComboBox<Wrestler> wrestlerPicker;

  // Step 2
  private TextField nameField;
  private RadioButtonGroup<FeudLength> lengthGroup;

  // Step 3
  private final List<BeatEditor> beatRows = new ArrayList<>();
  private VerticalLayout beatContainer;

  // Nav
  private final Button backButton;
  private final Button nextButton;
  private final H3 stepTitle;

  public FeudScriptWizardDialog(
      List<Wrestler> allWrestlers,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      int defaultMaxPle,
      FeudScriptService feudScriptService,
      Runnable onComplete) {
    this(
        allWrestlers,
        segmentTypeNames,
        segmentRuleNames,
        defaultMaxPle,
        feudScriptService,
        null,
        List.of(),
        List.of(),
        onComplete);
  }

  /**
   * Full wizard: {@code opponentAssistant} enables the AI Suggest Opponent button on beat rows
   * (when non-null). External candidates are derived per beat row from the step-1 wrestler
   * selection — anyone on the arc becomes a feud participant, everyone else is external. Pass
   * {@code upcomingShows}/{@code activeTitles} to expose the target-show picker and title-stakes
   * section on beat rows.
   */
  public FeudScriptWizardDialog(
      List<Wrestler> allWrestlers,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      int defaultMaxPle,
      FeudScriptService feudScriptService,
      FeudBeatAssistantService opponentAssistant,
      List<Show> upcomingShows,
      List<Title> activeTitles,
      Runnable onComplete) {
    this.allWrestlers = allWrestlers;
    this.segmentTypeNames = segmentTypeNames;
    this.segmentRuleNames = segmentRuleNames;
    this.defaultLength = FeudLength.fromPleCount(defaultMaxPle);
    this.feudScriptService = feudScriptService;
    this.opponentAssistant = opponentAssistant;
    this.upcomingShows = upcomingShows != null ? upcomingShows : List.of();
    this.activeTitles = activeTitles != null ? activeTitles : List.of();
    this.onComplete = onComplete;

    setWidth("min(1400px, 98vw)");
    setHeight("min(90vh, 90vh)");
    setDraggable(true);
    setResizable(true);
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(false);

    stepTitle = new H3("Step 1 of 3 — Select Wrestlers");
    backButton = new Button("Back", e -> navigateBack());
    nextButton = new Button("Next", e -> navigateForward());
    nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    backButton.setVisible(false);
    Button cancelButton = new Button("Cancel", e -> close());
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

    // Preserve a custom arc name across Back navigation (the field is rebuilt here).
    String savedName = nameField != null ? nameField.getValue() : null;

    Set<Wrestler> selected = wrestlerPicker.getValue();
    String defaultName =
        selected.stream().map(Wrestler::getName).collect(Collectors.joining(" vs ")) + " Arc";

    nameField = new TextField("Arc Name");
    nameField.setValue(savedName != null && !savedName.isBlank() ? savedName : defaultName);
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
      beatRows.forEach(beatContainer::add);
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
    // Validate every beat row first — an invalid row blocks the whole finish.
    boolean invalid = false;
    for (BeatEditor editor : beatRows) {
      if (!editor.isValid()) {
        editor.showValidationError();
        invalid = true;
      }
    }
    if (invalid) {
      Notification.show(
              "Fix the highlighted beat row(s) before finishing",
              4000,
              Notification.Position.BOTTOM_END)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    List<Wrestler> wrestlers = new ArrayList<>(wrestlerPicker.getValue());
    String name = nameField.getValue().trim();
    FeudLength length = lengthGroup.getValue() != null ? lengthGroup.getValue() : defaultLength;
    int maxPle = length.getPleCount();

    try {
      List<FeudScriptBeat> beats = new ArrayList<>();
      for (BeatEditor editor : beatRows) {
        editor.bindScriptContext(null, wrestlers);
        beats.add(editor.toBeat());
      }
      // Single transaction: a failure on any beat leaves nothing persisted.
      feudScriptService.createScriptWithBeats(name, wrestlers, maxPle, beats);
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
    // Everyone not on the arc is an external candidate for this row (recomputed so step-1
    // changes are always reflected).
    List<Wrestler> externalCandidates =
        allWrestlers.stream()
            .filter(w -> participants.stream().noneMatch(p -> p.getId().equals(w.getId())))
            .collect(Collectors.toList());
    BeatEditor editor =
        new BeatEditor(
            new BeatEditor.BeatEditorContext(
                participants,
                externalCandidates,
                upcomingShows,
                activeTitles,
                segmentTypeNames,
                segmentRuleNames,
                true,
                this::removeBeatRow,
                opponentAssistant));
    beatRows.add(editor);
    beatContainer.add(editor);
  }

  private void removeBeatRow(BeatEditor row) {
    beatRows.remove(row);
    beatContainer.remove(row);
  }
}

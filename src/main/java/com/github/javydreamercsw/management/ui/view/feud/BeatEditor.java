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

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.feud.FeudBeatParticipantRole;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.feud.AiSuggestedOpponentDTO;
import com.github.javydreamercsw.management.service.feud.FeudBeatAssistantService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Editor for a single {@link FeudScriptBeat}: match type, stipulation, winner control, culmination
 * flag and story notes. Shared by the arc creation wizard and the add-beat dialog so both entry
 * points stay in sync.
 *
 * <p>Besides the feud's own wrestlers, a beat can involve external participants — an optional
 * surprise opponent and run-in extras, placed on the opposing team at planning time. When an AI
 * assistant is bound, an "AI Suggest Opponent" button fills the opponent field from the eligible
 * roster; the booker confirms or overrides it.
 */
public class BeatEditor extends VerticalLayout {

  private static final String BOOKER_PICKS = "Booker Picks";
  private static final String AI_PICKS = "AI Picks";
  private static final String SYSTEM_ROLL = "System Roll";

  private final ComboBox<String> segmentTypeCombo;
  private final ComboBox<String> segmentRuleCombo;
  private final RadioButtonGroup<String> winnerControlRadio;
  private final ComboBox<Wrestler> plannedWinnerCombo;
  private final Checkbox culminationCheck;
  private final TextArea notesField;
  private final ComboBox<Wrestler> opponentCombo;
  private final MultiSelectComboBox<Wrestler> extrasMulti;
  private final Span rationaleLabel;
  private final Button suggestOpponentButton;

  private FeudBeatAssistantService opponentAssistant;
  private FeudScript script;
  private List<Wrestler> feudParticipants = List.of();

  /**
   * Creates a beat editor. {@code participants} limits the planned-winner choices; when {@code
   * removable} is true a Remove button wired to {@code removeHandler} is shown. No external
   * participant widgets and no AI assistant.
   */
  public BeatEditor(
      List<Wrestler> participants,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      boolean removable,
      Consumer<BeatEditor> removeHandler) {
    this(
        participants,
        segmentTypeNames,
        segmentRuleNames,
        List.of(),
        removable,
        removeHandler,
        null);
  }

  /**
   * Full editor: {@code externalCandidates} feeds the external opponent/extras widgets (already
   * excluding the feud's wrestlers); {@code opponentAssistant} enables the AI suggestion button
   * when non-null. Call {@link #bindScriptContext(FeudScript, List)} afterwards so the assistant
   * has the arc context.
   */
  public BeatEditor(
      List<Wrestler> participants,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      List<Wrestler> externalCandidates,
      boolean removable,
      Consumer<BeatEditor> removeHandler,
      FeudBeatAssistantService opponentAssistant) {

    segmentTypeCombo = new ComboBox<>("Match Type");
    segmentTypeCombo.setItems(segmentTypeNames);
    segmentTypeCombo.setRequired(true);
    segmentTypeCombo.setWidth("250px");

    segmentRuleCombo = new ComboBox<>("Stipulation");
    segmentRuleCombo.setItems(segmentRuleNames);
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

    // External participants (wrestlers outside the feud). They land on the opposing team.
    List<Wrestler> candidates =
        externalCandidates != null
            ? externalCandidates.stream()
                .filter(w -> participants.stream().noneMatch(p -> p.getId().equals(w.getId())))
                .toList()
            : List.of();
    opponentCombo = new ComboBox<>("External Opponent");
    opponentCombo.setItems(candidates);
    opponentCombo.setItemLabelGenerator(Wrestler::getName);
    opponentCombo.setPlaceholder("None");
    opponentCombo.setClearButtonVisible(true);
    opponentCombo.setWidth("200px");
    opponentCombo.setVisible(!candidates.isEmpty());

    extrasMulti = new MultiSelectComboBox<>("External Extras");
    extrasMulti.setItems(candidates);
    extrasMulti.setItemLabelGenerator(Wrestler::getName);
    extrasMulti.setPlaceholder("None");
    extrasMulti.setWidth("220px");
    extrasMulti.setVisible(!candidates.isEmpty());

    // Keep the two external widgets disjoint.
    opponentCombo.addValueChangeListener(
        e -> {
          Wrestler opponent = e.getValue();
          if (opponent != null && extrasMulti.getValue().contains(opponent)) {
            var remaining = new HashSet<>(extrasMulti.getValue());
            remaining.remove(opponent);
            extrasMulti.setValue(remaining);
          }
        });

    rationaleLabel = new Span();
    rationaleLabel.setVisible(false);
    rationaleLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
    rationaleLabel.getStyle().set("font-style", "italic");

    suggestOpponentButton = new Button("✨ AI Suggest Opponent");
    suggestOpponentButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    suggestOpponentButton.setVisible(opponentAssistant != null && !candidates.isEmpty());
    suggestOpponentButton.addClickListener(e -> suggestOpponent());
    this.opponentAssistant = opponentAssistant;

    HorizontalLayout topRow =
        new HorizontalLayout(
            segmentTypeCombo, segmentRuleCombo, winnerControlRadio, plannedWinnerCombo);
    topRow.setAlignItems(FlexComponent.Alignment.END);
    topRow.setWidthFull();

    HorizontalLayout externalRow =
        new HorizontalLayout(suggestOpponentButton, opponentCombo, extrasMulti);
    externalRow.setAlignItems(FlexComponent.Alignment.END);
    externalRow.setWidthFull();

    VerticalLayout externalArea = new VerticalLayout(externalRow, rationaleLabel);
    externalArea.setPadding(false);
    externalArea.setSpacing(false);
    externalArea.setWidthFull();
    externalArea.setVisible(externalRow.isVisible());

    HorizontalLayout optionRow = new HorizontalLayout(culminationCheck);
    optionRow.setAlignItems(FlexComponent.Alignment.CENTER);
    if (removable) {
      Button removeBtn =
          new Button(
              "Remove",
              ev -> {
                if (removeHandler != null) {
                  removeHandler.accept(this);
                }
              });
      removeBtn.addThemeVariants(
          ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      optionRow.add(removeBtn);
    }

    setPadding(true);
    setSpacing(false);
    getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
    getStyle().set("border-radius", "var(--lumo-border-radius-m)");
    getStyle().set("margin-bottom", "var(--lumo-space-s)");

    add(topRow, optionRow, externalArea, notesField);
  }

  /**
   * Binds the owning arc and its feud wrestlers, required for the AI opponent suggestion. Called by
   * the owning dialog after construction.
   */
  public void bindScriptContext(FeudScript script, List<Wrestler> feudParticipants) {
    this.script = script;
    this.feudParticipants = feudParticipants != null ? feudParticipants : List.of();
  }

  /**
   * Pre-fills the editor from an existing (pending) beat — match type, stipulation, winner control,
   * planned winner, culmination, notes and external participants. The persisted external
   * opponent/extras are only selectable if they appear in this editor's candidate list (roster may
   * have changed since the beat was created); anything missing is re-added to the candidate set so
   * an edit never silently drops an existing external.
   *
   * @param beat the pending beat to load
   * @param additionalCandidates persisted externals of the beat, so they remain selectable
   */
  public void setBeat(FeudScriptBeat beat, List<Wrestler> additionalCandidates) {
    segmentTypeCombo.setValue(beat.getSegmentType());
    if (beat.getSegmentRule() != null) {
      segmentRuleCombo.setValue(beat.getSegmentRule());
    }
    winnerControlRadio.setValue(toWinnerControlLabel(beat.getWinnerControl()));
    if (beat.getPlannedWinner() != null) {
      plannedWinnerCombo.setValue(beat.getPlannedWinner());
    }
    culminationCheck.setValue(beat.isCulmination());
    notesField.setValue(beat.getNotes() != null ? beat.getNotes() : "");

    List<Wrestler> selectable =
        new ArrayList<>(opponentCombo.getListDataView().getItems().toList());
    if (additionalCandidates != null) {
      for (Wrestler candidate : additionalCandidates) {
        if (selectable.stream().noneMatch(w -> w.getId().equals(candidate.getId()))) {
          selectable.add(candidate);
        }
      }
      opponentCombo.setItems(selectable);
      extrasMulti.setItems(selectable);
    }
    Wrestler opponent =
        beat.getExternalOpponents().isEmpty() ? null : beat.getExternalOpponents().get(0);
    if (opponent != null) {
      opponentCombo.setValue(opponent);
    }
    extrasMulti.setValue(new HashSet<>(beat.getExternalExtras()));
  }

  private String toWinnerControlLabel(FeudScriptWinnerControl control) {
    return switch (control) {
      case BOOKER_PICKS -> BOOKER_PICKS;
      case SYSTEM_ROLL -> SYSTEM_ROLL;
      default -> AI_PICKS;
    };
  }

  private void suggestOpponent() {
    if (opponentAssistant == null || script == null) {
      return;
    }
    suggestOpponentButton.setEnabled(false);
    suggestOpponentButton.setText("AI thinking…");
    UI ui = UI.getCurrent();
    if (ui == null) {
      // No UI context (unit tests) — synchronous call.
      try {
        applySuggestion(
            opponentAssistant.suggestOpponent(
                script,
                feudParticipants,
                segmentTypeCombo.getValue(),
                segmentRuleCombo.getValue(),
                notesField.getValue()));
      } catch (Exception ex) {
        // Mirror the async error path: keep manual selection, report, restore button.
        Notification.show(
                "AI Suggest Opponent failed: " + rootMessage(ex),
                5000,
                Notification.Position.BOTTOM_END)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      } finally {
        suggestOpponentButton.setEnabled(true);
        suggestOpponentButton.setText("✨ AI Suggest Opponent");
      }
      return;
    }
    GeneralSecurityUtils.runAsAdminAsync(
            () ->
                opponentAssistant.suggestOpponent(
                    script,
                    feudParticipants,
                    segmentTypeCombo.getValue(),
                    segmentRuleCombo.getValue(),
                    notesField.getValue()))
        .thenAccept(
            dto ->
                ui.access(
                    () -> {
                      applySuggestion(dto);
                      suggestOpponentButton.setEnabled(true);
                      suggestOpponentButton.setText("✨ AI Suggest Opponent");
                    }))
        .exceptionally(
            ex -> {
              Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
              ui.access(
                  () -> {
                    Notification.show(
                            "AI Suggest Opponent failed: " + rootMessage(cause),
                            5000,
                            Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    suggestOpponentButton.setEnabled(true);
                    suggestOpponentButton.setText("✨ AI Suggest Opponent");
                  });
              return null;
            });
  }

  private void applySuggestion(AiSuggestedOpponentDTO dto) {
    if (dto == null) {
      return;
    }
    Wrestler match = findCandidate(dto.getWrestlerId());
    if (match != null) {
      opponentCombo.setValue(match);
      rationaleLabel.setText("AI: " + dto.getRationale());
      rationaleLabel.setVisible(true);
    }
  }

  private Wrestler findCandidate(Long wrestlerId) {
    return opponentCombo
        .getListDataView()
        .getItems()
        .filter(w -> w.getId().equals(wrestlerId))
        .findFirst()
        .orElse(null);
  }

  private String rootMessage(Throwable t) {
    return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
  }

  /** Builds a {@link FeudScriptBeat} from the current field values. */
  public FeudScriptBeat toBeat() {
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
    if (opponentCombo.getValue() != null) {
      beat.addExternalParticipant(opponentCombo.getValue(), FeudBeatParticipantRole.OPPONENT);
    }
    for (Wrestler extra : extrasMulti.getValue()) {
      beat.addExternalParticipant(extra, FeudBeatParticipantRole.EXTRA);
    }
    return beat;
  }

  /** True when a match type is selected and the two external widgets stay disjoint. */
  public boolean isValid() {
    if (segmentTypeCombo.getValue() == null || segmentTypeCombo.getValue().isBlank()) {
      return false;
    }
    Wrestler opponent = opponentCombo.getValue();
    return opponent == null || !extrasMulti.getValue().contains(opponent);
  }

  /** Marks the match-type combo invalid when no match type is selected. */
  public void showValidationError() {
    segmentTypeCombo.setInvalid(true);
    segmentTypeCombo.setErrorMessage("Match type is required");
  }

  private FeudScriptWinnerControl toWinnerControl(String label) {
    return switch (label) {
      case BOOKER_PICKS -> FeudScriptWinnerControl.BOOKER_PICKS;
      case SYSTEM_ROLL -> FeudScriptWinnerControl.SYSTEM_ROLL;
      default -> FeudScriptWinnerControl.AI_PICKS;
    };
  }

  /** Externals selected so far (opponent + extras), for tests and owning dialogs. */
  public List<Wrestler> getSelectedExternals() {
    List<Wrestler> all = new ArrayList<>();
    if (opponentCombo.getValue() != null) {
      all.add(opponentCombo.getValue());
    }
    all.addAll(extrasMulti.getValue());
    return all;
  }
}

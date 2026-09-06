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

import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.List;
import java.util.function.Consumer;

/**
 * Editor for a single {@link FeudScriptBeat}: match type, stipulation, winner control, culmination
 * flag and story notes. Shared by the arc creation wizard and the add-beat dialog so both entry
 * points stay in sync.
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

  /**
   * Creates a beat editor. {@code participants} limits the planned-winner choices; when {@code
   * removable} is true a Remove button wired to {@code removeHandler} is shown.
   */
  public BeatEditor(
      List<Wrestler> participants,
      List<String> segmentTypeNames,
      List<String> segmentRuleNames,
      boolean removable,
      Consumer<BeatEditor> removeHandler) {

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

    HorizontalLayout topRow =
        new HorizontalLayout(
            segmentTypeCombo, segmentRuleCombo, winnerControlRadio, plannedWinnerCombo);
    topRow.setAlignItems(FlexComponent.Alignment.END);
    topRow.setWidthFull();

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

    add(topRow, optionRow, notesField);
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
    return beat;
  }

  /** True when a match type is selected. */
  public boolean isValid() {
    return segmentTypeCombo.getValue() != null && !segmentTypeCombo.getValue().isBlank();
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
}

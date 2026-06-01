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
package com.github.javydreamercsw.management.ui.view.outcome;

import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OutcomeMatrixEntryFormDialog extends Dialog {

  private final OutcomeMatrixService outcomeMatrixService;
  private final OutcomeMatrix matrix;
  private final OutcomeMatrixEntry entry;
  private final Runnable onSave;

  private final IntegerField diceRollField = new IntegerField("Dice Roll (11–66)");
  private final TextArea templateTextField = new TextArea("Template Text");
  private final IntegerField heatDeltaField = new IntegerField("Heat Δ");
  private final IntegerField fanDeltaField = new IntegerField("Fan Δ");
  private final IntegerField tvGradeDeltaField = new IntegerField("TV Grade Δ");
  private final IntegerField grudgeGradeDeltaField = new IntegerField("Grudge Grade Δ");
  private final Checkbox injuryCausedField = new Checkbox("Causes Injury");
  private final ComboBox<OutcomeMatrix> redirectField = new ComboBox<>("Redirect to Matrix");

  public OutcomeMatrixEntryFormDialog(
      @NonNull final OutcomeMatrixService outcomeMatrixService,
      @NonNull final OutcomeMatrix matrix,
      final OutcomeMatrixEntry existingEntry,
      @NonNull final Runnable onSave) {
    this.outcomeMatrixService = outcomeMatrixService;
    this.matrix = matrix;
    this.entry = existingEntry != null ? existingEntry : new OutcomeMatrixEntry();
    this.onSave = onSave;

    setHeaderTitle(
        existingEntry == null
            ? "New Entry"
            : "Edit Entry (Roll " + existingEntry.getDiceRoll() + ")");
    setWidth("500px");

    buildForm();
    if (existingEntry != null) {
      populateFields(existingEntry);
    } else {
      prefillNextAvailableRoll();
    }
  }

  private void prefillNextAvailableRoll() {
    Set<Integer> used =
        outcomeMatrixService.getEntries(matrix.getId()).stream()
            .map(OutcomeMatrixEntry::getDiceRoll)
            .collect(Collectors.toSet());
    for (int tens = 1; tens < 6 + 1; tens++) {
      for (int units = 1; units < 6 + 1; units++) {
        int roll = tens * 10 + units;
        if (!used.contains(roll)) {
          diceRollField.setValue(roll);
          return;
        }
      }
    }
  }

  private void buildForm() {
    diceRollField.setMin(11);
    diceRollField.setMax(66);
    diceRollField.setWidthFull();
    diceRollField.setRequired(true);

    templateTextField.setWidthFull();
    templateTextField.setMinHeight("100px");
    templateTextField.setHelperText(
        "Use FAVORED, UNDERDOG, ALLY, [celebrity 1] etc. as placeholders.");
    templateTextField.setRequired(true);

    heatDeltaField.setHelperText("Rivalry heat change");
    fanDeltaField.setHelperText("Fan count change");
    tvGradeDeltaField.setHelperText("Letter steps (+ up, - down)");
    grudgeGradeDeltaField.setHelperText("Grudge grade change");

    redirectField.setWidthFull();
    redirectField.setItemLabelGenerator(OutcomeMatrix::getName);
    redirectField.setClearButtonVisible(true);
    redirectField.setHelperText("If set, the dice roll redirects to this chart instead");
    redirectField.setItems(outcomeMatrixService.getAll());

    HorizontalLayout effectRow1 = new HorizontalLayout(heatDeltaField, fanDeltaField);
    effectRow1.setWidthFull();
    HorizontalLayout effectRow2 = new HorizontalLayout(tvGradeDeltaField, grudgeGradeDeltaField);
    effectRow2.setWidthFull();

    VerticalLayout content =
        new VerticalLayout(
            diceRollField,
            templateTextField,
            effectRow1,
            effectRow2,
            injuryCausedField,
            redirectField);
    content.setPadding(false);
    add(content);

    Button saveBtn = new Button("Save", new Icon(VaadinIcon.CHECK));
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveBtn.addClickListener(e -> save());

    Button cancelBtn = new Button("Cancel");
    cancelBtn.addClickListener(e -> close());

    getFooter().add(cancelBtn, saveBtn);
  }

  private void populateFields(@NonNull final OutcomeMatrixEntry e) {
    diceRollField.setValue(e.getDiceRoll());
    templateTextField.setValue(e.getTemplateText() != null ? e.getTemplateText() : "");
    heatDeltaField.setValue(e.getHeatDelta());
    fanDeltaField.setValue(e.getFanDelta() != null ? e.getFanDelta().intValue() : null);
    tvGradeDeltaField.setValue(e.getTvGradeDelta());
    grudgeGradeDeltaField.setValue(e.getGrudgeGradeDelta());
    injuryCausedField.setValue(e.isInjuryCaused());
    redirectField.setValue(e.getRedirectToMatrix());
  }

  private void save() {
    Integer diceRoll = diceRollField.getValue();
    String templateText = templateTextField.getValue();

    if (diceRoll == null) {
      Notification.show("Dice Roll is required.", 3000, Notification.Position.BOTTOM_START);
      return;
    }
    if (templateText == null || templateText.isBlank()) {
      Notification.show("Template text is required.", 3000, Notification.Position.BOTTOM_START);
      return;
    }

    entry.setDiceRoll(diceRoll);
    entry.setTemplateText(templateText);
    entry.setHeatDelta(heatDeltaField.getValue());
    Integer fanInt = fanDeltaField.getValue();
    entry.setFanDelta(fanInt != null ? fanInt.longValue() : null);
    entry.setTvGradeDelta(tvGradeDeltaField.getValue());
    entry.setGrudgeGradeDelta(grudgeGradeDeltaField.getValue());
    entry.setInjuryCaused(Boolean.TRUE.equals(injuryCausedField.getValue()));
    entry.setRedirectToMatrix(redirectField.getValue());

    try {
      if (entry.getId() == null) {
        outcomeMatrixService.addEntry(matrix.getId(), entry);
      } else {
        outcomeMatrixService.updateEntry(entry);
      }
      onSave.run();
      close();
    } catch (Exception ex) {
      log.error("Failed to save entry", ex);
      Notification n =
          Notification.show(
              "Save failed: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_START);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }
}

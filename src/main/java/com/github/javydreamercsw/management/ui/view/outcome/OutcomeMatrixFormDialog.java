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
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixEntry;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OutcomeMatrixFormDialog extends Dialog {

  private final OutcomeMatrixService outcomeMatrixService;
  private final Runnable onSave;
  private final OutcomeMatrix matrix;

  private final TextField nameField = new TextField("Name");
  private final ComboBox<OutcomeMatrixCategory> categoryField = new ComboBox<>("Category");
  private final TextArea descriptionField = new TextArea("Description");
  private final Binder<OutcomeMatrix> binder = new Binder<>(OutcomeMatrix.class);

  final Grid<OutcomeMatrixEntry> entriesGrid = new Grid<>(OutcomeMatrixEntry.class, false);

  public OutcomeMatrixFormDialog(
      @NonNull final OutcomeMatrixService outcomeMatrixService,
      final OutcomeMatrix existingMatrix,
      @NonNull final Runnable onSave) {
    this.outcomeMatrixService = outcomeMatrixService;
    this.onSave = onSave;
    this.matrix = existingMatrix != null ? existingMatrix : new OutcomeMatrix();

    setHeaderTitle(
        existingMatrix == null ? "New Outcome Matrix" : "Edit: " + existingMatrix.getName());
    setWidth("900px");
    setMaxHeight("90vh");

    buildForm();
    bindFields();
    if (existingMatrix != null) {
      binder.readBean(existingMatrix);
      refreshEntriesGrid();
    }
  }

  private void buildForm() {
    nameField.setWidthFull();
    nameField.setRequired(true);

    categoryField.setItems(OutcomeMatrixCategory.values());
    categoryField.setItemLabelGenerator(OutcomeMatrixCategory::getDisplayName);
    categoryField.setWidthFull();
    categoryField.setRequired(true);

    descriptionField.setWidthFull();
    descriptionField.setMinHeight("80px");

    setupEntriesGrid();

    Button addEntryBtn = new Button("Add Entry", new Icon(VaadinIcon.PLUS));
    addEntryBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    addEntryBtn.addClickListener(e -> openEntryDialog(null));

    VerticalLayout content =
        new VerticalLayout(nameField, categoryField, descriptionField, addEntryBtn, entriesGrid);
    content.setPadding(false);
    content.setSpacing(true);
    add(content);

    Button saveBtn = new Button("Save", new Icon(VaadinIcon.CHECK));
    saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveBtn.addClickListener(e -> save());

    Button cancelBtn = new Button("Cancel");
    cancelBtn.addClickListener(e -> close());

    getFooter().add(cancelBtn, saveBtn);
  }

  private void setupEntriesGrid() {
    entriesGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
    entriesGrid.setHeight("300px");
    entriesGrid.setWidthFull();

    entriesGrid
        .addColumn(OutcomeMatrixEntry::getDiceRoll)
        .setHeader("Roll")
        .setWidth("70px")
        .setFlexGrow(0);
    entriesGrid
        .addColumn(e -> truncate(e.getTemplateText(), 60))
        .setHeader("Template Text")
        .setFlexGrow(3);
    entriesGrid
        .addColumn(e -> nullableInt(e.getHeatDelta()))
        .setHeader("Heat Δ")
        .setWidth("75px")
        .setFlexGrow(0);
    entriesGrid
        .addColumn(e -> nullableInt(e.getGrudgeGradeDelta()))
        .setHeader("Grudge Δ")
        .setWidth("85px")
        .setFlexGrow(0);
    entriesGrid
        .addColumn(e -> nullableInt(e.getTvGradeDelta()))
        .setHeader("TV Δ")
        .setWidth("65px")
        .setFlexGrow(0);
    entriesGrid
        .addColumn(e -> e.isInjuryCaused() ? "Yes" : "")
        .setHeader("Injury")
        .setWidth("70px")
        .setFlexGrow(0);
    entriesGrid
        .addColumn(e -> e.getRedirectToMatrix() != null ? e.getRedirectToMatrix().getName() : "")
        .setHeader("Redirect")
        .setFlexGrow(1);

    entriesGrid
        .addComponentColumn(
            entry -> {
              Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
              editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
              editBtn.addClickListener(e -> openEntryDialog(entry));

              Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
              deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
              deleteBtn.addClickListener(e -> deleteEntry(entry));

              return new HorizontalLayout(editBtn, deleteBtn);
            })
        .setHeader("Actions")
        .setWidth("110px")
        .setFlexGrow(0);
  }

  private void bindFields() {
    binder
        .forField(nameField)
        .asRequired("Name is required")
        .bind(OutcomeMatrix::getName, OutcomeMatrix::setName);
    binder
        .forField(categoryField)
        .asRequired("Category is required")
        .bind(OutcomeMatrix::getCategory, OutcomeMatrix::setCategory);
    binder
        .forField(descriptionField)
        .bind(OutcomeMatrix::getDescription, OutcomeMatrix::setDescription);
  }

  private void save() {
    if (!binder.writeBeanIfValid(matrix)) {
      return;
    }
    try {
      if (matrix.getId() == null) {
        outcomeMatrixService.createMatrix(matrix);
      } else {
        outcomeMatrixService.updateMatrix(matrix);
      }
      onSave.run();
      close();
    } catch (Exception e) {
      log.error("Failed to save outcome matrix", e);
      Notification n =
          Notification.show(
              "Save failed: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  private void openEntryDialog(final OutcomeMatrixEntry entry) {
    if (matrix.getId() == null) {
      Notification.show(
          "Save the matrix first before adding entries.", 3000, Notification.Position.BOTTOM_START);
      return;
    }
    new OutcomeMatrixEntryFormDialog(outcomeMatrixService, matrix, entry, this::refreshEntriesGrid)
        .open();
  }

  private void deleteEntry(@NonNull final OutcomeMatrixEntry entry) {
    try {
      outcomeMatrixService.deleteEntry(entry.getId());
      refreshEntriesGrid();
    } catch (Exception e) {
      log.error("Failed to delete entry", e);
      Notification n =
          Notification.show(
              "Delete failed: " + e.getMessage(), 4000, Notification.Position.BOTTOM_START);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  void refreshEntriesGrid() {
    if (matrix.getId() != null) {
      List<OutcomeMatrixEntry> entries = outcomeMatrixService.getEntries(matrix.getId());
      entriesGrid.setItems(entries);
    }
  }

  private String truncate(final String text, final int maxLen) {
    if (text == null) return "";
    return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
  }

  private String nullableInt(final Integer value) {
    return value == null ? "" : (value > 0 ? "+" + value : String.valueOf(value));
  }
}

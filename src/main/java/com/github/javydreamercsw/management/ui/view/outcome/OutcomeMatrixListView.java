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

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrix;
import com.github.javydreamercsw.management.domain.outcome.OutcomeMatrixCategory;
import com.github.javydreamercsw.management.service.outcome.OutcomeMatrixService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Route("outcome-matrix-list")
@PageTitle("Outcome Matrices")
@PermitAll
@Menu(order = 15, icon = "vaadin:table", title = "Outcome Matrices")
@Slf4j
public class OutcomeMatrixListView extends Main {

  private final OutcomeMatrixService outcomeMatrixService;
  private final SecurityUtils securityUtils;
  public final Grid<OutcomeMatrix> grid = new Grid<>(OutcomeMatrix.class, false);
  final ComboBox<OutcomeMatrixCategory> categoryFilter = new ComboBox<>("Filter by category");
  final Button createButton = new Button("Add Matrix", new Icon(VaadinIcon.PLUS));

  public OutcomeMatrixListView(
      @NonNull final OutcomeMatrixService outcomeMatrixService,
      @NonNull final SecurityUtils securityUtils) {
    this.outcomeMatrixService = outcomeMatrixService;
    this.securityUtils = securityUtils;

    setSizeFull();

    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.setVisible(securityUtils.canCreate());
    createButton.addClickListener(e -> openCreateDialog());

    categoryFilter.setItems(OutcomeMatrixCategory.values());
    categoryFilter.setItemLabelGenerator(OutcomeMatrixCategory::getDisplayName);
    categoryFilter.setClearButtonVisible(true);
    categoryFilter.addValueChangeListener(e -> refreshGrid());

    add(new ViewToolbar("Outcome Matrices", ViewToolbar.group(categoryFilter, createButton)));

    setupGrid();
    add(grid);
    refreshGrid();
  }

  private void setupGrid() {
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setSizeFull();

    grid.addColumn(OutcomeMatrix::getName).setHeader("Name").setSortable(true).setFlexGrow(2);
    grid.addColumn(m -> m.getCategory().getDisplayName())
        .setHeader("Category")
        .setSortable(true)
        .setFlexGrow(1);
    grid.addColumn(m -> outcomeMatrixService.getEntries(m.getId()).size())
        .setHeader("Entries")
        .setSortable(false)
        .setWidth("90px")
        .setFlexGrow(0);

    grid.addComponentColumn(
            matrix -> {
              Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
              editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
              editBtn.setVisible(securityUtils.canEdit());
              editBtn.addClickListener(e -> openEditDialog(matrix));

              Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
              deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
              deleteBtn.setVisible(securityUtils.canDelete());
              deleteBtn.addClickListener(e -> confirmDelete(matrix));

              return new HorizontalLayout(editBtn, deleteBtn);
            })
        .setHeader("Actions")
        .setWidth("120px")
        .setFlexGrow(0);
  }

  public void refreshGrid() {
    OutcomeMatrixCategory selected = categoryFilter.getValue();
    if (selected != null) {
      grid.setItems(outcomeMatrixService.getByCategory(selected));
    } else {
      grid.setItems(outcomeMatrixService.getAll());
    }
  }

  private void openCreateDialog() {
    new OutcomeMatrixFormDialog(outcomeMatrixService, null, this::refreshGrid).open();
  }

  private void openEditDialog(@NonNull final OutcomeMatrix matrix) {
    new OutcomeMatrixFormDialog(outcomeMatrixService, matrix, this::refreshGrid).open();
  }

  private void confirmDelete(@NonNull final OutcomeMatrix matrix) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete \"" + matrix.getName() + "\"?");
    dialog.setText("This will permanently delete the matrix and all its entries.");
    dialog.setCancelable(true);
    dialog.setConfirmText("Delete");
    dialog.setConfirmButtonTheme("error primary");
    dialog.addConfirmListener(
        e -> {
          try {
            outcomeMatrixService.deleteMatrix(matrix.getId());
            refreshGrid();
            Notification.show("Matrix deleted.", 3000, Notification.Position.BOTTOM_START);
          } catch (Exception ex) {
            log.error("Failed to delete outcome matrix {}", matrix.getId(), ex);
            Notification n =
                Notification.show(
                    "Delete failed: " + ex.getMessage(), 5000, Notification.Position.BOTTOM_START);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
          }
        });
    dialog.open();
  }
}

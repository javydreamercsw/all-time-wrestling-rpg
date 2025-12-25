/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.injury;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.NonNull;

public class InjuryDialog extends Dialog {

  private final Wrestler wrestler;
  private final InjuryService injuryService;
  private final Runnable onSave;
  private final Grid<Injury> injuryGrid = new Grid<>(Injury.class);

  public InjuryDialog(
      @NonNull Wrestler wrestler, @NonNull InjuryService injuryService, @NonNull Runnable onSave) {
    this.wrestler = wrestler;
    this.injuryService = injuryService;
    this.onSave = onSave;

    setHeaderTitle("Manage Injuries for " + wrestler.getName());
    setWidth("80vw");
    setHeight("80vh");

    updateGrid();

    Button createButton =
        new Button(
            "Create Injury",
            e -> {
              CreateInjuryDialog createDialog =
                  new CreateInjuryDialog(
                      wrestler,
                      injuryService,
                      () -> {
                        updateGrid();
                        onSave.run();
                      });
              createDialog.setId("create-injury-dialog");
              createDialog.open();
            });
    createButton.setId("create-injury-button");

    add(new VerticalLayout(createButton, injuryGrid));
  }

  private void updateGrid() {
    injuryGrid.setItems(injuryService.getAllInjuriesForWrestler(wrestler.getId()));
    injuryGrid.removeAllColumns();
    injuryGrid.addColumn(Injury::getName).setHeader("Name");
    injuryGrid.addColumn(Injury::getDescription).setHeader("Description");
    injuryGrid.addColumn(Injury::getSeverity).setHeader("Severity");
    injuryGrid.addColumn(Injury::getHealthPenalty).setHeader("Health Penalty");
    injuryGrid.addColumn(Injury::getInjuryDate).setHeader("Injury Date");
    injuryGrid.addColumn(injury -> injury.getIsActive() ? "Active" : "Healed").setHeader("Status");
    injuryGrid
        .addComponentColumn(
            injury -> {
              Button healButton = new Button("Heal");
              healButton.setId("heal-injury-" + injury.getId());
              healButton.setEnabled(injury.getIsActive());
              healButton.addClickListener(
                  e -> {
                    injuryService.attemptHealing(injury.getId());
                    updateGrid();
                    onSave.run();
                  });
              return healButton;
            })
        .setHeader("Actions");
  }
}

package com.github.javydreamercsw.management.ui.view.injury;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
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
              healButton.setEnabled(injury.getIsActive());
              healButton.addClickListener(
                  e -> {
                    injuryService.attemptHealing(injury.getId(), null);
                    updateGrid();
                    onSave.run();
                  });
              return healButton;
            })
        .setHeader("Actions");
  }
}

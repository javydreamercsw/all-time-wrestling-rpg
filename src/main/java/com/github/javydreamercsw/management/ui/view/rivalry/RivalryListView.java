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
package com.github.javydreamercsw.management.ui.view.rivalry;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.rivalry.RivalryDTO;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;

@Route("rivalry-list")
@PageTitle("Rivalry List")
@Menu(order = 1, icon = "vaadin:flame", title = "Rivalry List")
@PermitAll
public class RivalryListView extends Main {

  /** Status filter options for the rivalry grid (ATW-aeib): active feuds first by default. */
  enum RivalryStatusFilter {
    ACTIVE(Boolean.TRUE),
    ALL(null),
    ENDED(Boolean.FALSE);

    private final Boolean isActive;

    RivalryStatusFilter(final Boolean isActive) {
      this.isActive = isActive;
    }

    Boolean toIsActive() {
      return isActive;
    }
  }

  private final RivalryService rivalryService;
  private final WrestlerService wrestlerService;
  private final SecurityUtils securityUtils;

  final Grid<RivalryDTO> rivalryGrid;
  final ComboBox<RivalryStatusFilter> statusFilter;

  public RivalryListView(
      @NonNull final RivalryService rivalryService,
      @NonNull final WrestlerService wrestlerService,
      @NonNull final SecurityUtils securityUtils) {
    this.rivalryService = rivalryService;
    this.wrestlerService = wrestlerService;
    this.securityUtils = securityUtils;

    List<Wrestler> allWrestlers =
        wrestlerService.getAllWrestlers().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .toList();

    rivalryGrid = new Grid<>();

    statusFilter = new ComboBox<>("Status");
    statusFilter.setItems(RivalryStatusFilter.values());
    statusFilter.setItemLabelGenerator(
        filter ->
            switch (filter) {
              case ACTIVE -> "Active";
              case ALL -> "All";
              case ENDED -> "Ended";
            });
    statusFilter.setValue(RivalryStatusFilter.ACTIVE);
    statusFilter.setWidth("140px");
    statusFilter.addValueChangeListener(e -> rivalryGrid.getDataProvider().refreshAll());

    Button newRivalryButton =
        new Button(
            "New Rivalry",
            e -> {
              ComboBox<Wrestler> w1 = new ComboBox<>("Wrestler 1");
              w1.setItems(allWrestlers);
              w1.setItemLabelGenerator(Wrestler::getName);
              ComboBox<Wrestler> w2 = new ComboBox<>("Wrestler 2");
              w2.setItems(allWrestlers);
              w2.setItemLabelGenerator(Wrestler::getName);
              TextField notes = new TextField("Storyline Notes");
              notes.setWidthFull();

              Dialog createDialog = new Dialog();
              createDialog.setHeaderTitle("New Rivalry");
              Button saveBtn =
                  new Button(
                      "Create",
                      ev -> {
                        if (w1.getValue() == null || w2.getValue() == null) {
                          return;
                        }
                        rivalryService.createRivalry(
                            w1.getValue().getId(), w2.getValue().getId(), notes.getValue());
                        rivalryGrid.getDataProvider().refreshAll();
                        createDialog.close();
                        Notification.show("Rivalry created", 2000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                      });
              saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
              Button cancelBtn = new Button("Cancel", ev -> createDialog.close());
              cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
              createDialog.add(new VerticalLayout(w1, w2, notes));
              createDialog.getFooter().add(cancelBtn, saveBtn);
              createDialog.open();
            });
    newRivalryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    newRivalryButton.setVisible(securityUtils.canCreate());

    // Grouped view (ATW-8nc8): one row per wrestler pair — the active rivalry when the pair
    // has one, otherwise the latest ended cycle. The expander column (last) reveals the pair's
    // full feud-cycle history.
    rivalryGrid.setItems(
        query ->
            rivalryService
                .getRepresentativeRivalriesWithWrestlers(
                    toSpringPageRequest(query), statusFilter.getValue().toIsActive())
                .map(rivalryService.getRivalryMapper()::toRivalryDTO)
                .stream());
    // Expandable feud-cycle history per pair (ATW-8nc8); toggled by the chevron column below.
    rivalryGrid.setItemDetailsRenderer(new ComponentRenderer<>(this::buildPairHistoryDetails));
    rivalryGrid
        .addColumn(rivalry -> rivalry.getWrestler1().getName())
        .setHeader("Wrestler 1")
        .setSortable(true)
        .setSortProperty("wrestler1.name");
    rivalryGrid
        .addColumn(rivalry -> rivalry.getWrestler2().getName())
        .setHeader("Wrestler 2")
        .setSortable(true)
        .setSortProperty("wrestler2.name");
    rivalryGrid
        .addColumn(RivalryDTO::getHeat)
        .setHeader("Heat")
        .setSortable(true)
        .setSortProperty("heat");
    rivalryGrid
        .addColumn(RivalryDTO::getStorylineNotes)
        .setHeader("Notes")
        .setSortable(true)
        .setSortProperty("storylineNotes");
    rivalryGrid
        .addColumn(RivalryDTO::getStartedDate)
        .setHeader("Start Date")
        .setSortable(true)
        .setSortProperty("startedDate");
    rivalryGrid
        .addColumn(RivalryDTO::getEndedDate)
        .setHeader("End Date")
        .setSortable(true)
        .setSortProperty("endedDate");
    rivalryGrid
        .addComponentColumn(
            rivalry -> {
              Button addHeatButton = new Button("Add Heat");
              addHeatButton.addClickListener(
                  e -> {
                    Dialog dialog = new Dialog();
                    NumberField heatField = new NumberField("Heat to Add");
                    Button saveButton =
                        new Button(
                            "Save",
                            event -> {
                              rivalryService.addHeat(
                                  rivalry.getId(), heatField.getValue().intValue(), "UI Edit");
                              dialog.close();
                              rivalryGrid.getDataProvider().refreshAll();
                            });
                    saveButton.setVisible(securityUtils.canEdit());
                    dialog.add(new VerticalLayout(heatField, saveButton));
                    dialog.open();
                  });
              addHeatButton.setVisible(securityUtils.canEdit());
              return addHeatButton;
            })
        .setHeader("Actions");

    rivalryGrid
        .addComponentColumn(
            rivalry -> {
              Button deleteButton = new Button("Delete");
              deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
              deleteButton.addClickListener(
                  e -> {
                    // Deletion is permanent — confirm before calling the service (ATW-gaxw): the
                    // service clears segment references AND evicts the rivalries cache, which the
                    // old direct-repository delete skipped (deleted rivalries kept appearing in
                    // show-planning context for the cache TTL).
                    ConfirmDialog confirm = new ConfirmDialog();
                    confirm.setHeader("Delete rivalry?");
                    confirm.setText(
                        "Permanently delete the rivalry between "
                            + rivalry.getWrestler1().getName()
                            + " and "
                            + rivalry.getWrestler2().getName()
                            + "? Its heat history and segment links are removed.");
                    confirm.setConfirmButton("Delete", ev -> deleteRivalry(rivalry.getId()));
                    confirm.open();
                  });
              deleteButton.setVisible(securityUtils.canDelete());
              return deleteButton;
            })
        .setHeader("Delete");

    // Expander column LAST (ATW-8nc8) so the E2E tests' fixed column indexes (Heat = index 2)
    // stay stable.
    rivalryGrid
        .addComponentColumn(
            rivalry -> {
              Button expander = new Button("History");
              expander.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
              expander.addClickListener(
                  e ->
                      rivalryGrid.setDetailsVisible(
                          rivalry, !Boolean.TRUE.equals(rivalryGrid.isDetailsVisible(rivalry))));
              return expander;
            })
        .setHeader("");

    rivalryGrid.addItemClickListener(
        e -> UI.getCurrent().navigate(RivalryDetailView.class, e.getItem().getId()));
    rivalryGrid.getStyle().set("cursor", "pointer");
    rivalryGrid.setSizeFull();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    if (securityUtils.canCreate()) {
      add(new ViewToolbar("Rivalry List", ViewToolbar.group(statusFilter, newRivalryButton)));
    } else {
      add(new ViewToolbar("Rivalry List", ViewToolbar.group(statusFilter)));
    }
    Div gridWrapper = new Div(rivalryGrid);
    gridWrapper.addClassName("grid-scroll-container");
    add(gridWrapper);
  }

  /** Confirmed delete: goes through the service so the rivalries cache is evicted (ATW-gaxw). */
  void deleteRivalry(Long rivalryId) {
    rivalryService.deleteRivalry(rivalryId);
    rivalryGrid.getDataProvider().refreshAll();
    Notification.show("Rivalry deleted", 2000, Notification.Position.BOTTOM_END)
        .addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  /** Test bridge for the private details builder. */
  Component pairHistoryDetailsForTest(RivalryDTO rivalry) {
    return buildPairHistoryDetails(rivalry);
  }

  /**
   * The pair's full feud-cycle history (active + ended rows, newest first) for the expandable
   * details row (ATW-8nc8).
   */
  private Component buildPairHistoryDetails(RivalryDTO rivalry) {
    List<RivalryDTO> history =
        rivalryService
            .getRivalryHistoryBetween(
                rivalry.getWrestler1().getId(), rivalry.getWrestler2().getId())
            .stream()
            .map(rivalryService.getRivalryMapper()::toRivalryDTO)
            .toList();
    VerticalLayout details = new VerticalLayout();
    details.setPadding(false);
    details.setSpacing(false);
    if (history.size() <= 1) {
      details.add(new Span("First feud cycle for this pair."));
      return details;
    }
    Grid<RivalryDTO> historyGrid = new Grid<>();
    historyGrid.addColumn(RivalryDTO::getHeat).setHeader("Heat").setWidth("80px");
    historyGrid.addColumn(RivalryDTO::getStartedDate).setHeader("Start").setWidth("170px");
    historyGrid.addColumn(RivalryDTO::getEndedDate).setHeader("End").setWidth("170px");
    historyGrid.addColumn(RivalryDTO::getStorylineNotes).setHeader("Notes").setFlexGrow(1);
    historyGrid.setItems(history);
    details.add(historyGrid);
    return details;
  }
}

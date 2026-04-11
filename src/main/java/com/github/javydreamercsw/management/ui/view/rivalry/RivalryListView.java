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
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.transaction.annotation.Transactional;

@Route("rivalry-list")
@PageTitle("Rivalry List")
@Menu(order = 1, icon = "vaadin:flame", title = "Rivalry List")
@PermitAll
@Transactional(readOnly = true)
public class RivalryListView extends Main {

  private final RivalryService rivalryService;
  private final RivalryRepository rivalryRepository;
  private final WrestlerService wrestlerService;
  private final WrestlerRepository wrestlerRepository;
  private final SecurityUtils securityUtils;

  final Grid<Rivalry> rivalryGrid;

  public RivalryListView(
      @NonNull RivalryService rivalryService,
      @NonNull RivalryRepository rivalryRepository,
      @NonNull WrestlerService wrestlerService,
      @NonNull WrestlerRepository wrestlerRepository,
      @NonNull SecurityUtils securityUtils) {
    this.rivalryService = rivalryService;
    this.rivalryRepository = rivalryRepository;
    this.wrestlerService = wrestlerService;
    this.wrestlerRepository = wrestlerRepository;
    this.securityUtils = securityUtils;

    ComboBox<Wrestler> wrestler1ComboBox = new ComboBox<>("Wrestler 1");
    wrestler1ComboBox.setItems(
        wrestlerRepository.findAll().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    wrestler1ComboBox.setItemLabelGenerator(Wrestler::getName);

    ComboBox<Wrestler> wrestler2ComboBox = new ComboBox<>("Wrestler 2");
    wrestler2ComboBox.setItems(
        wrestlerRepository.findAll().stream()
            .sorted(Comparator.comparing(Wrestler::getName))
            .collect(Collectors.toList()));
    wrestler2ComboBox.setItemLabelGenerator(Wrestler::getName);

    TextField storylineNotes = new TextField("Storyline Notes");

    rivalryGrid = new Grid<>();

    Button createButton =
        new Button(
            "Create",
            event -> {
              rivalryService.createRivalry(
                  wrestler1ComboBox.getValue().getId(),
                  wrestler2ComboBox.getValue().getId(),
                  storylineNotes.getValue());
              rivalryGrid.getDataProvider().refreshAll();
              wrestler1ComboBox.clear();
              wrestler2ComboBox.clear();
              storylineNotes.clear();
              Notification.show("Rivalry created", 2000, Notification.Position.BOTTOM_END)
                  .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.setVisible(securityUtils.canCreate());

    rivalryGrid.setItems(
        query -> rivalryService.getAllRivalriesWithWrestlers(toSpringPageRequest(query)).stream());
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
        .addColumn(Rivalry::getHeat)
        .setHeader("Heat")
        .setSortable(true)
        .setSortProperty("heat");
    rivalryGrid
        .addColumn(Rivalry::getStorylineNotes)
        .setHeader("Notes")
        .setSortable(true)
        .setSortProperty("storylineNotes");
    rivalryGrid
        .addColumn(Rivalry::getStartedDate)
        .setHeader("Start Date")
        .setSortable(true)
        .setSortProperty("startedDate");
    rivalryGrid
        .addColumn(Rivalry::getEndedDate)
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
                    rivalryRepository.deleteById(rivalry.getId());
                    rivalryGrid.getDataProvider().refreshAll();
                    Notification.show("Rivalry deleted", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                  });
              deleteButton.setVisible(securityUtils.canDelete());
              return deleteButton;
            })
        .setHeader("Delete");

    rivalryGrid.setSizeFull();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    if (securityUtils.canCreate()) {
      add(
          new ViewToolbar(
              "Rivalry List",
              ViewToolbar.group(
                  wrestler1ComboBox, wrestler2ComboBox, storylineNotes, createButton)));
    } else {
      add(new ViewToolbar("Rivalry List"));
    }
    add(rivalryGrid);
  }
}

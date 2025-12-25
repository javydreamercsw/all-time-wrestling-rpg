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
package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Route("wrestler-rankings")
@PageTitle("Wrestler Rankings")
@Menu(order = 4, icon = "vaadin:trophy", title = "Wrestler Rankings")
@PermitAll
@Slf4j
public class WrestlerRankingsView extends Main {

  private final WrestlerService wrestlerService;
  private final TitleService titleService;
  private final TierBoundaryService tierBoundaryService;
  private final Grid<Wrestler> grid = new Grid<>(Wrestler.class, false);
  private Set<Long> championIds = new HashSet<>();
  private ComboBox<Gender> genderComboBox;

  public WrestlerRankingsView(
      WrestlerService wrestlerService,
      TitleService titleService,
      TierBoundaryService tierBoundaryService) {
    this.wrestlerService = wrestlerService;
    this.titleService = titleService;
    this.tierBoundaryService = tierBoundaryService;
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.MEDIUM);
    setHeightFull();

    add(createToolbar());
    configureGrid();
    VerticalLayout content = new VerticalLayout(grid);
    content.setSizeFull();
    content.setFlexGrow(1, grid);
    content.setFlexGrow(1); // Make the VerticalLayout itself grow
    add(content);
    updateList();
  }

  private ViewToolbar createToolbar() {
    Button showTierBoundariesButton = new Button("Show Tier Boundaries");
    showTierBoundariesButton.addClickListener(event -> showTierBoundariesDialog());

    genderComboBox = new ComboBox<>("Gender");
    genderComboBox.setId("gender-selection");
    genderComboBox.setItems(Gender.values());
    genderComboBox.setItemLabelGenerator(Gender::name);
    genderComboBox.setClearButtonVisible(true);
    genderComboBox.addValueChangeListener(event -> updateList());

    return new ViewToolbar(
        "Wrestler Rankings", ViewToolbar.group(genderComboBox, showTierBoundariesButton));
  }

  private void showTierBoundariesDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Tier Boundaries");
    dialog.setWidth("40em");

    Grid<TierBoundary> tierGrid = new Grid<>(TierBoundary.class, false);
    tierGrid.addColumn(tb -> tb.getTier().getDisplayWithEmoji()).setHeader("Tier");
    tierGrid
        .addColumn(tb -> String.format("%,d - %,d", tb.getMinFans(), tb.getMaxFans()))
        .setHeader("Fan Range");
    tierGrid
        .addColumn(tb -> String.format("%,d", tb.getChallengeCost()))
        .setHeader("Challenge Cost");
    tierGrid
        .addColumn(tb -> String.format("%,d", tb.getContenderEntryFee()))
        .setHeader("Contender Entry Fee");

    ComboBox<Gender> genderDialogComboBox = new ComboBox<>("Gender");
    genderDialogComboBox.setItems(Gender.values());
    genderDialogComboBox.setValue(Gender.MALE);
    genderDialogComboBox.addValueChangeListener(
        event -> {
          List<TierBoundary> tierBoundaries =
              tierBoundaryService.findAllByGender(event.getValue()).stream()
                  .sorted(Comparator.comparing(TierBoundary::getMinFans).reversed())
                  .collect(Collectors.toList());
          log.info(
              "Found {} tier boundaries to display for {}",
              tierBoundaries.size(),
              event.getValue());
          tierGrid.setItems(tierBoundaries);
        });

    List<TierBoundary> tierBoundaries =
        tierBoundaryService.findAllByGender(Gender.MALE).stream()
            .sorted(Comparator.comparing(TierBoundary::getMinFans).reversed())
            .collect(Collectors.toList());
    log.info("Found {} tier boundaries to display for MALE", tierBoundaries.size());
    tierGrid.setItems(tierBoundaries);

    dialog.add(genderDialogComboBox, tierGrid);
    dialog.open();
  }

  private void configureGrid() {
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.setSizeFull();
    grid.setHeightFull();

    grid.addComponentColumn(
            wrestler -> {
              HorizontalLayout layout = new HorizontalLayout();
              layout.setAlignItems(FlexComponent.Alignment.CENTER);

              Span nameSpan = new Span(wrestler.getName());
              nameSpan.addClassNames(
                  "wrestler-tier-" + wrestler.getTier().name().toLowerCase(),
                  "wrestler-tier-badge");
              layout.add(nameSpan);

              // Add a star icon for champions
              if (this.championIds.contains(wrestler.getId())) {
                Icon trophyIcon = VaadinIcon.TROPHY.create();
                trophyIcon.addClickListener(
                    event -> {
                      // Show title details in a notification or dialog
                      Notification.show(
                          "Champion: "
                              + wrestler.getName()
                              + " holds "
                              + titleService.findTitlesByChampion(wrestler).size()
                              + " titles.");
                    });
                layout.add(trophyIcon);
              }
              return layout;
            })
        .setHeader("Wrestler")
        .setSortable(true)
        .setComparator(Wrestler::getName);

    grid.addColumn(Wrestler::getFans).setHeader("Fans").setSortable(true);
    grid.addColumn(wrestler -> wrestler.getTier().getDisplayWithEmoji())
        .setHeader("Tier")
        .setSortable(true);
  }

  private void updateList() {
    this.championIds =
        titleService.findAll().stream()
            .filter(title -> title.getChampion() != null && !title.getChampion().isEmpty())
            .flatMap(title -> title.getChampion().stream())
            .map(Wrestler::getId)
            .collect(Collectors.toSet());

    Gender selectedGender = genderComboBox.getValue();
    List<Wrestler> wrestlers = wrestlerService.findAll();
    if (selectedGender != null) {
      wrestlers =
          wrestlers.stream()
              .filter(w -> w.getGender() == selectedGender)
              .collect(Collectors.toList());
    }
    grid.setItems(wrestlers);
  }
}

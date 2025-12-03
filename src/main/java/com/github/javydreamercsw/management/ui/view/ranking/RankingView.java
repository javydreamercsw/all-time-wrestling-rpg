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
package com.github.javydreamercsw.management.ui.view.ranking;

import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.transaction.annotation.Transactional;

@Route(value = "championship-rankings", layout = MainLayout.class)
@PageTitle("Championship Rankings")
@PermitAll
@Transactional(readOnly = true)
public class RankingView extends Main {

  private final RankingService rankingService;

  private final Image championshipImage = new Image();
  private final VerticalLayout championLayout = new VerticalLayout();
  private final Grid<RankedWrestlerDTO> contendersGrid = new Grid<>();
  private final ComboBox<ChampionshipDTO> championshipComboBox;

  public RankingView(@NonNull RankingService rankingService) {
    this.rankingService = rankingService;

    championshipComboBox = new ComboBox<>("Championship");
    championshipComboBox.setItems(
        rankingService.getChampionships().stream()
            .sorted(Comparator.comparing(ChampionshipDTO::getName))
            .collect(Collectors.toList()));
    championshipComboBox.setItemLabelGenerator(ChampionshipDTO::getName);
    championshipComboBox.addValueChangeListener(event -> updateView(event.getValue()));

    // Select the first championship by default
    rankingService.getChampionships().stream()
        .sorted(Comparator.comparing(ChampionshipDTO::getName))
        .findFirst()
        .ifPresent(championshipComboBox::setValue);

    contendersGrid
        .addColumn(
            new ComponentRenderer<>(
                wrestler -> {
                  Span span = new Span(String.valueOf(wrestler.getRank()));
                  if (wrestler.getRank() == 1) {
                    span.getStyle().set("font-weight", "bold");
                  }
                  return span;
                }))
        .setHeader("Rank");
    contendersGrid.addColumn(RankedWrestlerDTO::getName).setHeader("Name");
    contendersGrid.addColumn(RankedWrestlerDTO::getFans).setHeader("Fans");

    championshipImage.setId("championship-image");
    championshipImage.setMaxHeight("300px");

    HorizontalLayout topLayout = new HorizontalLayout(championshipComboBox);
    topLayout.setAlignItems(Alignment.CENTER);

    add(topLayout, championLayout, championshipImage, contendersGrid);
  }

  private void updateView(ChampionshipDTO championship) {
    if (championship == null) {
      championshipImage.setVisible(false);
      championLayout.setVisible(false);
      contendersGrid.setVisible(false);
      return;
    }

    championshipImage.setVisible(true);
    championLayout.setVisible(true);
    contendersGrid.setVisible(true);

    // Update championship image
    String imagePath = "images/championships/" + championship.getImageName();
    championshipImage.setSrc(imagePath);

    // Update champion
    championLayout.removeAll();
    List<ChampionDTO> champions = rankingService.getCurrentChampions(championship.getId());
    if (!champions.isEmpty()) {
      championLayout.add(new H2("Current Champion(s)"));
      String championNames =
          champions.stream().map(ChampionDTO::getName).collect(Collectors.joining(" & "));
      championLayout.add(new H2(championNames));
      championLayout.add("Fans: " + champions.get(0).getFans());
      championLayout.add(" Reign: " + champions.get(0).getReignDays() + " days");
    }

    // Update contenders
    contendersGrid.setItems(rankingService.getRankedContenders(championship.getId()));
  }
}

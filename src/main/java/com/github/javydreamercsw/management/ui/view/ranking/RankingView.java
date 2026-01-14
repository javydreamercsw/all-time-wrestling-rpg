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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.management.dto.ranking.ChampionDTO;
import com.github.javydreamercsw.management.dto.ranking.ChampionshipDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedTeamDTO;
import com.github.javydreamercsw.management.dto.ranking.RankedWrestlerDTO;
import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.ui.component.HistoryTimelineComponent;
import com.github.javydreamercsw.management.ui.component.ReignCardComponent;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Route(value = "championship-rankings", layout = MainLayout.class)
@PageTitle("Championship Rankings")
@PermitAll
@Transactional(readOnly = true)
@Slf4j
public class RankingView extends Main {

  private final RankingService rankingService;
  private final TierBoundaryService tierBoundaryService;

  private final Image championshipImage = new Image();
  private final VerticalLayout championLayout = new VerticalLayout();
  private final VerticalLayout historyLayout = new VerticalLayout();
  private final Grid<RankedWrestlerDTO> wrestlerContendersGrid = new Grid<>();
  private final Grid<RankedTeamDTO> teamContendersGrid = new Grid<>();

  public RankingView(
      @NonNull RankingService rankingService, @NonNull TierBoundaryService tierBoundaryService) {
    this.rankingService = rankingService;
    this.tierBoundaryService = tierBoundaryService;

    ComboBox<ChampionshipDTO> championshipComboBox = new ComboBox<>("Championship");
    championshipComboBox.setId("championship-combo-box");
    championshipComboBox.setItems(
        rankingService.getChampionships().stream()
            .sorted(Comparator.comparing(ChampionshipDTO::getName))
            .collect(Collectors.toList()));
    championshipComboBox.setItemLabelGenerator(ChampionshipDTO::getName);
    championshipComboBox.addValueChangeListener(event -> updateView(event.getValue()));

    // Select the first championship by default
    rankingService.getChampionships().stream()
        .min(Comparator.comparing(ChampionshipDTO::getName))
        .ifPresent(championshipComboBox::setValue);

    wrestlerContendersGrid
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
    wrestlerContendersGrid.addColumn(RankedWrestlerDTO::getName).setHeader("Name");
    wrestlerContendersGrid.addColumn(RankedWrestlerDTO::getFans).setHeader("Fans");
    wrestlerContendersGrid.setId("wrestler-contenders-grid");

    teamContendersGrid
        .addColumn(
            new ComponentRenderer<>(
                team -> {
                  Span span = new Span(String.valueOf(team.getRank()));
                  if (team.getRank() == 1) {
                    span.getStyle().set("font-weight", "bold");
                  }
                  return span;
                }))
        .setHeader("Rank");
    teamContendersGrid.addColumn(RankedTeamDTO::getName).setHeader("Name");
    teamContendersGrid.addColumn(RankedTeamDTO::getFans).setHeader("Fans");
    teamContendersGrid.setId("team-contenders-grid");

    championshipImage.setId("championship-image");
    championshipImage.setMaxHeight("300px");
    championshipImage.setWidth("auto");
    championshipImage.getStyle().set("object-fit", "contain");

    Button showTierBoundariesButton = new Button("Show Tier Boundaries");
    showTierBoundariesButton.setId("show-tier-boundaries-button");
    showTierBoundariesButton.addClickListener(event -> showTierBoundariesDialog());

    HorizontalLayout topLayout =
        new HorizontalLayout(championshipComboBox, showTierBoundariesButton);
    topLayout.setAlignItems(Alignment.CENTER);

    add(
        topLayout,
        championLayout,
        championshipImage,
        wrestlerContendersGrid,
        teamContendersGrid,
        historyLayout);
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

    ComboBox<Gender> genderComboBox = new ComboBox<>("Gender");
    genderComboBox.setItems(Gender.values());
    genderComboBox.setId("gender-selection");
    genderComboBox.setItemLabelGenerator(Gender::name);
    genderComboBox.setValue(Gender.MALE); // Default to male

    genderComboBox.addValueChangeListener(
        event -> {
          List<TierBoundary> tierBoundaries =
              tierBoundaryService.findAllByGender(event.getValue()).stream()
                  .sorted(Comparator.comparing(TierBoundary::getMinFans).reversed())
                  .collect(Collectors.toList());
          log.info(
              "Found {} tier boundaries for {} to display.",
              tierBoundaries.size(),
              event.getValue());
          tierGrid.setItems(tierBoundaries);
        });

    // Initial load
    List<TierBoundary> initialBoundaries =
        tierBoundaryService.findAllByGender(Gender.MALE).stream()
            .sorted(Comparator.comparing(TierBoundary::getMinFans).reversed())
            .collect(Collectors.toList());
    log.info("Found {} tier boundaries for MALE to display.", initialBoundaries.size());
    tierGrid.setItems(initialBoundaries);

    dialog.add(genderComboBox, tierGrid);
    dialog.open();
  }

  private void updateView(ChampionshipDTO championship) {
    if (championship == null) {
      championshipImage.setVisible(false);
      championLayout.setVisible(false);
      wrestlerContendersGrid.setVisible(false);
      teamContendersGrid.setVisible(false);
      return;
    }

    championshipImage.setVisible(true);
    championLayout.setVisible(true);

    // Update championship image using static resource URL
    // Resources in META-INF/resources are served relative to the context path
    String imageName = championship.getImageName();
    // Use relative path (no leading slash) so Vaadin automatically includes context path
    String imageUrl = "images/championships/" + imageName;

    log.debug("Loading championship image for '{}' at URL: {}", championship.getName(), imageUrl);

    championshipImage.setSrc(imageUrl);
    championshipImage.setAlt(championship.getName() + " Championship");

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
    List<?> contenders = rankingService.getRankedContenders(championship.getId());
    if (!contenders.isEmpty()) {
      if (contenders.get(0) instanceof RankedWrestlerDTO) {
        wrestlerContendersGrid.setVisible(true);
        teamContendersGrid.setVisible(false);
        wrestlerContendersGrid.setItems((List<RankedWrestlerDTO>) contenders);
      } else {
        wrestlerContendersGrid.setVisible(false);
        teamContendersGrid.setVisible(true);
        teamContendersGrid.setItems((List<RankedTeamDTO>) contenders);
      }
    } else {
      wrestlerContendersGrid.setVisible(false);
      teamContendersGrid.setVisible(false);
    }

    // Update history
    historyLayout.removeAll();
    historyLayout.add(new H3("Championship History"));
    List<TitleReignDTO> history = rankingService.getTitleReignHistory(championship.getId());
    if (history.isEmpty()) {
      historyLayout.add("No history available.");
    } else {
      historyLayout.add(new HistoryTimelineComponent(history));
      Div cardsContainer = new Div();
      cardsContainer.addClassNames(
          LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.SMALL);
      history.forEach(reign -> cardsContainer.add(new ReignCardComponent(reign)));
      historyLayout.add(cardsContainer);
    }
  }
}

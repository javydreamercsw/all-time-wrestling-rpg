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
package com.github.javydreamercsw.management.ui.component;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.dto.SeasonStatsDTO;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import java.util.function.Consumer;

/** Component to display a summary of a wrestler's performance in a season. */
public class SeasonSummaryComponent extends VerticalLayout {

  private final ComboBox<Season> seasonSelector;
  private final VerticalLayout statsLayout;

  public SeasonSummaryComponent(List<Season> seasons, Consumer<Season> onSeasonChange) {
    setSpacing(true);
    setPadding(true);
    addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.BoxShadow.SMALL,
        LumoUtility.Padding.MEDIUM);

    seasonSelector = new ComboBox<>("Select Season");
    seasonSelector.setItems(seasons);
    seasonSelector.setItemLabelGenerator(Season::getName);
    seasonSelector.setWidthFull();
    seasonSelector.addValueChangeListener(
        event -> {
          if (event.getValue() != null) {
            onSeasonChange.accept(event.getValue());
          }
        });

    statsLayout = new VerticalLayout();
    statsLayout.setPadding(false);
    statsLayout.setSpacing(true);

    add(seasonSelector, statsLayout);
  }

  public void updateStats(SeasonStatsDTO stats) {
    statsLayout.removeAll();

    if (stats == null) {
      statsLayout.add(new Span("No data available for this season."));
      return;
    }

    H4 title = new H4(stats.getSeasonName() + " Summary");
    title.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);

    Span record =
        new Span(
            String.format(
                "Record: %d-%d-%d", stats.getWins(), stats.getLosses(), stats.getDraws()));
    record.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.BOLD);

    VerticalLayout fanGrowthLayout = new VerticalLayout();
    fanGrowthLayout.setPadding(false);
    fanGrowthLayout.setSpacing(false);

    Span fanLabel = new Span("Fan Growth");
    fanLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

    long growth = stats.getFanGrowth();
    String growthText = (growth >= 0 ? "+" : "") + growth + " fans";
    Span growthSpan = new Span(growthText);
    growthSpan.addClassNames(
        LumoUtility.FontSize.SMALL,
        growth >= 0 ? LumoUtility.TextColor.SUCCESS : LumoUtility.TextColor.ERROR);

    ProgressBar fanProgress = new ProgressBar();
    // Assuming a max growth goal of 1000 for the progress bar visualization
    double progress = Math.min(1.0, Math.max(0.0, growth / 1000.0));
    fanProgress.setValue(progress);

    fanGrowthLayout.add(new VerticalLayout(fanLabel, growthSpan), fanProgress);

    statsLayout.add(title, record, fanGrowthLayout);

    if (stats.getAccolades() != null && !stats.getAccolades().isEmpty()) {
      Span accoladesTitle = new Span("Accolades: " + String.join(", ", stats.getAccolades()));
      accoladesTitle.addClassNames(
          LumoUtility.FontSize.SMALL, LumoUtility.Margin.Top.SMALL, LumoUtility.FontWeight.MEDIUM);
      statsLayout.add(accoladesTitle);
    }
  }

  public void setSelectedSeason(Season season) {
    seasonSelector.setValue(season);
  }
}

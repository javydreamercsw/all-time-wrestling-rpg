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

import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.ui.view.show.ShowDetailView;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class ReignCardComponent extends Composite<Div> {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
          .withLocale(Locale.US)
          .withZone(ZoneId.systemDefault());

  public ReignCardComponent(TitleReignDTO reign) {
    getContent()
        .addClassNames(
            Display.FLEX,
            FlexDirection.COLUMN,
            Padding.MEDIUM,
            Border.ALL,
            BorderRadius.MEDIUM,
            Background.CONTRAST_5,
            Gap.XSMALL);

    if (reign.isCurrent()) {
      getContent().addClassNames(Border.ALL, BoxShadow.SMALL);
      Span currentBadge = new Span("CURRENT CHAMPION");
      currentBadge.addClassNames(FontSize.XSMALL, FontWeight.BOLD, TextColor.SUCCESS);
      getContent().add(currentBadge);
    }

    Span title = new Span(reign.getChampionshipName());
    title.addClassNames(FontSize.LARGE, FontWeight.BOLD);

    Span tier = new Span(reign.getChampionshipTier());
    tier.addClassNames(FontSize.XSMALL, TextColor.SECONDARY);

    Span champions = new Span("Champions: " + String.join(" & ", reign.getChampionNames()));
    champions.addClassNames(FontWeight.MEDIUM);

    Span duration =
        new Span(String.format("%s (%d days)", formatPeriod(reign), reign.getDurationDays()));
    duration.addClassNames(FontSize.SMALL);

    getContent().add(title, tier, champions, duration);

    if (reign.getWonAtShowId() != null) {
      RouterLink matchLink =
          new RouterLink(
              "Won at: " + reign.getWonAtShowName(), ShowDetailView.class, reign.getWonAtShowId());
      matchLink.addClassNames(FontSize.SMALL, TextColor.PRIMARY);

      Tooltip tooltip = Tooltip.forComponent(matchLink);
      tooltip.setText("View details for " + reign.getWonAtShowName());

      getContent().add(matchLink);
    }
  }

  private String formatPeriod(TitleReignDTO reign) {
    String start = DATE_FORMATTER.format(reign.getStartDate());
    String end = reign.getEndDate() != null ? DATE_FORMATTER.format(reign.getEndDate()) : "Present";
    return start + " - " + end;
  }
}

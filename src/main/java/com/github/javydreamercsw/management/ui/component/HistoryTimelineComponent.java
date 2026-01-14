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
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

public class HistoryTimelineComponent extends Composite<Div> {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
          .withLocale(Locale.US)
          .withZone(ZoneId.systemDefault());

  public HistoryTimelineComponent(List<TitleReignDTO> history) {
    getContent().addClassNames(Display.FLEX, Overflow.AUTO, Padding.Vertical.MEDIUM, Gap.SMALL);
    getContent().getStyle().set("scrollbar-width", "thin");

    for (int i = history.size() - 1; i >= 0; i--) {
      TitleReignDTO reign = history.get(i);
      getContent().add(createTimelineItem(reign, i == 0));
      if (i > 0) {
        getContent().add(createConnector());
      }
    }
  }

  private Div createTimelineItem(TitleReignDTO reign, boolean isCurrent) {
    Div item = new Div();
    item.addClassNames(
        Display.FLEX,
        FlexDirection.COLUMN,
        AlignItems.CENTER,
        Padding.SMALL,
        Border.ALL,
        BorderRadius.MEDIUM,
        Background.BASE);

    if (isCurrent && reign.isCurrent()) {
      item.addClassNames(Border.ALL, BoxShadow.SMALL);
      Span badge = new Span("CURRENT");
      badge.addClassNames(FontSize.XXSMALL, FontWeight.BOLD, TextColor.SUCCESS);
      item.add(badge);
    }

    Span name = new Span(String.join(" & ", reign.getChampionNames()));
    name.addClassNames(FontWeight.BOLD, FontSize.SMALL, TextAlignment.CENTER);

    Span dates =
        new Span(
            String.format(
                "%s - %s",
                DATE_FORMATTER.format(reign.getStartDate()),
                reign.getEndDate() != null
                    ? DATE_FORMATTER.format(reign.getEndDate())
                    : "Present"));
    dates.addClassNames(FontSize.XXSMALL, TextColor.SECONDARY);

    item.add(name, dates);
    item.setMinWidth("120px");
    return item;
  }

  private Div createConnector() {
    Div connector = new Div();
    connector.addClassNames(AlignSelf.CENTER, Background.CONTRAST_30);
    connector.setHeight("2px");
    connector.setWidth("20px");
    return connector;
  }
}

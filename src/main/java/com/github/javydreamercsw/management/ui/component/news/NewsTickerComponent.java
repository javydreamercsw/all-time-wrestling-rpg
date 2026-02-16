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
package com.github.javydreamercsw.management.ui.component.news;

import com.github.javydreamercsw.management.domain.news.NewsItem;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.NonNull;

public class NewsTickerComponent extends Composite<Div> {

  private final NewsService newsService;
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("MMM dd, HH:mm").withZone(ZoneId.systemDefault());

  public NewsTickerComponent(@NonNull NewsService newsService) {
    this.newsService = newsService;

    getContent().setId("news-ticker-component");
    getContent()
        .addClassNames(
            LumoUtility.Background.CONTRAST_5,
            LumoUtility.BorderRadius.MEDIUM,
            LumoUtility.Padding.SMALL,
            LumoUtility.Margin.Vertical.SMALL,
            LumoUtility.Display.FLEX,
            LumoUtility.AlignItems.CENTER,
            LumoUtility.Gap.SMALL,
            LumoUtility.Overflow.HIDDEN);
    getContent().getStyle().set("cursor", "pointer");

    refresh();
  }

  public void refresh() {
    getContent().removeAll();
    List<NewsItem> latest = newsService.getLatestNews();

    if (latest.isEmpty()) {
      Span empty = new Span("No recent news or rumors.");
      empty.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
      getContent().add(empty);
      return;
    }

    NewsItem item = latest.get(0); // Show the very latest

    Span categoryBadge = new Span(item.getCategory().getEmoji());
    categoryBadge.addClassNames(LumoUtility.FontWeight.BOLD);

    Span headline = new Span(item.getHeadline());
    headline.addClassNames(
        LumoUtility.FontSize.SMALL,
        LumoUtility.FontWeight.MEDIUM,
        LumoUtility.TextOverflow.ELLIPSIS);

    Span time = new Span(formatter.format(item.getPublishDate()));
    time.addClassNames(
        LumoUtility.FontSize.XXSMALL,
        LumoUtility.TextColor.SECONDARY,
        LumoUtility.Margin.Left.AUTO);

    getContent().add(categoryBadge, headline, time);
    getContent().addClickListener(e -> showDetails(item));
  }

  private void showDetails(NewsItem item) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(item.getCategory().getEmoji() + " " + item.getHeadline());

    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);

    Span meta =
        new Span(
            item.getCategory().getDisplayName() + " • " + formatter.format(item.getPublishDate()));
    meta.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

    Paragraph body = new Paragraph(item.getContent());

    content.add(meta, body);

    if (item.getImportance() >= 4) {
      HorizontalLayout importance = new HorizontalLayout();
      importance.setAlignItems(FlexComponent.Alignment.CENTER);
      importance.add(new Span("Priority: "));
      for (int i = 0; i < item.getImportance(); i++) {
        Icon star = VaadinIcon.STAR.create();
        star.setSize("12px");
        star.setColor("gold");
        importance.add(star);
      }
      content.add(importance);
    }

    dialog.add(content);
    dialog
        .getFooter()
        .add(new com.vaadin.flow.component.button.Button("Close", e -> dialog.close()));
    dialog.open();
  }
}

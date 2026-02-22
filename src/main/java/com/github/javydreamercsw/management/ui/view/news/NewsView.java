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
package com.github.javydreamercsw.management.ui.view.news;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.news.NewsItem;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import lombok.NonNull;

@Route("news")
@PageTitle("News & Rumors")
@Menu(order = 5, icon = "vaadin:newspaper", title = "News & Rumors")
@PermitAll
public class NewsView extends Main {

  private final NewsService newsService;
  private final NewsGenerationService newsGenerationService;
  private final Grid<NewsItem> newsGrid;
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  public NewsView(
      @NonNull NewsService newsService,
      @NonNull NewsGenerationService newsGenerationService,
      @NonNull SecurityUtils securityUtils) {
    this.newsService = newsService;
    this.newsGenerationService = newsGenerationService;
    this.newsGrid = new Grid<>();

    setSizeFull();
    addClassNames(
        LumoUtility.BoxSizing.BORDER,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Gap.SMALL);

    add(new ViewToolbar("News & Rumors"));

    if (securityUtils.canCreate()) {
      com.vaadin.flow.component.button.Button createButton =
          new com.vaadin.flow.component.button.Button(
              "Create News",
              e -> {
                NewsDialog dialog = new NewsDialog(newsService, this::reloadGrid);
                dialog.open();
              });
      createButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);

      com.vaadin.flow.component.button.Button synthButton =
          new com.vaadin.flow.component.button.Button(
              "Generate Monthly Synthesis",
              e -> {
                newsGenerationService.generateMonthlySynthesis();
                reloadGrid();
                com.vaadin.flow.component.notification.Notification.show(
                    "Monthly synthesis generated.");
              });
      synthButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);

      add(new com.vaadin.flow.component.orderedlayout.HorizontalLayout(createButton, synthButton));
    }

    setupGrid();
    add(newsGrid);

    reloadGrid();
  }

  private void setupGrid() {
    newsGrid
        .addColumn(item -> formatter.format(item.getPublishDate()))
        .setHeader("Date")
        .setSortable(true)
        .setComparator(Comparator.comparing(NewsItem::getPublishDate))
        .setWidth("160px")
        .setFlexGrow(0);

    newsGrid
        .addComponentColumn(
            item -> {
              HorizontalLayout layout = new HorizontalLayout();
              layout.setAlignItems(FlexComponent.Alignment.CENTER);

              Span category =
                  new Span(
                      item.getCategory().getEmoji() + " " + item.getCategory().getDisplayName());
              category.getElement().getThemeList().add("badge");
              if (item.getIsRumor()) {
                category.getElement().getThemeList().add("contrast");
              }

              layout.add(category);
              return layout;
            })
        .setHeader("Category")
        .setWidth("150px")
        .setFlexGrow(0);

    newsGrid
        .addColumn(NewsItem::getHeadline)
        .setHeader("Headline")
        .setSortable(true)
        .setFlexGrow(1);

    newsGrid
        .addComponentColumn(
            item -> {
              HorizontalLayout importance = new HorizontalLayout();
              for (int i = 0; i < item.getImportance(); i++) {
                Icon star = VaadinIcon.STAR.create();
                star.setSize("12px");
                star.setColor("gold");
                importance.add(star);
              }
              return importance;
            })
        .setHeader("Importance")
        .setWidth("120px")
        .setFlexGrow(0);

    newsGrid.setItemDetailsRenderer(
        new com.vaadin.flow.data.renderer.ComponentRenderer<>(
            item -> {
              Span content = new Span(item.getContent());
              content.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Display.BLOCK);
              return content;
            }));

    newsGrid.setSizeFull();
  }

  private void reloadGrid() {
    newsGrid.setItems(newsService.getAllNews());
  }
}

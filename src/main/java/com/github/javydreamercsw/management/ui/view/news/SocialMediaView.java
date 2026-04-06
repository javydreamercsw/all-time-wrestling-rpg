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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.news.NewsItem;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.ui.view.MainLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.NonNull;

@Route(value = "news/feed", layout = MainLayout.class)
@PageTitle("Wrestling World Social Feed")
@PermitAll
public class SocialMediaView extends VerticalLayout {

  private final NewsService newsService;
  private final ObjectMapper objectMapper;
  private final VerticalLayout feedContainer;
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("MMM dd, HH:mm").withZone(ZoneId.systemDefault());

  public SocialMediaView(
      @NonNull NewsService newsService,
      @NonNull SecurityUtils securityUtils,
      @NonNull ObjectMapper objectMapper) {
    this.newsService = newsService;
    this.objectMapper = objectMapper;

    setSpacing(true);
    setPadding(true);
    setAlignItems(Alignment.CENTER);

    H2 title = new H2("Wrestling World Feed");
    title.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
    add(title);

    add(createNewsletterSection());

    if (securityUtils.canCreate()) {
      add(createPostEditor());
    }

    feedContainer = new VerticalLayout();
    feedContainer.setWidthFull();
    feedContainer.setMaxWidth("600px");
    feedContainer.setPadding(false);
    feedContainer.setSpacing(true);
    add(feedContainer);

    refreshFeed();
  }

  private HorizontalLayout createNewsletterSection() {
    HorizontalLayout newsletter = new HorizontalLayout();
    newsletter.setWidthFull();
    newsletter.setMaxWidth("600px");
    newsletter.setAlignItems(Alignment.CENTER);
    newsletter.addClassNames(
        LumoUtility.Background.CONTRAST_10,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Margin.Bottom.SMALL);

    Icon icon = VaadinIcon.NEWSPAPER.create();
    icon.setColor("var(--lumo-primary-text-color)");

    VerticalLayout text = new VerticalLayout();
    text.setPadding(false);
    text.setSpacing(false);

    Span header = new Span("Monthly Newsletter");
    header.addClassNames(LumoUtility.FontWeight.BOLD);
    Span sub = new Span("Download the latest deep-dive analysis.");
    sub.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.TextColor.SECONDARY);
    text.add(header, sub);

    newsletter.add(icon, text);
    newsletter.expand(text);

    newsService
        .getLatestMonthlyAnalysis()
        .ifPresent(
            item -> {
              Anchor downloadAnchor =
                  new Anchor(
                      DownloadHandler.fromInputStream(
                          (event) -> {
                            try {
                              String json =
                                  objectMapper
                                      .writerWithDefaultPrettyPrinter()
                                      .writeValueAsString(item);
                              byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
                              return new DownloadResponse(
                                  new ByteArrayInputStream(jsonBytes),
                                  "monthly_wrapup.json",
                                  "application/json",
                                  jsonBytes.length);
                            } catch (Exception e) {
                              return DownloadResponse.error(500);
                            }
                          }),
                      "");

              Button downloadBtn = new Button("Download Latest", VaadinIcon.DOWNLOAD.create());
              downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
              downloadAnchor.add(downloadBtn);

              newsletter.add(downloadAnchor);
            });

    return newsletter;
  }

  private Section createPostEditor() {
    Section editor = new Section();
    editor.setWidthFull();
    editor.setMaxWidth("600px");
    editor.addClassNames(
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Margin.Bottom.MEDIUM);

    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(true);

    TextArea postContent = new TextArea();
    postContent.setPlaceholder("What's happening in the wrestling world?");
    postContent.setWidthFull();
    postContent.setHeight("100px");

    Button postButton =
        new Button(
            "Post",
            e -> {
              if (!postContent.getValue().isBlank()) {
                newsService.createNewsItem(
                    "User Post", postContent.getValue(), NewsCategory.BREAKING, false, 3);
                postContent.clear();
                refreshFeed();
              }
            });
    postButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout footer = new HorizontalLayout(postButton);
    footer.setWidthFull();
    footer.setJustifyContentMode(JustifyContentMode.END);

    layout.add(postContent, footer);
    editor.add(layout);
    return editor;
  }

  private void refreshFeed() {
    feedContainer.removeAll();
    List<NewsItem> news = newsService.getAllNews();

    for (NewsItem item : news) {
      feedContainer.add(createPostCard(item));
    }
  }

  private VerticalLayout createPostCard(NewsItem item) {
    VerticalLayout card = new VerticalLayout();
    card.setWidthFull();
    card.addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderColor.CONTRAST_10,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.BoxShadow.SMALL);

    // Header
    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setAlignItems(Alignment.CENTER);

    Avatar avatar = new Avatar("ATW News");
    avatar.addClassNames(LumoUtility.Margin.Right.SMALL);

    VerticalLayout authorInfo = new VerticalLayout();
    authorInfo.setPadding(false);
    authorInfo.setSpacing(false);

    Span name = new Span("ATW News Bot");
    name.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);

    Span handle = new Span("@atw_official • " + formatter.format(item.getPublishDate()));
    handle.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.TextColor.SECONDARY);

    authorInfo.add(name, handle);

    Span categoryBadge =
        new Span(item.getCategory().getEmoji() + " " + item.getCategory().getDisplayName());
    categoryBadge.getElement().getThemeList().add("badge small");
    if (item.getIsRumor()) {
      categoryBadge.getElement().getThemeList().add("contrast");
    }

    header.add(avatar, authorInfo, categoryBadge);
    header.expand(authorInfo);

    // Content
    Paragraph headline = new Paragraph(item.getHeadline());
    headline.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.Margin.Vertical.SMALL);

    Paragraph content = new Paragraph(item.getContent());
    content.addClassNames(LumoUtility.FontSize.SMALL);

    // Footer/Interactions (Visual Only)
    HorizontalLayout interactions = new HorizontalLayout();
    interactions.setSpacing(true);
    interactions.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.TextColor.SECONDARY);

    interactions.add(createInteraction(VaadinIcon.COMMENT, "0"));
    interactions.add(createInteraction(VaadinIcon.RETWEET, "0"));
    interactions.add(createInteraction(VaadinIcon.HEART, "0"));

    card.add(header, headline, content, interactions);
    return card;
  }

  private HorizontalLayout createInteraction(VaadinIcon vaadinIcon, String count) {
    HorizontalLayout layout = new HorizontalLayout();
    layout.setSpacing(false);
    layout.setAlignItems(Alignment.CENTER);
    layout.addClassNames(LumoUtility.Gap.XSMALL);

    Icon icon = vaadinIcon.create();
    icon.setSize("14px");

    Span label = new Span(count);
    label.addClassNames(LumoUtility.FontSize.XXSMALL);

    layout.add(icon, label);
    return layout;
  }
}

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
* along with this program.  |
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.ui.view.news;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.service.news.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NewsDocsE2ETest extends AbstractE2ETest {

  @Autowired private NewsService newsService;

  @Test
  void testCaptureNewsFeature() {
    // 1. Setup - Ensure some news exists
    newsService.createNewsItem(
        "Breaking: Major Championship Change!",
        "In a shocking turn of events, the ATW World Championship has changed hands in a surprise match.",
        NewsCategory.BREAKING,
        false,
        5);

    newsService.createNewsItem(
        "Rumor: Backstage Tension Rising?",
        "Our sources indicate that things are getting heated between major factions backstage.",
        NewsCategory.RUMOR,
        true,
        3);

    // 2. Capture News & Rumors View
    driver.get("http://localhost:" + serverPort + getContextPath() + "/news");
    waitForVaadinClientToLoad();
    documentFeature(
        "Dashboards",
        "News & Rumors Feed",
        "A procedural news system that reacts to in-game events across the promotion.",
        "dashboards-news-feed");

    // 3. Capture News Ticker on Booker Dashboard
    driver.get("http://localhost:" + serverPort + getContextPath() + "/booker");
    waitForVaadinClientToLoad();
    documentFeature(
        "Booker",
        "Dashboard with News Ticker",
        "Latest headlines and rumors are visible directly on the booker dashboard.",
        "booker-dashboard-news-ticker");
  }
}

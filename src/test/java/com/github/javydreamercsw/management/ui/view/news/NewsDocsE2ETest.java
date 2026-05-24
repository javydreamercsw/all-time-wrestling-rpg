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

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.service.news.NewsService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class NewsDocsE2ETest extends AbstractE2ETest {

  @Autowired private NewsService newsService;

  @Test
  void testCaptureNewsFeature() {
    // 1. Setup - Ensure some news exists
    newsService.createNewsItem(
        "Breaking: Major Championship Change!",
        """
        In a shocking turn of events, the ATW World Championship has changed hands in a surprise\
         match.\
        """,
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
    navigateTo("news");
    documentFeature(
        "Dashboards",
        "News & Rumors Feed",
        "A procedural news system that reacts to in-game events across the promotion.",
        "dashboards-news-feed");

    // 3. Capture News Ticker on Booker Dashboard
    navigateTo("booker");
    documentFeature(
        "Booker",
        "Dashboard with News Ticker",
        "Latest headlines and rumors are visible directly on the booker dashboard.",
        "booker-dashboard-news-ticker");
  }

  @Test
  void testRecordNewsWalkthrough() {
    setVideoInfo("Dashboards", "News & Rumors Walkthrough", "news-walkthrough");

    newsService.createNewsItem(
        "Breaking: World Title Changes Hands at SuperShow",
        "In a shocking upset the challenger defeated the reigning champion after a gruelling"
            + " 45-minute iron man match, ending a 90-day title reign.",
        NewsCategory.BREAKING,
        false,
        5);

    newsService.createNewsItem(
        "Rumor: Top Star Unhappy — Locker Room Tension Brewing?",
        "Sources close to the locker room suggest friction between two main-event calibre"
            + " wrestlers may lead to a shake-up in the booking card.",
        NewsCategory.RUMOR,
        true,
        3);

    newsService.createNewsItem(
        "Injury Report: Rising Star Sidelined 2 Shows",
        "Medical staff have confirmed a knee sprain suffered during last week's tag match."
            + " The wrestler is expected to return after two shows of rest.",
        NewsCategory.INJURY,
        false,
        4);

    newsService.createNewsItem(
        "Contract News: Three Free Agents Available This Week",
        "With the end-of-season contract window open, three mid-card wrestlers have become"
            + " available for signing. Act fast — rival promotions are circling.",
        NewsCategory.CONTRACT,
        false,
        3);

    navigateTo("news");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    captureCaption(
        "News & Rumors — a rolling feed of every significant event in the promotion."
            + " Headlines are categorised as Breaking News, Rumor Mill, Injury Report,"
            + " Contract News, or Expert Analysis. Importance stars (1–5) rank urgency.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 250)");
    sleep(1000);

    captureCaption(
        "Click any row to expand the full story. Rumours are flagged separately from"
            + " confirmed news — track which stories are verified before acting on them"
            + " in your booking decisions.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(800);

    captureCaption(
        "Admins and bookers can click Generate Monthly Synthesis to have the AI"
            + " summarise the month's events into a single narrative dispatch, or"
            + " Create News to manually write a headline that appears in the feed.",
        4500);

    sleep(1500);
  }
}

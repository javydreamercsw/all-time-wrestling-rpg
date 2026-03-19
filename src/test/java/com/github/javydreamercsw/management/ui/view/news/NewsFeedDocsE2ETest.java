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

import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NewsFeedDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private com.github.javydreamercsw.management.DataInitializer dataInitializer;

  @org.junit.jupiter.api.BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testCaptureNewsAndSocialFeed() {
    // 1. Navigate to News & Rumors (Admin view has synthesis button)
    driver.get("http://localhost:" + serverPort + getContextPath() + "/news");
    waitForVaadinClientToLoad();

    // 2. Capture News Grid & Synthesis Button
    documentFeature(
        "News",
        "News & Rumors",
        "Keep track of everything happening in the wrestling world. Review match results, "
            + "injuries, and contract updates in a structured grid. Authorized users can trigger a "
            + "comprehensive Monthly Synthesis to wrap up major story arcs.",
        "news-grid");

    // 3. Navigate to Social Feed
    driver.get("http://localhost:" + serverPort + getContextPath() + "/news/feed");
    waitForVaadinClientToLoad();

    // 4. Capture Social Feed
    documentFeature(
        "News",
        "Wrestling World Feed",
        "Immerse yourself in the narrative with the Social Feed. See real-time reactions from"
            + " wrestlers and commentators, and download the monthly Newsletter for a deep-dive"
            + " analytical summary of the latest events.",
        "news-social-feed");
  }
}

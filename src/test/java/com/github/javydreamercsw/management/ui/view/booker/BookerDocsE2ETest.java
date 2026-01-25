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
package com.github.javydreamercsw.management.ui.view.booker;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BookerDocsE2ETest extends AbstractE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private ShowService showService;

  @Test
  void testCaptureShowPlanningView() {
    // 1. Setup a show to plan
    Show show = new Show();
    show.setName("Show to Plan");
    show.setShowDate(LocalDate.now().plusDays(7));
    show.setDescription("Planning Documentation Show");
    show.setType(showTypeRepository.findByName("Weekly").get());
    show = showRepository.save(show);

    // 2. Navigate to Show Planning
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-planning");
    waitForVaadinClientToLoad();

    // 3. Select the show
    selectFromVaadinComboBox("select-show-combo-box", "Show to Plan");
    waitForText("Show Planning Context");

    documentFeature(
        "Booker",
        "Show Planning",
        "The Show Planning interface allows you to book matches and segments for an upcoming show."
            + " Drag and drop wrestlers to create matches, set stipulations, and define segment"
            + " types.",
        "booker-show-planning");
  }

  @Test
  void testCaptureShowListView() {
    // Navigate to Show List (History)
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");
    waitForVaadinClientToLoad();
    waitForText("Shows");

    documentFeature(
        "Booker",
        "Show History",
        "View a complete history of all booked shows. Click on any show to view its detailed"
            + " results, ratings, and match history.",
        "booker-show-history");
  }

  @Test
  void testCaptureMatchNarrationView() {
    // 1. Setup a dummy show and segment
    Show show = new Show();
    show.setName("Docs Weekly Show");
    show.setShowDate(LocalDate.now());
    show.setDescription("Documentation Show");
    show.setType(showTypeRepository.findByName("Weekly").get());
    show = showRepository.save(show);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentTypeRepository.findByName("One on One").get());
    segment = segmentRepository.save(segment);

    // 2. Navigate to Match View
    driver.get("http://localhost:" + serverPort + getContextPath() + "/match/" + segment.getId());
    waitForVaadinClientToLoad();

    // 3. Verify & Capture
    waitForText("Match Details");
    waitForText("Match Narration");

    documentFeature(
        "Booker",
        "Match Narration",
        "Bring your matches to life with AI-generated or manual narration. The Story Director uses"
            + " match participants, rules, and outcomes to weave a compelling narrative of the"
            + " action.",
        "booker-match-narration");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

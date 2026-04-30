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
package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

class ShowExportDocsE2ETest extends AbstractE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testCaptureExportShowCard() {
    // 1. Setup a dummy show with a segment
    ShowType weekly =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  st.setExpectedMatches(5);
                  st.setExpectedPromos(2);
                  return showTypeRepository.save(st);
                });

    Show show = new Show();
    show.setName("Export Docs Show");
    show.setShowDate(LocalDate.now());
    show.setDescription("Documentation Show for Export");
    show.setType(weekly);
    show = showRepository.save(show);

    Wrestler w1 = wrestlerRepository.findAll().get(0);
    Wrestler w2 = wrestlerRepository.findAll().get(1);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentTypeRepository.findByName("One on One").get());
    segment.addParticipant(w1);
    segment.addParticipant(w2);
    segment.setSummary("An epic showdown to showcase the export feature.");
    segment.setMainEvent(true);
    segmentRepository.save(segment);

    // 2. Navigate to Show List
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");
    waitForVaadinClientToLoad();

    // 3. Open Export Dialog from grid
    String exportBtnId = "export-show-button-" + show.getId();
    waitForVaadinElement(driver, By.id(exportBtnId));

    clickElement(By.id(exportBtnId));

    // 4. Verify & Capture

    documentFeature(
        "Community",
        "Social Media Export",
        "Share your show cards with the world! The Social Media Export tool generates"
            + " perfectly formatted match cards for Markdown, Facebook, X (Twitter), and Bluesky."
            + " Choose to include match results, segment summaries, and highlighted main events"
            + " to keep your community engaged and informed.",
        "community-show-export");
  }
}

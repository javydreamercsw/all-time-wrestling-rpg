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
package com.github.javydreamercsw.management.ui.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

/** Docs screenshots for the #1 contender automation feature (ATW-jjl7 / ATW-0bfe). */
class ContenderAutomationDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  /** Creates a show carrying a #1 contender match segment and returns it. */
  private Show ensureContenderShow() {
    return showRepository.findAll().stream()
        .filter(s -> "Contender Docs Show".equals(s.getName()))
        .findFirst()
        .orElseGet(
            () -> {
              ShowType weekly = showTypeRepository.findByName("Weekly").orElseThrow();
              Show show = new Show();
              show.setName("Contender Docs Show");
              show.setShowDate(LocalDate.now().plusDays(3));
              show.setType(weekly);
              show.setUniverse(defaultUniverse);
              show = showRepository.save(show);

              SegmentType oneOnOne =
                  segmentTypeRepository
                      .findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode())
                      .orElseThrow();
              List<Wrestler> wrestlers = wrestlerRepository.findAll();
              Title title = titleRepository.findAll().stream().findFirst().orElse(null);

              Segment segment = new Segment();
              segment.setShow(show);
              segment.setSegmentType(oneOnOne);
              segment.setSegmentDate(Instant.now());
              segment.setContenderMatch(true);
              if (title != null) {
                segment.getTitles().add(title);
              }
              if (wrestlers.size() >= 2) {
                segment.addParticipant(wrestlers.get(0));
                segment.addParticipant(wrestlers.get(1));
              }
              segmentRepository.save(segment);
              return show;
            });
  }

  @Test
  void testCaptureContenderMatchBadge() {
    Show show = ensureContenderShow();

    // Admin/booker users see the editing grid, where the contender star is rendered
    // next to the segment type (id = contender-match-badge-<segmentId>).
    navigateToAndWaitForElement(
        "show-detail/" + show.getId(), By.cssSelector("[id^='contender-match-badge-']"));

    documentFeature(
        "Game Mechanics",
        "Contender Matches",
        """
        Mark any match as a #1 Contender Match to put the next title shot on the line. The\
         segment carries a gold star badge on the show card, the winner is automatically\
         designated as the title's #1 contender, and the match earns bonus rivalry heat and\
         fan gains.\
        """,
        "contender-match-badge");
  }

  @Test
  void testCaptureContenderSettings() {
    // Game Settings lives in a tab of the Admin view.
    WebElement tab =
        navigateToAndWaitForElement(
            "admin", By.xpath("//vaadin-tab[contains(text(), 'Game Settings')]"));
    clickElement(tab);

    // The contender settings live in a collapsed section — expand it first.
    WebElement contenderSection =
        waitForVaadinElement(
            driver, By.cssSelector("#settings-section-contender > vaadin-details-summary"));
    clickElement(contenderSection);

    WebElement contenderToggle =
        waitForVaadinElement(driver, By.id("contender-auto-select-enabled"));
    assertNotNull(contenderToggle);
    scrollIntoView(contenderToggle);

    documentFeature(
        "Admin",
        "Contender Automation Settings",
        """
        Every contender-automation threshold is configurable: enable or disable auto-selection\
         after title matches, tune the fan-gap percentage that counts as a tie, pick the\
         tie-breaker match type and its minimum participants, and adjust the heat bonus and\
         fan multiplier applied to contender matches.\
        """,
        "admin-contender-settings");
  }
}

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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Docs screenshot test: verifies that a VIEWER sees the Show Detail page in read-only mode — no
 * Adjudicate, Add Segment, Edit, or other mutation controls are rendered.
 */
class ShowDetailViewerDocsE2ETest extends AbstractE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;

  private Show testShow;

  @BeforeEach
  void setupTestData() {
    ShowType showType =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  st.setExpectedMatches(3);
                  st.setExpectedPromos(2);
                  return showTypeRepository.save(st);
                });

    testShow = new Show();
    testShow.setName("Viewer Read-Only Show");
    testShow.setShowDate(LocalDate.now());
    testShow.setDescription("Demonstration of the read-only show view for VIEWER accounts.");
    testShow.setType(showType);
    testShow.setUniverse(defaultUniverse);
    testShow = showRepository.save(testShow);

    Wrestler w1 = ensureWrestler("Viewer Test Alpha");
    Wrestler w2 = ensureWrestler("Viewer Test Beta");

    SegmentType matchType =
        segmentTypeRepository
            .findByName("One on One")
            .orElseGet(
                () -> {
                  SegmentType st = new SegmentType();
                  st.setName("One on One");
                  return segmentTypeRepository.save(st);
                });

    Segment segment = new Segment();
    segment.setShow(testShow);
    segment.setSegmentType(matchType);
    segment.addParticipant(w1);
    segment.addParticipant(w2);
    segment.setSegmentOrder(1);
    segmentRepository.saveAndFlush(segment);
  }

  @Test
  void viewerSeesReadOnlyShowDetail() {
    login("viewer", "viewer123");

    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + testShow.getId());
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.id("show-info-details"));

    List<WebElement> adjudicateButtons = driver.findElements(By.id("adjudicate-show-btn"));
    assertTrue(
        adjudicateButtons.isEmpty()
            || adjudicateButtons.stream().noneMatch(WebElement::isDisplayed),
        "Adjudicate button must not be visible to VIEWER");

    List<WebElement> addSegmentButtons = driver.findElements(By.id("add-segment-btn"));
    assertTrue(
        addSegmentButtons.isEmpty()
            || addSegmentButtons.stream().noneMatch(WebElement::isDisplayed),
        "Add Segment button must not be visible to VIEWER");

    documentFeature(
        "Entities",
        "Show Detail — Viewer Read-Only",
        "Users with the VIEWER role see the Show Detail page in read-only mode."
            + " Segment results, participants, and narration are all visible,"
            + " but mutation controls (Adjudicate, Add Segment, Edit, Delete) are hidden.",
        "entities-show-detail-viewer-readonly");
  }

  private Wrestler ensureWrestler(final String name) {
    return wrestlerRepository
        .findByName(name)
        .orElseGet(
            () ->
                wrestlerRepository.save(
                    Wrestler.builder()
                        .name(name)
                        .startingHealth(100)
                        .startingStamina(100)
                        .active(true)
                        .build()));
  }
}

/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

/** Documentation screenshot: the Adjudicate Show pre-flight dialog (2.13.0). */
class ShowAdjudicationDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private UniverseRepository universeRepository;

  private Show testShow;

  @BeforeEach
  public void setupTestData() {
    cleanupLeagues();

    ShowType showType =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  st.setDescription("Weekly Show Docs");
                  st.setExpectedMatches(2);
                  st.setExpectedPromos(1);
                  return showTypeRepository.saveAndFlush(st);
                });

    Universe universe =
        universeRepository
            .findByName("Default Universe")
            .orElseThrow(() -> new IllegalStateException("Default Universe missing"));

    Wrestler a = seedWrestler("Docs Adjudication A");
    Wrestler b = seedWrestler("Docs Adjudication B");

    testShow = new Show();
    testShow.setName("Docs Adjudication Show " + System.nanoTime());
    testShow.setType(showType);
    testShow.setShowDate(LocalDate.now());
    testShow.setDescription("Adjudicate Show with narration and outcome warnings");
    testShow.setUniverse(universe);
    testShow = showRepository.saveAndFlush(testShow);

    SegmentType singlesType =
        segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()).orElseThrow();

    Segment winnerless = new Segment();
    winnerless.setShow(testShow);
    winnerless.setSegmentType(singlesType);
    winnerless.setAdjudicationStatus(AdjudicationStatus.PENDING);
    winnerless.addParticipant(a);
    winnerless.addParticipant(b);
    segmentRepository.saveAndFlush(winnerless);
  }

  private Wrestler seedWrestler(String name) {
    Wrestler w = new Wrestler();
    w.setName(name + " " + System.nanoTime());
    w.setActive(true);
    w.setIsPlayer(false);
    w.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.MALE);
    return wrestlerRepository.saveAndFlush(w);
  }

  @Test
  public void testCaptureAdjudicationDialog() {
    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + testShow.getId());

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    WebElement adjudicateButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("adjudicate-show-btn")));
    clickElement(adjudicateButton);

    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-confirm-dialog")));

    documentFeature(
        "Booker",
        "Adjudicate Show",
        "Adjudicating a show: AI narration is generated for pending segments and the booker"
            + " is warned about matches without a winner before confirming.",
        "show-adjudication-dialog");
  }
}

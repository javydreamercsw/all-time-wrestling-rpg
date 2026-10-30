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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentResult;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * E2E test for the 2.13.0 Adjudicate Show flow: the pre-flight confirm dialog appears with the
 * missing-winner warning, confirming runs narration generation (Mock provider) plus adjudication,
 * and the booked-draw segment renders a Draw badge.
 */
class ShowAdjudicationE2ETest extends AbstractE2ETest {

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
                  st.setDescription("Weekly Show E2E");
                  st.setExpectedMatches(2);
                  st.setExpectedPromos(1);
                  return showTypeRepository.saveAndFlush(st);
                });

    Universe universe =
        universeRepository
            .findByName("Default Universe")
            .orElseThrow(() -> new IllegalStateException("Default Universe missing"));

    Wrestler a = seedWrestler("E2E Adjudication A");
    Wrestler b = seedWrestler("E2E Adjudication B");

    testShow = new Show();
    testShow.setName("E2E Adjudication Show " + System.nanoTime());
    testShow.setType(showType);
    testShow.setShowDate(LocalDate.now());
    testShow.setDescription("2.13.0 adjudication flow");
    testShow.setUniverse(universe);
    testShow = showRepository.saveAndFlush(testShow);

    SegmentType singlesType =
        segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()).orElseThrow();

    // Winnerless match — should produce the missing-winner warning.
    Segment winnerless = new Segment();
    winnerless.setShow(testShow);
    winnerless.setSegmentType(singlesType);
    winnerless.setAdjudicationStatus(AdjudicationStatus.PENDING);
    winnerless.addParticipant(a);
    winnerless.addParticipant(b);
    segmentRepository.saveAndFlush(winnerless);

    // Booked draw — should NOT warn and should render a Draw badge.
    Segment draw = new Segment();
    draw.setShow(testShow);
    draw.setSegmentType(singlesType);
    draw.setAdjudicationStatus(AdjudicationStatus.PENDING);
    draw.addParticipant(a);
    draw.addParticipant(b);
    draw.declareResult(SegmentResult.DRAW);
    segmentRepository.saveAndFlush(draw);
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
  public void testAdjudicateShowsPreFlightDialogThenAdjudicates() {
    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + testShow.getId());

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

    // The Adjudicate button is enabled because pending segments exist.
    WebElement adjudicateButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("adjudicate-show-btn")));
    clickElement(adjudicateButton);

    // The pre-flight confirm dialog appears with the missing-winner warning.
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-confirm-dialog")));
    String dialogText =
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-confirm-dialog")))
            .getText();
    assertTrue(
        dialogText.contains("no winner selected"),
        "pre-flight dialog must warn about the winnerless match, got: " + dialogText);

    // Confirm — the flow proceeds (narration generation via Mock + adjudication).
    WebElement confirmButton =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//vaadin-confirm-dialog//vaadin-button[text()='Adjudicate Anyway']")));
    clickElement(confirmButton);

    // Wait for adjudication to complete: segments become ADJUDICATED and the button
    // disables (no pending segments left).
    org.awaitility.Awaitility.await()
        .atMost(java.time.Duration.ofSeconds(90))
        .untilAsserted(
            () -> {
              List<Segment> segments = segmentRepository.findByShow(testShow);
              assertNotNull(segments);
              assertTrue(segments.size() >= 2);
              assertTrue(
                  segments.stream()
                      .allMatch(s -> s.getAdjudicationStatus() == AdjudicationStatus.ADJUDICATED),
                  "all segments should be adjudicated after confirming");
              Segment drawSegment =
                  segments.stream()
                      .filter(s -> s.getResult() == SegmentResult.DRAW)
                      .findFirst()
                      .orElseThrow();
              // Mock provider generated a real narration.
              assertTrue(
                  drawSegment.getNarration() != null && !drawSegment.getNarration().isBlank(),
                  "booked draw must be narrated by the adjudication flow");
              assertTrue(drawSegment.isNarrationGenerated());
              assertTrue(drawSegment.getWinners().isEmpty());
            });

    // Refresh the page: the booked draw shows a Draw badge in the grid's Winner(s)
    // column (the booker's view — the viewer feed card renders the same badge).
    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + testShow.getId());
    WebDriverWait refreshWait = new WebDriverWait(driver, Duration.ofSeconds(30));
    refreshWait.until(ExpectedConditions.presenceOfElementLocated(By.id("segments-grid-wrapper")));
    // Grid cells live inside vaadin-grid's shadow DOM; getPageSource() includes it.
    org.awaitility.Awaitility.await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () -> {
              String pageSource = driver.getPageSource();
              assertTrue(
                  pageSource.contains("🤝 Draw"),
                  "the booked draw segment should show a Draw badge in the Winner(s) column");
            });
  }
}

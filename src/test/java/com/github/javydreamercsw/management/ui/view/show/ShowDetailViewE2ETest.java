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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.time.Duration;
import java.time.LocalDate;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ShowDetailViewE2ETest extends AbstractE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;

  private Show testShow;

  @BeforeEach
  public void setupTestData() {
    ShowType showType = new ShowType();
    showType.setName("Weekly Show");
    showType.setDescription("A weekly show");
    showTypeRepository.save(showType);

    testShow = new Show();
    testShow.setName("Test Show for Detail View");
    testShow.setType(showType);
    testShow.setShowDate(LocalDate.now());
    testShow.setDescription("Test Description");
    showRepository.save(testShow);

    wrestlerRepository.save(createTestWrestler("Wrestler 1"));
    wrestlerRepository.save(createTestWrestler("Wrestler 2"));

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Singles Match");
    segmentTypeRepository.save(segmentType);
  }

  @Test
  public void testAddSegmentWithNarration() {
    // Navigate to the Show Detail view
    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + testShow.getId());

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Click the "Add Segment" button
    WebElement addSegmentButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[text()='Add Segment']")));
    Assertions.assertNotNull(addSegmentButton);
    clickElement(addSegmentButton);

    // Wait for the dialog to open
    WebElement dialog =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Fill in the form
    Assertions.assertNotNull(dialog);
    String narrationText = "This is a test narration.";
    String summaryText = "This is a test summary.";

    WebElement summaryArea = dialog.findElement(By.id("summary-text-area"));
    summaryArea.sendKeys(summaryText, Keys.TAB);
    wait.until(ExpectedConditions.textToBePresentInElementValue(summaryArea, summaryText));

    WebElement narrationArea = dialog.findElement(By.id("narration-text-area"));
    narrationArea.sendKeys(narrationText, Keys.TAB);
    wait.until(ExpectedConditions.textToBePresentInElementValue(narrationArea, narrationText));

    WebElement segmentTypeComboBox = dialog.findElement(By.id("segment-type-combo-box"));
    segmentTypeComboBox.sendKeys("Singles Match", Keys.TAB);

    WebElement wrestlersComboBox = dialog.findElement(By.id("wrestlers-combo-box"));
    selectFromVaadinMultiSelectComboBox(wrestlersComboBox, "Wrestler 1");
    selectFromVaadinMultiSelectComboBox(wrestlersComboBox, "Wrestler 2");

    // Click the "Add Segment" button in the dialog
    WebElement addSegmentDialogButton = dialog.findElement(By.id("add-segment-save-button"));
    clickElement(addSegmentDialogButton);

    // Wait for the grid to update and check for the new segment's narration and summary
    Failsafe.with(
            RetryPolicy.builder()
                .withDelay(Duration.ofMillis(500))
                .withMaxDuration(Duration.ofSeconds(10))
                .withMaxAttempts(3)
                .handle(AssertionFailedError.class, NoSuchElementException.class)
                .onRetry(
                    e -> // Navigate to the Show Detail view
                    driver.get(
                            "http://localhost:"
                                + serverPort
                                + getContextPath()
                                + "/show-detail/"
                                + testShow.getId()))
                .build())
        .get(
            () -> {
              wait.until(
                  ExpectedConditions.presenceOfElementLocated(By.id("segments-grid-wrapper")));
              WebElement refreshedGrid =
                  wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("segments-grid")));
              assertTrue(refreshedGrid.getText().contains(narrationText));
              assertTrue(refreshedGrid.getText().contains(summaryText));
              return refreshedGrid;
            });
  }
}

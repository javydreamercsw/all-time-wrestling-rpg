package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.time.Duration;
import java.time.LocalDate;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ShowDetailViewE2ETest extends AbstractE2ETest {

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

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Wrestler 1");
    wrestler1.setDeckSize(10);
    wrestler1.setLowHealth(0);
    wrestler1.setStartingHealth(100);
    wrestler1.setCurrentHealth(100);
    wrestler1.setStartingStamina(100);
    wrestler1.setLowStamina(0);
    wrestlerRepository.save(wrestler1);

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setName("Wrestler 2");
    wrestler2.setDeckSize(10);
    wrestler2.setLowHealth(0);
    wrestler2.setStartingHealth(100);
    wrestler2.setCurrentHealth(100);
    wrestler2.setStartingStamina(100);
    wrestler2.setLowStamina(0);
    wrestlerRepository.save(wrestler2);

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
    clickAndScrollIntoView(addSegmentButton);

    // Wait for the dialog to open
    WebElement dialog =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Fill in the form
    WebElement segmentTypeComboBox = dialog.findElement(By.id("segment-type-combo-box"));
    segmentTypeComboBox.sendKeys("Singles Match", Keys.RETURN);

    WebElement wrestlersComboBox = dialog.findElement(By.id("wrestlers-combo-box"));
    clickAndScrollIntoView(wrestlersComboBox);

    wrestlersComboBox.sendKeys("Wrestler 1", Keys.RETURN, "Wrestler 2", Keys.RETURN);

    String narrationText = "This is a test narration.";
    String summaryText = "This is a test summary.";

    WebElement summaryArea = dialog.findElement(By.id("summary-text-area"));
    WebElement narrationArea = dialog.findElement(By.id("narration-text-area"));

    summaryArea.sendKeys(summaryText);
    narrationArea.sendKeys(narrationText);

    wait.until(ExpectedConditions.textToBePresentInElementValue(summaryArea, summaryText));

    narrationArea.sendKeys(narrationText);
    wait.until(ExpectedConditions.textToBePresentInElementValue(narrationArea, narrationText));

    // Click the "Add Segment" button in the dialog
    WebElement addSegmentDialogButton = dialog.findElement(By.id("add-segment-save-button"));
    clickAndScrollIntoView(addSegmentDialogButton);

    // Wait for the dialog to go away
    wait.until(ExpectedConditions.invisibilityOfAllElements(dialog));

    // Wait for the grid to update and check for the new segment's narration and summary
    RetryPolicy<Object> retryPolicy =
        RetryPolicy.builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(10))
            .handle(AssertionFailedError.class)
            .build();
    Failsafe.with(retryPolicy)
        .get(
            () -> {
              WebElement segmentGrid = driver.findElement(By.id("segments-grid-wrapper"));
              wait.until(ExpectedConditions.visibilityOfAllElements(segmentGrid));
              WebElement refreshedGrid = segmentGrid.findElement(By.id("segments-grid"));
              assertTrue(refreshedGrid.getText().contains(narrationText));
              assertTrue(refreshedGrid.getText().contains(summaryText));
              return refreshedGrid;
            });
  }
}

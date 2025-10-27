package com.github.javydreamercsw.management.ui.view.wrestler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WrestlerRankingsViewE2ETest extends AbstractE2ETest {

  @Test
  public void testChampionIcon() {
    driver.get("http://localhost:8080/wrestler-rankings");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for the grid to be present
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    assertNotNull(grid);

    // This test assumes there is at least one champion.
    // The test data should be set up accordingly.
    WebElement trophyIcon = grid.findElement(By.tagName("vaadin-icon"));
    assertNotNull(trophyIcon);
    assertTrue(trophyIcon.getAttribute("icon").contains("trophy"));
  }
}

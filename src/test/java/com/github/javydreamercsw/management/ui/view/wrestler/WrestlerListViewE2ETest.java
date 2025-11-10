package com.github.javydreamercsw.management.ui.view.wrestler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.AbstractE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WrestlerListViewE2ETest extends AbstractE2ETest {

  @Test
  public void testNavigateToWrestlerListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");

    // Check that the grid is present
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    assertNotNull(driver.findElement(By.tagName("vaadin-grid")));
  }
}

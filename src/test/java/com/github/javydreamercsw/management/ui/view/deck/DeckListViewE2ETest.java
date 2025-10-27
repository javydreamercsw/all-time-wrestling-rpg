package com.github.javydreamercsw.management.ui.view.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.AbstractE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DeckListViewE2ETest extends AbstractE2ETest {

  @Test
  public void testNavigateToDeckListView() {
    driver.get("http://localhost:8080/deck-list");

    // Check that the grid is present
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    assertNotNull(driver.findElement(By.tagName("vaadin-grid")));
  }

  @Test
  public void testGridSize() {
    driver.get("http://localhost:8080/deck-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Check that the grid is present
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    assertNotNull(grid);

    // Check that the grid and its container are full size
    assertEquals("100%", grid.getCssValue("height"));

    WebElement parent = grid.findElement(By.xpath(".."));
    assertEquals("100%", parent.getCssValue("height"));
  }
}

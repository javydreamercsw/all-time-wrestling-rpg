package com.github.javydreamercsw.management.ui.view.rivalry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.AbstractE2ETest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class RivalryListViewE2ETest extends AbstractE2ETest {

  @Test
  public void testNavigateToRivalryListView() {
    driver.get("http://localhost:" + serverPort + "/rivalry-list");

    // Check that the grid is present
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    assertNotNull(driver.findElement(By.tagName("vaadin-grid")));
  }

  @Test
  public void testSortByHeat() {
    driver.get("http://localhost:" + serverPort + "/rivalry-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Wait for the grid to be present and populated
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    wait.until(d -> !grid.findElements(By.tagName("vaadin-grid-cell-content")).isEmpty());

    // Get the header cell for the "Heat" column
    WebElement heatHeader = grid.findElement(By.xpath("//vaadin-grid-sorter[contains(., 'Heat')]"));

    // Get the initial order of heat values
    List<Integer> initialOrder =
        getColumnData(grid, 2).stream().map(Integer::parseInt).collect(Collectors.toList());

    // Click to sort ascending
    heatHeader.click();
    wait.until(ExpectedConditions.attributeContains(heatHeader, "direction", "asc"));
    List<Integer> ascOrder =
        getColumnData(grid, 2).stream().map(Integer::parseInt).collect(Collectors.toList());
    List<Integer> sortedInitial = new ArrayList<>(initialOrder);
    Collections.sort(sortedInitial);
    assertEquals(sortedInitial, ascOrder);

    // Click to sort descending
    heatHeader.click();
    wait.until(ExpectedConditions.attributeContains(heatHeader, "direction", "desc"));
    List<Integer> descOrder =
        getColumnData(grid, 2).stream().map(Integer::parseInt).collect(Collectors.toList());
    Collections.reverse(sortedInitial);
    assertEquals(sortedInitial, descOrder);
  }

  @Test
  public void testNewColumns() {
    driver.get("http://localhost:" + serverPort + "/rivalry-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Wait for the grid to be present and populated
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    wait.until(d -> !grid.findElements(By.tagName("vaadin-grid-cell-content")).isEmpty());

    // Check for the new columns
    assertNotNull(grid.findElement(By.xpath("//vaadin-grid-sorter[contains(., 'Notes')]")));
    assertNotNull(grid.findElement(By.xpath("//vaadin-grid-sorter[contains(., 'Start Date')]")));
    assertNotNull(grid.findElement(By.xpath("//vaadin-grid-sorter[contains(., 'End Date')]")));
  }

  private List<String> getColumnData(WebElement grid, int columnIndex) {
    List<WebElement> cells =
        grid.findElements(
            By.xpath(
                "//vaadin-grid-cell-content[count(ancestor::vaadin-grid-column) = "
                    + (columnIndex + 1)
                    + "]"));
    return cells.stream().map(WebElement::getText).collect(Collectors.toList());
  }
}

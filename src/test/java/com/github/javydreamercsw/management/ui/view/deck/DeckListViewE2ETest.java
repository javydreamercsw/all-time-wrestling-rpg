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
package com.github.javydreamercsw.management.ui.view.deck;

import static org.assertj.core.api.Assertions.assertThat;
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
    driver.get("http://localhost:" + serverPort + getContextPath() + "/deck-list");

    // Check that the grid is present
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    assertNotNull(driver.findElement(By.tagName("vaadin-grid")));
  }

  @Test
  public void testGridSize() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/deck-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Check that the grid is present
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    assertNotNull(grid);

    // Check that the grid and its container are full size
    assertThat(grid.getSize().getHeight()).isGreaterThan(0);

    WebElement parent = grid.findElement(By.xpath(".."));
    assertThat(parent.getSize().getHeight()).isGreaterThan(0);
  }
}

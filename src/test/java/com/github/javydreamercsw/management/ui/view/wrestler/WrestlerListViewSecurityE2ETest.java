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
package com.github.javydreamercsw.management.ui.view.wrestler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WrestlerListViewSecurityE2ETest extends AbstractE2ETest {

  private Wrestler playerWrestler;
  private Wrestler otherWrestler;

  @BeforeEach
  void setUp() {
    // Clean up database
    databaseCleaner.clearRepositories();

    Account player = createTestAccount("player-test", "ValidPassword1!", RoleName.PLAYER);
    createTestAccount("viewer-test", "ValidPassword1!", RoleName.VIEWER);
    createTestAccount("booker-test", "ValidPassword1!", RoleName.BOOKER);

    // Create wrestlers
    playerWrestler = TestUtils.createWrestler("Player's Wrestler");
    playerWrestler.setAccount(player);
    playerWrestler.setIsPlayer(true);
    wrestlerRepository.save(playerWrestler);

    otherWrestler = TestUtils.createWrestler("Other Wrestler");
    wrestlerRepository.save(otherWrestler);
  }

  @Test
  void testAsViewer() {
    logout();
    login("viewer-test", "ValidPassword1!");
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));

    // Assert that the "Create Wrestler" button is not present
    List<WebElement> createButtons = driver.findElements(By.id("create-wrestler-button"));
    assertTrue(createButtons.isEmpty());

    // Assert that the action menu buttons are not present
    List<WebElement> menuButtons =
        driver.findElements(By.id("action-menu-" + playerWrestler.getId()));
    assertTrue(menuButtons.isEmpty());
  }

  @Test
  void testAsPlayer() {
    logout();
    login("player-test", "ValidPassword1!");
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));

    // Assert that the "Create Wrestler" button is not present
    List<WebElement> createButtons = driver.findElements(By.id("create-wrestler-button"));
    assertTrue(createButtons.isEmpty());

    // Check own wrestler's menu
    WebElement ownMenu =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-menu-bar[@id='action-menu-"
                        + playerWrestler.getId()
                        + "']/vaadin-menu-bar-button")));
    Assertions.assertNotNull(ownMenu);
    clickElement(ownMenu);
    assertTrue(driver.findElement(By.id("edit-" + playerWrestler.getId())).isDisplayed());
    assertTrue(driver.findElements(By.id("delete-" + playerWrestler.getId())).isEmpty());

    // Close the menu
    clickElement(ownMenu);

    // Check other wrestler's menu (should not have edit/delete)
    WebElement otherMenu =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-menu-bar[@id='action-menu-"
                        + otherWrestler.getId()
                        + "']/vaadin-menu-bar-button")));
    Assertions.assertNotNull(otherMenu);
    clickElement(otherMenu);
    assertTrue(driver.findElements(By.id("edit-" + otherWrestler.getId())).isEmpty());
  }

  @Test
  void testAsBooker() {
    logout();
    login("booker-test", "ValidPassword1!");
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));

    // Assert that the "Create Wrestler" button is present
    assertTrue(driver.findElement(By.id("create-wrestler-button")).isDisplayed());

    // Check action menu
    WebElement menu =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-menu-bar[@id='action-menu-"
                        + playerWrestler.getId()
                        + "']/vaadin-menu-bar-button")));
    Assertions.assertNotNull(menu);
    clickElement(menu);
    assertTrue(driver.findElement(By.id("edit-" + playerWrestler.getId())).isDisplayed());
    assertTrue(driver.findElement(By.id("delete-" + playerWrestler.getId())).isDisplayed());
  }

  @Test
  void testAsAdmin() {
    // Default user is admin
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));

    // Assert that the "Create Wrestler" button is present
    assertTrue(driver.findElement(By.id("create-wrestler-button")).isDisplayed());

    // Check action menu
    WebElement menu =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-menu-bar[@id='action-menu-"
                        + playerWrestler.getId()
                        + "']/vaadin-menu-bar-button")));
    Assertions.assertNotNull(menu);
    clickElement(menu);
    assertTrue(driver.findElement(By.id("edit-" + playerWrestler.getId())).isDisplayed());
    assertTrue(driver.findElement(By.id("delete-" + playerWrestler.getId())).isDisplayed());
  }
}

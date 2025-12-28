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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.AccountService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WrestlerListViewSecurityE2ETest extends AbstractE2ETest {

  @Autowired
  @Qualifier("managementAccountService") private AccountService accountService;

  private Wrestler playerWrestler;
  private Wrestler otherWrestler;

  @BeforeEach
  void setUp() {
    // Clean up database
    segmentRepository.deleteAll();
    wrestlerRepository.deleteAll();
    accountRepository.deleteAll();
    roleRepository.deleteAll();

    // Re-create roles
    roleRepository.save(new Role(RoleName.ADMIN, "Admin role"));
    roleRepository.save(new Role(RoleName.BOOKER, "Booker role"));
    roleRepository.save(new Role(RoleName.PLAYER, "Player role"));
    roleRepository.save(new Role(RoleName.VIEWER, "Viewer role"));

    // Re-create accounts
    accountService.createAccount("admin", "Admin123!", "admin@atwrpg.local", RoleName.ADMIN);
    accountService.createAccount("booker", "Booker123!", "booker@atwrpg.local", RoleName.BOOKER);
    Account playerAccount =
        accountService.createAccount(
            "player", "Player123!", "player@atwrpg.local", RoleName.PLAYER);
    accountService.createAccount("viewer", "Viewer123!", "viewer@atwrpg.local", RoleName.VIEWER);

    // Create wrestlers
    playerWrestler = TestUtils.createWrestler("Player's Wrestler");
    playerWrestler.setAccount(playerAccount);
    playerWrestler.setIsPlayer(true);
    wrestlerRepository.save(playerWrestler);

    otherWrestler = TestUtils.createWrestler("Other Wrestler");
    wrestlerRepository.save(otherWrestler);
  }

  @Test
  void testAsViewer() {
    logout();
    login("viewer", "Viewer123!");
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
    login("player", "Player123!");
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
    clickElement(ownMenu);
    assertTrue(driver.findElement(By.id("edit-" + playerWrestler.getId())).isDisplayed());
    assertFalse(driver.findElements(By.id("delete-" + playerWrestler.getId())).size() > 0);

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
    clickElement(otherMenu);
    assertFalse(driver.findElements(By.id("edit-" + otherWrestler.getId())).size() > 0);
  }

  @Test
  void testAsBooker() {
    logout();
    login("booker", "Booker123!");
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
    clickElement(menu);
    assertTrue(driver.findElement(By.id("edit-" + playerWrestler.getId())).isDisplayed());
    assertTrue(driver.findElement(By.id("delete-" + playerWrestler.getId())).isDisplayed());
  }

  private void logout() {
    driver.get("http://localhost:" + serverPort + getContextPath());
    waitForVaadinElement(driver, By.id("logout-button"));
    WebElement logoutButton = driver.findElement(By.id("logout-button"));
    clickElement(logoutButton);
  }
}

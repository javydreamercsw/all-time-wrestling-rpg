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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WrestlerListViewE2ETest extends AbstractE2ETest {

  @Autowired private InjuryService injuryService;

  @BeforeEach
  void setUp() {
    segmentRepository.deleteAll();
    wrestlerRepository.deleteAll();
    // Create some wrestlers for the tests
    for (int i = 0; i < 4; i++) {
      wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler " + i));
    }
  }

  @Test
  void testCreateWrestler() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Get the initial size of the grid
    long initialSize = wrestlerRepository.count();

    // Click the "Create Wrestler" button
    WebElement createButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-wrestler-button")));
    clickAndScrollIntoView(createButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Find the components
    WebElement nameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-dialog-name-field")));

    // Enter a new wrestler name
    nameField.sendKeys("Test Wrestler", Keys.TAB);

    // Click the save button
    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("wrestler-dialog-save-button")));
    clickAndScrollIntoView(saveButton);

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Verify that the new wrestler appears in the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Test Wrestler"));
          } catch (Exception e) {
            return false;
          }
        });

    assertEquals(initialSize + 1, wrestlerRepository.count());
  }

  @Test
  void testEditWrestler() {
    // Create a wrestler to edit
    Wrestler wrestler = wrestlerRepository.save(createTestWrestler("Wrestler to Edit"));
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    // Find the menu for the wrestler and click it
    WebElement menu =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-menu-bar[@id='action-menu-"
                        + wrestler.getId()
                        + "']/vaadin-menu-bar-button")));
    clickAndScrollIntoView(menu);

    // Find the "Edit" button for the wrestler and click it
    WebElement editButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("edit-" + wrestler.getId())));

    clickAndScrollIntoView(editButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Find the editor's name field and change the value
    WebElement nameEditor =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-dialog-name-field")));

    nameEditor.sendKeys(" Updated", Keys.TAB);

    // Find the "Save" button and click it
    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("wrestler-dialog-save-button")));
    clickAndScrollIntoView(saveButton);

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Verify that the grid is updated
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Wrestler to Edit Updated"));
          } catch (Exception e) {
            return false;
          }
        });

    assertTrue(
        wrestlerRepository.findAll().stream()
            .anyMatch(w -> w.getName().equals("Wrestler to Edit Updated")));
  }

  @Test
  void testDeleteWrestler() {
    // Create a wrestler to delete
    Wrestler wrestler = wrestlerRepository.save(createTestWrestler("Wrestler to Delete"));
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    long initialSize = wrestlerRepository.count();

    // Find the menu for the wrestler and click it
    WebElement menu =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-menu-bar[@id='action-menu-"
                        + wrestler.getId()
                        + "']/vaadin-menu-bar-button")));
    clickAndScrollIntoView(menu);

    // Find the "Delete" button for the wrestler and click it
    WebElement deleteButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("delete-" + wrestler.getId())));

    clickAndScrollIntoView(deleteButton);

    // Verify that the wrestler is removed from the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .noneMatch(it -> it.getText().equals("Wrestler to Delete"));
          } catch (Exception e) {
            return false;
          }
        });
    assertEquals(initialSize - 1, wrestlerRepository.count());
  }

  @Test
  void testAddBump() {
    // Create a wrestler
    Wrestler wrestler =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler for Bump"));
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler and click it
    WebElement menu =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-menu-bar[@id='action-menu-"
                        + wrestler.getId()
                        + "']/vaadin-menu-bar-button")));
    clickAndScrollIntoView(menu);

    // Find the "Add Bump" button for the wrestler and click it
    WebElement addBumpButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("add-bump-" + wrestler.getId())));

    clickAndScrollIntoView(addBumpButton);

    // Verify that the bump count is updated
    wait.until(
        d -> {
          try {
            return wrestlerRepository.findById(wrestler.getId()).orElseThrow().getBumps() == 1;
          } catch (Exception e) {
            return false;
          }
        });
    assertEquals(1, wrestlerRepository.findById(wrestler.getId()).orElseThrow().getBumps());
  }

  @Test
  void testHealBump() {
    // Create a wrestler with a bump
    Wrestler wrestler =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler to Heal Bump"));
    wrestler.addBump();
    wrestlerRepository.save(wrestler);
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler and click it
    WebElement menu =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-menu-bar[@id='action-menu-"
                        + wrestler.getId()
                        + "']/vaadin-menu-bar-button")));
    clickAndScrollIntoView(menu);

    // Find the "Heal Bump" button for the wrestler and click it
    WebElement healBumpButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("heal-bump-" + wrestler.getId())));

    clickAndScrollIntoView(healBumpButton);

    // Verify that the bump count is updated
    wait.until(
        d -> {
          try {
            return wrestlerRepository.findById(wrestler.getId()).orElseThrow().getBumps() == 0;
          } catch (Exception e) {
            return false;
          }
        });
    assertEquals(0, wrestlerRepository.findById(wrestler.getId()).orElseThrow().getBumps());
  }

  @Test
  void testManageInjuries() {
    // Create a wrestler
    Wrestler wrestler =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler for Injuries"));
    // Create a couple of injuries for the wrestler
    injuryService.createInjury(
        wrestler.getId(),
        "Bruised Ribs",
        "Slightly bruised ribs.",
        InjurySeverity.MINOR,
        "Fell off the top rope.");
    Injury injuryToHeal =
        injuryService
            .createInjury(
                wrestler.getId(),
                "Twisted Ankle",
                "Twisted his ankle.",
                InjurySeverity.MODERATE,
                "Landed awkwardly.")
            .get();

    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler and click it
    WebElement menu =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath(
                    "//vaadin-menu-bar[@id='action-menu-"
                        + wrestler.getId()
                        + "']/vaadin-menu-bar-button")));
    clickAndScrollIntoView(menu);

    // Find the "Manage Injuries" button for the wrestler and click it
    WebElement manageInjuriesButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("manage-injuries-" + wrestler.getId())));

    Actions actions = new Actions(driver);
    actions.moveToElement(manageInjuriesButton).click().perform();

    // Verify that the InjuryDialog appears
    WebElement dialog = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("overlay")));
    assertTrue(dialog.isDisplayed());

    // Verify the injuries are in the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Bruised Ribs"));
          } catch (Exception e) {
            return false;
          }
        });
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Twisted Ankle"));
          } catch (Exception e) {
            return false;
          }
        });

    // Heal an injury
    WebElement healButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("heal-injury-" + injuryToHeal.getId())));
    clickAndScrollIntoView(healButton);

    // Create a new injury
    WebElement createButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-injury-button")));
    clickAndScrollIntoView(createButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Fill the form
    WebElement nameField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-injury-name")));
    nameField.sendKeys("Broken Leg");
    WebElement descriptionField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("create-injury-description")));
    descriptionField.sendKeys("A very broken leg.");
    WebElement severitySelector =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-injury-severity")));
    clickAndScrollIntoView(severitySelector);

    severitySelector.sendKeys("CRITICAL", Keys.TAB);

    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-injury-save-button")));
    clickAndScrollIntoView(saveButton);

    // Verify the new injury is in the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Broken Leg"));
          } catch (Exception e) {
            return false;
          }
        });
  }
}

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
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WrestlerListViewE2ETest extends AbstractE2ETest {

  @Autowired private InjuryService injuryService;
  @Autowired private InjuryRepository injuryRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private WrestlerService wrestlerService;

  private Universe defaultUniverse;

  @BeforeEach
  void setUp() {
    cleanupLeagues();
    injuryRepository.deleteAll();
    segmentRepository.deleteAll();
    wrestlerStateRepository.deleteAll();
    wrestlerRepository.deleteAll();
    universeRepository.deleteAll();

    defaultUniverse =
        universeRepository.save(Universe.builder().id(1L).name("Default Universe").build());

    // Create some wrestlers for the tests
    for (int i = 0; i < 4; i++) {
      Wrestler w = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler " + i));
      wrestlerService.getOrCreateState(w.getId(), defaultUniverse.getId());
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
    Assertions.assertNotNull(createButton);
    clickElement(createButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));

    // Find the components
    WebElement nameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-dialog-name-field")));

    // Enter a new wrestler name
    Assertions.assertNotNull(nameField);
    nameField.sendKeys("Test Wrestler", Keys.TAB);

    // Click the save button
    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("wrestler-dialog-save-button")));
    Assertions.assertNotNull(saveButton);
    clickElement(saveButton);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog")));

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
  void testDefaultSorting() {
    wrestlerStateRepository.deleteAll();
    wrestlerRepository.deleteAll();
    Wrestler w1 = wrestlerService.save(TestUtils.createWrestler("Zack"));
    wrestlerService.getOrCreateState(w1.getId(), defaultUniverse.getId());
    Wrestler w2 = wrestlerService.save(TestUtils.createWrestler("Adam"));
    wrestlerService.getOrCreateState(w2.getId(), defaultUniverse.getId());
    Wrestler w3 = wrestlerService.save(TestUtils.createWrestler("Ben"));
    wrestlerService.getOrCreateState(w3.getId(), defaultUniverse.getId());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");

    // Wait for the grid to settle
    waitForGridToSettle("wrestler-list-grid", Duration.ofSeconds(30));

    // Get all cell contents
    List<WebElement> cells = driver.findElements(By.tagName("vaadin-grid-cell-content"));
    List<String> namesInOrder =
        cells.stream()
            .map(WebElement::getText)
            .map(String::trim)
            .filter(text -> text.equals("Adam") || text.equals("Ben") || text.equals("Zack"))
            .toList();

    assertEquals(3, namesInOrder.size(), "Should find all three wrestler names in the grid");
    assertEquals("Adam", namesInOrder.get(0));
    assertEquals("Ben", namesInOrder.get(1));
    assertEquals("Zack", namesInOrder.get(2));
  }

  @Test
  void testEditWrestler() {

    // Create a wrestler to edit

    Wrestler wrestler = wrestlerService.save(TestUtils.createWrestler("Edit"));
    wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler

    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    Assertions.assertNotNull(menuBar);

    // Select "Edit" from the menu

    selectFromVaadinMenuBar(menuBar, "Edit");

    // Wait for the dialog to appear

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));

    // Find the editor's name field and change the value

    WebElement nameEditor =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-dialog-name-field")));

    Assertions.assertNotNull(nameEditor);

    nameEditor.sendKeys(" Updated", Keys.TAB);

    // Find the "Save" button and click it

    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("wrestler-dialog-save-button")));

    Assertions.assertNotNull(saveButton);

    clickElement(saveButton);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog")));

    // Verify that the grid is updated

    wait.until(
        d -> {
          try {

            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Edit Updated"));

          } catch (Exception e) {

            return false;
          }
        });

    assertTrue(
        wrestlerRepository.findAll().stream().anyMatch(w -> w.getName().equals("Edit Updated")));
  }

  @Test
  void testDeleteWrestler() {

    // Create a wrestler to delete

    Wrestler wrestler = wrestlerService.save(TestUtils.createWrestler("Delete"));
    wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    long initialSize = wrestlerRepository.count();

    // Find the menu for the wrestler

    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    Assertions.assertNotNull(menuBar);

    // Select "Delete" from the menu

    selectFromVaadinMenuBar(menuBar, "Delete");

    // Verify that the wrestler is removed from the grid

    wait.until(
        d -> {
          try {

            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .noneMatch(it -> it.getText().equals("Delete"));

          } catch (Exception e) {

            return false;
          }
        });

    assertEquals(initialSize - 1, wrestlerRepository.count());
  }

  @Test
  void testAddBump() {

    // Create a wrestler

    Wrestler wrestler = wrestlerService.save(TestUtils.createWrestler("Bump"));
    wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler

    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    Assertions.assertNotNull(menuBar);

    // Select "Add Bump" from the menu

    selectFromVaadinMenuBar(menuBar, "Add Bump");

    // Verify that the bump count is updated

    wait.until(
        d -> {
          try {

            Assertions.assertNotNull(wrestler.getId());

            return wrestlerService
                    .getOrCreateState(wrestler.getId(), defaultUniverse.getId())
                    .getBumps()
                == 1;

          } catch (Exception e) {

            return false;
          }
        });

    Assertions.assertNotNull(wrestler.getId());

    assertEquals(
        1, wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId()).getBumps());
  }

  @Test
  void testHealBump() {

    // Create a wrestler with a bump

    Wrestler wrestler = wrestlerService.save(TestUtils.createWrestler("Heal Bump"));
    WrestlerState state =
        wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    state.setBumps(1);
    wrestlerStateRepository.saveAndFlush(state);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler

    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    Assertions.assertNotNull(menuBar);

    // Select "Heal Bump" from the menu

    selectFromVaadinMenuBar(menuBar, "Heal Bump");

    // Verify that the bump count is updated

    wait.until(
        d -> {
          try {

            Assertions.assertNotNull(wrestler.getId());

            return wrestlerService
                    .getOrCreateState(wrestler.getId(), defaultUniverse.getId())
                    .getBumps()
                == 0;

          } catch (Exception e) {

            return false;
          }
        });

    Assertions.assertNotNull(wrestler.getId());

    assertEquals(
        0, wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId()).getBumps());
  }

  @Test
  void testManageInjuries() {

    // Create a wrestler

    Wrestler wrestler = wrestlerService.save(TestUtils.createWrestler("Injuries"));
    wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());

    // Create a couple of injuries for the wrestler

    injuryService.createInjury(
        wrestler.getId(),
        defaultUniverse.getId(),
        "Bruised Ribs",
        "Slightly bruised ribs.",
        InjurySeverity.MINOR,
        "Fell off the top rope.");

    Injury injuryToHeal =
        injuryService
            .createInjury(
                wrestler.getId(),
                defaultUniverse.getId(),
                "Twisted Ankle",
                "Twisted his ankle.",
                InjurySeverity.MODERATE,
                "Landed awkwardly.")
            .get();

    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler

    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    Assertions.assertNotNull(menuBar);

    // Select "Manage Injuries" from the menu

    selectFromVaadinMenuBar(menuBar, "Manage Injuries");

    // Verify that the InjuryDialog appears

    WebElement dialog =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));

    Assertions.assertNotNull(dialog);

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

    Assertions.assertNotNull(healButton);

    clickElement(healButton);

    // Create a new injury

    WebElement createButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-injury-button")));

    Assertions.assertNotNull(createButton);

    clickElement(createButton);

    // Wait for the dialog to appear

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));

    // Fill the form

    WebElement nameField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-injury-name")));

    Assertions.assertNotNull(nameField);

    nameField.sendKeys("Broken Leg");

    WebElement descriptionField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("create-injury-description")));

    Assertions.assertNotNull(descriptionField);

    descriptionField.sendKeys("A very broken leg.");

    WebElement severitySelector =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-injury-severity")));

    Assertions.assertNotNull(severitySelector);

    clickElement(severitySelector);

    severitySelector.sendKeys("CRITICAL", Keys.TAB);

    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-injury-save-button")));

    Assertions.assertNotNull(saveButton);

    clickElement(saveButton);

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

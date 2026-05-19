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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Tag("video")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WrestlerListViewE2ETest extends AbstractE2ETest {

  @Autowired private InjuryService injuryService;
  @Autowired private InjuryRepository injuryRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private WrestlerService wrestlerService;

  @BeforeEach
  public void setUp() {
    cleanupLeagues();

    // Create some wrestlers for the tests
    for (int i = 0; i < 4; i++) {
      Wrestler w = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Wrestler " + i));
      wrestlerService.getOrCreateState(w.getId(), defaultUniverse.getId());
    }
  }

  @Test
  void testCreateWrestler() {
    setVideoInfo("Roster Management", "Creating a Wrestler", "create-wrestler");

    navigateTo("wrestler-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Get the initial size of the grid
    long initialSize = wrestlerRepository.count();

    waitForGridToSettle("wrestler-list-grid", Duration.ofSeconds(30));
    captureCaption(
        "Wrestler List — your full roster at a glance. Click Create Wrestler to add a new"
            + " athlete to the promotion. Each row shows core attributes and provides an"
            + " action menu for editing, injuries, bumps, and more.",
        4000);

    // Click the "Create Wrestler" button
    WebElement createButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-wrestler-button")));
    assertNotNull(createButton);
    clickElement(createButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));

    // Find the components
    WebElement nameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-dialog-name-field")));
    captureCaption(
        "Enter the wrestler's name and configure their starting attributes — health,"
            + " stamina, deck size, and gender. These values shape how the wrestler"
            + " performs in matches and which titles they are eligible for.",
        4000);

    // Enter a new wrestler name
    assertNotNull(nameField);
    nameField.sendKeys("Test Wrestler", Keys.TAB);

    // Click the save button
    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("wrestler-dialog-save-button")));
    assertNotNull(saveButton);
    clickElement(saveButton);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog")));

    waitForGridToSettle("wrestler-list-grid", Duration.ofSeconds(30));
    captureCaption(
        "The new wrestler appears in the roster immediately, ready to be booked into shows,"
            + " assigned to factions, and given a card deck. The roster is sorted"
            + " alphabetically by default for easy navigation.",
        4000);

    assertEquals(initialSize + 1, wrestlerRepository.count());
  }

  @Test
  void testDefaultSorting() {
    wrestlerStateRepository.deleteAll();
    wrestlerRepository.deleteAll();
    Wrestler w1 = wrestlerService.save(TestUtils.createWrestler("Zack"));
    assertNotNull(w1.getId());
    assertNotNull(defaultUniverse.getId());
    wrestlerService.getOrCreateState(w1.getId(), defaultUniverse.getId());
    Wrestler w2 = wrestlerService.save(TestUtils.createWrestler("Adam"));
    assertNotNull(w2.getId());
    wrestlerService.getOrCreateState(w2.getId(), defaultUniverse.getId());
    Wrestler w3 = wrestlerService.save(TestUtils.createWrestler("Ben"));
    assertNotNull(w3.getId());
    wrestlerService.getOrCreateState(w3.getId(), defaultUniverse.getId());

    navigateTo("wrestler-list");

    // Wait for the grid to settle
    waitForGridToSettle("wrestler-list-grid", Duration.ofSeconds(30));

    // Get all cell contents
    List<WebElement> cells = driver.findElements(By.tagName("vaadin-grid-cell-content"));
    List<String> namesInOrder =
        cells.stream()
            .map(WebElement::getText)
            .map(String::trim)
            .filter(text -> "Adam".equals(text) || "Ben".equals(text) || "Zack".equals(text))
            .toList();

    assertEquals(3, namesInOrder.size(), "Should find all three wrestler names in the grid");
    assertEquals("Adam", namesInOrder.get(0));
    assertEquals("Ben", namesInOrder.get(1));
    assertEquals("Zack", namesInOrder.get(2));
  }

  @Test
  void testEditWrestler() {
    setVideoInfo("Roster Management", "Editing a Wrestler", "edit-wrestler");

    // Create a wrestler to edit
    Wrestler wrestler = wrestlerService.save(TestUtils.createWrestler("Edit"));
    wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());
    navigateTo("wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    waitForGridToSettle("wrestler-list-grid", Duration.ofSeconds(30));
    captureCaption(
        "Each wrestler row has an action menu — open it to edit attributes, manage"
            + " injuries, add or heal bumps, and navigate to the full profile view."
            + " All operations apply to just this wrestler.",
        4000);

    // Find the menu for the wrestler
    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    assertNotNull(menuBar);

    // Select "Edit" from the menu
    selectFromVaadinMenuBar(menuBar, "Edit");

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    captureCaption(
        "The Edit dialog lets you update the wrestler's name and core attributes — health,"
            + " stamina, deck size, gender, and active status. Changes take effect"
            + " immediately when you click Save.",
        4000);

    // Find the editor's name field and change the value
    WebElement nameEditor =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("wrestler-dialog-name-field")));

    assertNotNull(nameEditor);

    nameEditor.sendKeys(" Updated", Keys.TAB);

    // Find the "Save" button and click it
    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("wrestler-dialog-save-button")));

    assertNotNull(saveButton);
    clickElement(saveButton);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog")));

    // Verify that the grid is updated
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> "Edit Updated".equals(it.getText()));
          } catch (Exception e) {

            return false;
          }
        });
    captureCaption(
        "Changes are saved immediately and reflected in the roster grid with no page"
            + " reload. The updated name also propagates to any show segments or faction"
            + " memberships that reference this wrestler.",
        4000);

    assertTrue(
        wrestlerRepository.findAll().stream().anyMatch(w -> "Edit Updated".equals(w.getName())));
  }

  @Test
  void testDeleteWrestler() {

    // Create a wrestler to delete
    Wrestler wrestler = wrestlerService.save(TestUtils.createWrestler("Delete"));
    wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());

    navigateTo("wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    long initialSize = wrestlerRepository.count();

    // Find the menu for the wrestler
    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    assertNotNull(menuBar);

    // Select "Delete" from the menu
    selectFromVaadinMenuBar(menuBar, "Delete");

    // Verify that the wrestler is removed from the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .noneMatch(it -> "Delete".equals(it.getText()));
          } catch (Exception e) {

            return false;
          }
        });

    assertEquals(initialSize - 1, wrestlerRepository.count());
  }

  @Test
  void testAddBump() {

    // Create a wrestle
    Wrestler wrestler = wrestlerService.save(TestUtils.createWrestler("Bump"));
    wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId());

    navigateTo("wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler
    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    assertNotNull(menuBar);

    // Select "Add Bump" from the menu
    selectFromVaadinMenuBar(menuBar, "Add Bump");

    // Verify that the bump count is updated
    wait.until(
        d -> {
          try {
            assertNotNull(wrestler.getId());
            return wrestlerService
                    .getOrCreateState(wrestler.getId(), defaultUniverse.getId())
                    .getBumps()
                == 1;

          } catch (Exception e) {

            return false;
          }
        });

    assertNotNull(wrestler.getId());

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

    navigateTo("wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler
    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    assertNotNull(menuBar);

    // Select "Heal Bump" from the menu
    selectFromVaadinMenuBar(menuBar, "Heal Bump");

    // Verify that the bump count is updated
    wait.until(
        d -> {
          try {
            assertNotNull(wrestler.getId());
            return wrestlerService
                    .getOrCreateState(wrestler.getId(), defaultUniverse.getId())
                    .getBumps()
                == 0;
          } catch (Exception e) {
            return false;
          }
        });

    assertNotNull(wrestler.getId());

    assertEquals(
        0, wrestlerService.getOrCreateState(wrestler.getId(), defaultUniverse.getId()).getBumps());
  }

  @Test
  void testManageInjuries() {
    setVideoInfo("Roster Management", "Managing Wrestler Injuries", "manage-wrestler-injuries");

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

    navigateTo("wrestler-list");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Find the menu for the wrestler
    WebElement menuBar =
        wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("action-menu-" + wrestler.getId())));

    assertNotNull(menuBar);

    // Select "Manage Injuries" from the menu
    selectFromVaadinMenuBar(menuBar, "Manage Injuries");

    // Verify that the InjuryDialog appears
    WebElement dialog =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    assertNotNull(dialog);
    assertTrue(dialog.isDisplayed());

    // Verify the injuries are in the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> "Bruised Ribs".equals(it.getText()));
          } catch (Exception e) {
            return false;
          }
        });

    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> "Twisted Ankle".equals(it.getText()));
          } catch (Exception e) {
            return false;
          }
        });
    captureCaption(
        "Manage Injuries shows all active injuries for a wrestler — severity (Minor through"
            + " Critical), cause description, and a Heal button for each. Injuries can"
            + " affect booking availability and AI narration tone.",
        4500);

    // Heal an injury
    WebElement healButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("heal-injury-" + injuryToHeal.getId())));

    assertNotNull(healButton);
    clickElement(healButton);
    captureCaption(
        "Clicking Heal removes the injury from the active list immediately — the wrestler"
            + " is marked recovered and becomes fully eligible for booking again."
            + " Healed injuries are retained in history for reference.",
        3500);

    // Create a new injury
    WebElement createButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-injury-button")));

    assertNotNull(createButton);
    clickElement(createButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));

    // Fill the form
    WebElement nameField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-injury-name")));

    assertNotNull(nameField);
    nameField.sendKeys("Broken Leg");

    WebElement descriptionField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("create-injury-description")));

    assertNotNull(descriptionField);
    descriptionField.sendKeys("A very broken leg.");

    WebElement severitySelector =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-injury-severity")));

    assertNotNull(severitySelector);
    clickElement(severitySelector);
    severitySelector.sendKeys("CRITICAL", Keys.TAB);
    captureCaption(
        "New injuries can be logged at any time — name, description, severity (Minor,"
            + " Moderate, Serious, or Critical), and the in-kayfabe cause are all recorded"
            + " for narrative and booking reference purposes.",
        4000);

    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-injury-save-button")));

    assertNotNull(saveButton);
    clickElement(saveButton);

    // Verify the new injury is in the grid (use a longer wait for the Vaadin round-trip)
    new WebDriverWait(driver, Duration.ofSeconds(30))
        .until(
            d -> {
              try {
                return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                    .anyMatch(it -> "Broken Leg".equals(it.getText()));
              } catch (Exception e) {
                return false;
              }
            });
    captureCaption(
        "The new injury appears in the grid — it will affect the wrestler's booking"
            + " availability and appear in AI narration commentary until it is healed."
            + " Critical injuries are flagged prominently to alert bookers.",
        4000);
  }
}

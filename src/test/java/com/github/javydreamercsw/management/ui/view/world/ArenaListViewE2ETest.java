/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import java.time.Duration;
import java.util.Set;
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
class ArenaListViewE2ETest extends AbstractE2ETest {

  @Autowired private ArenaService arenaService;
  @Autowired private ArenaRepository arenaRepository;
  @Autowired private LocationService locationService;
  @Autowired private LocationRepository locationRepository;

  @BeforeEach
  void setUp() {
    cleanupLeagues();
    arenaRepository.deleteAll();
    locationRepository.deleteAll();
  }

  @Test
  void testCreateArena() {
    Location location = locationService.createLocation("Arena City", "Desc", null, Set.of());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/arena-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Get the initial size of the grid
    long initialSize = arenaRepository.count();

    // Click the "Add Arena" button
    WebElement addBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("add-arena-button")));
    Assertions.assertNotNull(addBtn);
    clickElement(addBtn);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("arena-form-dialog")));

    // Find the components
    WebElement nameField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("arena-name-field")));
    Assertions.assertNotNull(nameField);
    nameField.sendKeys("E2E Test Arena", Keys.TAB);

    WebElement locationField = driver.findElement(By.id("arena-location-field"));
    locationField.click();
    locationField.sendKeys(location.getName(), Keys.ENTER, Keys.TAB);

    WebElement capacityField = driver.findElement(By.id("arena-capacity-field"));
    capacityField.sendKeys("15000", Keys.TAB);

    WebElement biasField = driver.findElement(By.id("arena-bias-field"));
    biasField.click();
    biasField.sendKeys("NEUTRAL", Keys.ENTER, Keys.TAB);

    WebElement descriptionField = driver.findElement(By.id("arena-description-field"));
    descriptionField.sendKeys("An arena created by E2E test", Keys.TAB);

    // Click the save button
    WebElement saveBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("arena-save-button")));
    Assertions.assertNotNull(saveBtn);
    clickElement(saveBtn);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("arena-form-dialog")));

    // Verify that the new arena appears in the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("E2E Test Arena"));
          } catch (Exception e) {
            return false;
          }
        });

    assertEquals(initialSize + 1, arenaRepository.count());
  }

  @Test
  void testEditArena() {
    Location location = locationService.createLocation("Edit Arena City", "Desc", null, Set.of());
    Arena arena =
        arenaService.createArena(
            "Edit Me Arena", "Desc", location.getId(), 5000, Arena.AlignmentBias.NEUTRAL, Set.of());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/arena-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for the grid to load
    waitForGridToSettle("arena-grid", Duration.ofSeconds(30));

    // Click edit button for the specific arena
    WebElement editBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("edit-arena-" + arena.getId())));
    clickElement(editBtn);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("arena-form-dialog")));

    WebElement nameField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("arena-name-field")));
    nameField.sendKeys(" Updated", Keys.TAB);

    // Click the save button
    WebElement saveBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("arena-save-button")));
    clickElement(saveBtn);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("arena-form-dialog")));

    // Verify update
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Edit Me Arena Updated"));
          } catch (Exception e) {
            return false;
          }
        });

    assertTrue(
        arenaRepository.findAll().stream()
            .anyMatch(a -> a.getName().equals("Edit Me Arena Updated")));
  }

  @Test
  void testDeleteArena() {
    Location location = locationService.createLocation("Delete Arena City", "Desc", null, Set.of());
    Arena arena =
        arenaService.createArena(
            "Delete Me Arena",
            "Desc",
            location.getId(),
            5000,
            Arena.AlignmentBias.NEUTRAL,
            Set.of());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/arena-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for the grid to load
    waitForGridToSettle("arena-grid", Duration.ofSeconds(30));

    long initialSize = arenaRepository.count();

    // Click delete button for the specific arena
    WebElement deleteBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("delete-arena-" + arena.getId())));
    clickElement(deleteBtn);

    // Confirm deletion in dialog
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("delete-arena-dialog")));
    WebElement confirmBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("confirm-delete-arena-button")));
    clickElement(confirmBtn);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("delete-arena-dialog")));

    // Verify deletion
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .noneMatch(it -> it.getText().equals("Delete Me Arena"));
          } catch (Exception e) {
            return false;
          }
        });

    assertEquals(initialSize - 1, arenaRepository.count());
  }
}

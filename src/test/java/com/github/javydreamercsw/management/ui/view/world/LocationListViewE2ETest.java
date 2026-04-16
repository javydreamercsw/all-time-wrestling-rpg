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
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
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
class LocationListViewE2ETest extends AbstractE2ETest {

  @Autowired private LocationService locationService;
  @Autowired private LocationRepository locationRepository;
  @Autowired private ArenaRepository arenaRepository;

  @BeforeEach
  void setUp() {
    cleanupLeagues();
    arenaRepository.deleteAll();
    locationRepository.deleteAll();
  }

  @Test
  void testCreateLocation() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/location-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Get the initial size of the grid
    long initialSize = locationRepository.count();

    // Click the "Add Location" button
    WebElement addBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("add-location-button")));
    Assertions.assertNotNull(addBtn);
    clickElement(addBtn);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("location-form-dialog")));

    // Find the components
    WebElement nameField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("location-name-field")));
    Assertions.assertNotNull(nameField);
    nameField.sendKeys("E2E Test City", Keys.TAB);

    WebElement descriptionField = driver.findElement(By.id("location-description-field"));
    descriptionField.sendKeys("A city created by E2E test", Keys.TAB);

    WebElement tagsField = driver.findElement(By.id("location-tags-field"));
    tagsField.sendKeys("E2E, Test", Keys.TAB);

    // Click the save button
    WebElement saveBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("location-save-button")));
    Assertions.assertNotNull(saveBtn);
    clickElement(saveBtn);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("location-form-dialog")));

    // Verify that the new location appears in the grid
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("E2E Test City"));
          } catch (Exception e) {
            return false;
          }
        });

    assertEquals(initialSize + 1, locationRepository.count());
  }

  @Test
  void testEditLocation() {
    Location location = locationService.createLocation("Edit Me City", "Desc", null, Set.of());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/location-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for the grid to load
    waitForGridToSettle("location-grid", Duration.ofSeconds(30));

    // Click edit button for the specific location
    WebElement editBtn =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("edit-location-" + location.getId())));
    clickElement(editBtn);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("location-form-dialog")));

    WebElement nameField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("location-name-field")));
    nameField.sendKeys(" Updated", Keys.TAB);

    // Click the save button
    WebElement saveBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("location-save-button")));
    clickElement(saveBtn);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("location-form-dialog")));

    // Verify update
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Edit Me City Updated"));
          } catch (Exception e) {
            return false;
          }
        });

    assertTrue(
        locationRepository.findAll().stream()
            .anyMatch(l -> l.getName().equals("Edit Me City Updated")));
  }

  @Test
  void testDeleteLocation() {
    Location location = locationService.createLocation("Delete Me City", "Desc", null, Set.of());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/location-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for the grid to load
    waitForGridToSettle("location-grid", Duration.ofSeconds(30));

    long initialSize = locationRepository.count();

    // Click delete button for the specific location
    WebElement deleteBtn =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("delete-location-" + location.getId())));
    clickElement(deleteBtn);

    // Confirm deletion in dialog
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("delete-location-dialog")));
    WebElement confirmBtn =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("confirm-delete-location-button")));
    clickElement(confirmBtn);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("delete-location-dialog")));

    // Verify deletion
    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .noneMatch(it -> it.getText().equals("Delete Me City"));
          } catch (Exception e) {
            return false;
          }
        });

    assertEquals(initialSize - 1, locationRepository.count());
  }
}

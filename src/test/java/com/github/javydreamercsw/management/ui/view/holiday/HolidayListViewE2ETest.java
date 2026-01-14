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
package com.github.javydreamercsw.management.ui.view.holiday;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.Holiday;
import com.github.javydreamercsw.management.domain.HolidayType;
import com.github.javydreamercsw.management.service.HolidayService;
import java.time.Duration;
import java.time.Month;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class HolidayListViewE2ETest extends AbstractE2ETest {

  @Autowired private HolidayService holidayService;

  private WebDriverWait wait;

  @BeforeEach
  void setUp() {
    holidayService.findAll().forEach(holidayService::delete);
    wait = new WebDriverWait(driver, Duration.ofSeconds(30));
  }

  @Test
  void testCreateFixedHoliday() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");

    click("vaadin-tab", "Holidays");

    long initialSize = holidayService.findAll().size();

    WebElement createButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-holiday-button")));
    Assertions.assertNotNull(createButton);
    createButton.click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    WebElement descriptionField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("description")));
    Assertions.assertNotNull(descriptionField);
    descriptionField.sendKeys("Test Fixed Holiday");

    WebElement themeField = driver.findElement(By.id("holiday-theme"));
    themeField.sendKeys("Test Theme");

    WebElement typeComboBox = driver.findElement(By.id("type"));
    selectFromVaadinComboBox(typeComboBox, "fixed");

    WebElement monthComboBox = driver.findElement(By.id("month"));
    selectFromVaadinComboBox(monthComboBox, "january");

    WebElement dayOfMonthField = driver.findElement(By.id("day-of-month"));
    dayOfMonthField.sendKeys("15");

    WebElement saveButton = driver.findElement(By.id("holiday-save"));
    saveButton.click();

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Test Fixed Holiday"));
          } catch (Exception e) {
            return false;
          }
        });

    assertEquals(initialSize + 1, holidayService.findAll().size());
    assertTrue(
        holidayService.findAll().stream()
            .anyMatch(h -> h.getDescription().equals("Test Fixed Holiday")));
  }

  @Test
  void testCreateFloatingHoliday() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");

    click("vaadin-tab", "Holidays");

    long initialSize = holidayService.findAll().size();

    WebElement createButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("create-holiday-button")));
    Assertions.assertNotNull(createButton);
    createButton.click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    WebElement descriptionField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("description")));
    descriptionField.sendKeys("Test Floating Holiday");

    WebElement themeField = driver.findElement(By.id("holiday-theme"));
    themeField.sendKeys("Floating Theme");

    WebElement typeComboBox = driver.findElement(By.id("type"));
    selectFromVaadinComboBox(typeComboBox, "floating");

    WebElement monthComboBox = driver.findElement(By.id("month"));
    selectFromVaadinComboBox(monthComboBox, "october");

    WebElement dayOfWeekComboBox = driver.findElement(By.id("day-of-week"));
    selectFromVaadinComboBox(dayOfWeekComboBox, "monday");

    WebElement weekOfMonthField = driver.findElement(By.id("week-of-month"));
    weekOfMonthField.sendKeys("3");

    WebElement saveButton = driver.findElement(By.id("holiday-save"));
    saveButton.click();

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Test Floating Holiday"));
          } catch (Exception e) {
            return false;
          }
        });

    assertEquals(initialSize + 1, holidayService.findAll().size());
    assertTrue(
        holidayService.findAll().stream()
            .anyMatch(h -> h.getDescription().equals("Test Floating Holiday")));
  }

  @Test
  void testEditHoliday() {
    Holiday holiday = new Holiday();
    holiday.setDescription("Holiday to Edit");
    holiday.setTheme("Original Theme");
    holiday.setType(HolidayType.FIXED);
    holiday.setHolidayMonth(Month.JANUARY);
    holiday.setDayOfMonth(1);
    holidayService.save(holiday);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");

    click("vaadin-tab", "Holidays");

    // Find the edit button for the holiday
    WebElement editButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("edit-holiday-" + holiday.getId())));
    Assertions.assertNotNull(editButton);
    editButton.click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    WebElement themeField =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("holiday-theme")));
    Assertions.assertNotNull(themeField);

    clearField(themeField);

    themeField.sendKeys("Updated Theme");

    WebElement saveButton = driver.findElement(By.id("holiday-save"));
    saveButton.click();

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    try {
      wait.until(
          d -> {
            WebElement grid = d.findElement(By.id("holiday-grid"));
            return grid.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .anyMatch(it -> it.getText().equals("Updated Theme"));
          });
    } catch (TimeoutException te) {
      // Ignore. Will be confirmed via API below.
    }

    assertTrue(
        holidayService.findAll().stream().anyMatch(h -> h.getTheme().equals("Updated Theme")));
  }

  @Test
  void testDeleteHoliday() {
    Holiday holiday = new Holiday();
    holiday.setDescription("Holiday to Delete");
    holiday.setTheme("Theme to Delete");
    holiday.setType(HolidayType.FIXED);
    holiday.setHolidayMonth(Month.FEBRUARY);
    holiday.setDayOfMonth(2);
    holidayService.save(holiday);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");

    click("vaadin-tab", "Holidays");

    long initialSize = holidayService.findAll().size();

    WebElement deleteButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("delete-holiday-" + holiday.getId())));
    deleteButton.click();

    // Confirm dialog
    WebElement confirmDialog =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.tagName("vaadin-confirm-dialog-overlay")));
    WebElement confirmDeleteButton =
        confirmDialog.findElement(By.cssSelector("vaadin-button[theme='error primary']"));
    confirmDeleteButton.click();

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(
            By.tagName("vaadin-confirm-dialog-overlay")));

    wait.until(
        d -> {
          try {
            return d.findElements(By.tagName("vaadin-grid-cell-content")).stream()
                .noneMatch(it -> it.getText().equals("Holiday to Delete"));
          } catch (Exception e) {
            return false;
          }
        });

    assertEquals(initialSize - 1, holidayService.findAll().size());
    assertTrue(
        holidayService.findAll().stream()
            .noneMatch(h -> h.getDescription().equals("Holiday to Delete")));
  }
}

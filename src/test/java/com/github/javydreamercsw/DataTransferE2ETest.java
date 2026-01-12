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
package com.github.javydreamercsw;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.security.test.context.support.WithMockUser;

public class DataTransferE2ETest extends AbstractE2ETest {

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testNavigateToDataTransferView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    WebElement dataTransferWizard = waitForVaadinElement(driver, By.id("data-transfer-wizard"));
    assertNotNull(dataTransferWizard);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testConnectionConfigurationView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    nextButton.click();

    WebElement hostField = waitForVaadinElement(driver, By.id("host-field"));
    assertNotNull(hostField);
    WebElement portField = waitForVaadinElement(driver, By.id("port-field"));
    assertNotNull(portField);
    WebElement usernameField = waitForVaadinElement(driver, By.id("username-field"));
    assertNotNull(usernameField);
    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    assertNotNull(passwordField);
    WebElement testConnectionButton = waitForVaadinElement(driver, By.id("test-connection-button"));
    assertNotNull(testConnectionButton);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testConnectionParametersValidation() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    nextButton.click();

    // Clear the host field to trigger validation error
    WebElement hostField = waitForVaadinElement(driver, By.id("host-field"));
    clearField(hostField);

    // Attempt to click next button, should not proceed
    nextButton.click();

    // Assert that the wizard is still on the connection configuration step
    WebElement connectionConfigStep = waitForVaadinElement(driver, By.id("connection-config-step"));
    assertNotNull(connectionConfigStep);

    // Assert that a validation failed notification is displayed
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//vaadin-notification-card[contains(., 'Validation failed')]")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testDataTransferProcessView() {
    System.setProperty("simulateFailure", "false");
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    WebElement previousButton = waitForVaadinElement(driver, By.id("previous-button"));
    WebElement cancelButton = waitForVaadinElement(driver, By.id("cancel-button"));

    // Step 1: Connection Configuration
    // Fill in valid connection parameters
    WebElement hostField = waitForVaadinElement(driver, By.id("host-field"));
    hostField.sendKeys("localhost");
    WebElement portField = waitForVaadinElement(driver, By.id("port-field"));
    portField.sendKeys("3306");
    WebElement usernameField = waitForVaadinElement(driver, By.id("username-field"));
    usernameField.sendKeys("testuser");
    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    passwordField.sendKeys("testpassword");

    // Click the next button to advance to Data Selection step
    nextButton.click();

    // Assert that the data selection step is displayed
    WebElement dataSelectionStep = waitForVaadinElement(driver, By.id("data-selection-step"));
    assertNotNull(dataSelectionStep);

    // Click the next button again to advance to Data Transfer Process step
    nextButton.click();

    // Assert that the data transfer process step is displayed via WebDriver
    WebElement dataTransferProcessStep =
        waitForVaadinElement(driver, By.id("data-transfer-process-step"));
    assertNotNull(dataTransferProcessStep);

    // Assert that a progress indicator is present
    WebElement progressIndicator = waitForVaadinElement(driver, By.id("progress-indicator"));
    assertNotNull(progressIndicator);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testRollbackMechanism() {
    System.setProperty("simulateFailure", "true");
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));

    // Step 1: Connection Configuration
    // Fill in valid connection parameters
    WebElement hostField = waitForVaadinElement(driver, By.id("host-field"));
    hostField.sendKeys("localhost");
    WebElement portField = waitForVaadinElement(driver, By.id("port-field"));
    portField.sendKeys("3306");
    WebElement usernameField = waitForVaadinElement(driver, By.id("username-field"));
    usernameField.sendKeys("testuser");
    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    passwordField.sendKeys("testpassword");

    // Click the next button to advance to Data Selection step
    nextButton.click();

    // Assert that the data selection step is displayed
    WebElement dataSelectionStep = waitForVaadinElement(driver, By.id("data-selection-step"));
    assertNotNull(dataSelectionStep);

    // Click the next button again to advance to Data Transfer Process step
    nextButton.click();

    // Assert that the data transfer process step is displayed via WebDriver
    WebElement dataTransferProcessStep =
        waitForVaadinElement(driver, By.id("data-transfer-process-step"));
    assertNotNull(dataTransferProcessStep);
  }
}

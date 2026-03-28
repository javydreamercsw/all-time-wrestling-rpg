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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.mysql.MySQLContainer;

public class DataTransferE2ETest extends AbstractE2ETest {

  private static final MySQLContainer MYSQL_CONTAINER =
      new MySQLContainer("mysql:8.0.26")
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass");

  static {
    MYSQL_CONTAINER.start();
  }

  @Test
  public void testNavigateToDataTransferView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    waitForVaadinClientToLoad();
    WebElement dataTransferWizard = waitForVaadinElement(driver, By.id("data-transfer-wizard"));
    assertNotNull(dataTransferWizard);
  }

  @Test
  public void testConnectionConfigurationView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    waitForVaadinClientToLoad();

    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    clickElement(nextButton);

    waitForVaadinClientToLoad();

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
  public void testConnectionParametersValidation() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    waitForVaadinClientToLoad();

    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    clickElement(nextButton);

    // Wait for the config step to become visible
    waitForVaadinClientToLoad();
    waitForVaadinElementVisible(By.id("connection-config-step"));

    // Clear the host field to trigger validation error
    WebElement hostField = waitForVaadinElement(driver, By.id("host-field"));
    clearField(hostField);

    // Attempt to click next button, should not proceed
    clickElement(nextButton);

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
  public void testDataTransferProcessView() throws SQLException {
    System.setProperty("simulateFailure", "false");
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    waitForVaadinClientToLoad();

    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    clickElement(nextButton);

    waitForVaadinClientToLoad();

    // Step 1: Connection Configuration
    // Fill in valid connection parameters
    WebElement hostField = waitForVaadinElement(driver, By.id("host-field"));
    hostField.sendKeys(MYSQL_CONTAINER.getHost());
    WebElement portField = waitForVaadinElement(driver, By.id("port-field"));
    portField.sendKeys(MYSQL_CONTAINER.getFirstMappedPort().toString());
    WebElement targetDatabaseField = waitForVaadinElement(driver, By.id("target-database-field"));
    clearField(targetDatabaseField);
    targetDatabaseField.sendKeys(MYSQL_CONTAINER.getDatabaseName());
    WebElement usernameField = waitForVaadinElement(driver, By.id("username-field"));
    usernameField.sendKeys(MYSQL_CONTAINER.getUsername());
    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    passwordField.sendKeys(MYSQL_CONTAINER.getPassword(), Keys.TAB);

    // Click the next button to advance to Data Selection step
    clickElement(nextButton);

    // Assert that the data selection step is displayed
    WebElement dataSelectionStep = waitForVaadinElement(driver, By.id("data-selection-step"));
    assertNotNull(dataSelectionStep);
    selectFromVaadinComboBox("table-selection-combo", "All Tables");

    // Click the next button again to advance to Data Transfer Process step
    nextButton = waitForVaadinElement(driver, By.id("next-button"));
    clickElement(nextButton);

    // Assert that the data transfer process step is displayed via WebDriver
    WebElement dataTransferProcessStep =
        waitForVaadinElement(driver, By.id("data-transfer-process-step"));

    assertNotNull(dataTransferProcessStep);

    // Assert that a progress indicator is present
    WebElement progressIndicator = waitForVaadinElement(driver, By.id("progress-indicator"));
    assertNotNull(progressIndicator);

    // Wait for completion message
    new WebDriverWait(driver, Duration.ofSeconds(60))
        .until(
            ExpectedConditions.textToBePresentInElementLocated(
                By.id("status-label"), "Data transfer completed successfully."));

    // Verify data in MySQL
    try (Connection conn =
            DriverManager.getConnection(
                MYSQL_CONTAINER.getJdbcUrl(),
                MYSQL_CONTAINER.getUsername(),
                MYSQL_CONTAINER.getPassword());
        Statement stmt = conn.createStatement()) {
      ResultSet rs = stmt.executeQuery("SELECT count(*) FROM wrestler");
      rs.next();
      int count = rs.getInt(1);
      assertTrue(count > 0, "Wrestlers should have been migrated to MySQL");
    }
  }

  @Test
  public void testRollbackMechanism() {
    System.setProperty("simulateFailure", "true");
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    waitForVaadinClientToLoad();

    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    clickElement(nextButton);

    waitForVaadinClientToLoad();

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
    clickElement(nextButton);

    // Assert that the data selection step is displayed
    WebElement dataSelectionStep = waitForVaadinElement(driver, By.id("data-selection-step"));
    assertNotNull(dataSelectionStep);

    // Click the next button again to advance to Data Transfer Process step
    clickElement(nextButton);

    // Assert that the data transfer process step is displayed via WebDriver
    WebElement dataTransferProcessStep =
        waitForVaadinElement(driver, By.id("data-transfer-process-step"));
    assertNotNull(dataTransferProcessStep);
  }

  @Test
  public void testRollbackFailureMechanism() {
    System.setProperty("simulateFailure", "true");
    System.setProperty("simulateRollbackFailure", "true");
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    waitForVaadinClientToLoad();

    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    clickElement(nextButton);

    waitForVaadinClientToLoad();

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
    clickElement(nextButton);

    // Assert that the data selection step is displayed

    WebElement dataSelectionStep = waitForVaadinElement(driver, By.id("data-selection-step"));
    assertNotNull(dataSelectionStep);

    // Click the next button again to advance to Data Transfer Process step (simulates failure)
    clickElement(nextButton);

    // Assert that the data transfer process step is displayed

    WebElement dataTransferProcessStep =
        waitForVaadinElement(driver, By.id("data-transfer-process-step"));
    assertNotNull(dataTransferProcessStep);
  }

  @Test
  public void testCancelButton() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    waitForVaadinClientToLoad();

    WebElement cancelButton = waitForVaadinElement(driver, By.id("cancel-button"));
    clickElement(cancelButton);

    // Verify redirection to home page (or context root)
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            ExpectedConditions.urlMatches(
                "http://localhost:" + serverPort + getContextPath() + "/?"));
  }

  @Test
  public void testBackButton() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    waitForVaadinClientToLoad(); // Wait for page to fully load

    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    clickElement(nextButton); // To Connection Config

    waitForVaadinClientToLoad();

    WebElement backButton = waitForVaadinElement(driver, By.id("back-button"));
    assertTrue(backButton.isEnabled(), "Back button should be enabled on step 1");

    clickElement(backButton);

    // Should be back to intro step
    WebElement welcomeHeader =
        waitForVaadinElement(
            driver, By.xpath("//h3[contains(text(), 'Welcome to Data Transfer Wizard')]"));
    assertNotNull(welcomeHeader);
  }

  @Test
  public void testDataTransferWithNonBlankPassword() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    waitForVaadinClientToLoad(); // Wait for page to fully load

    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    clickElement(nextButton);

    waitForVaadinClientToLoad();

    WebElement hostField = waitForVaadinElement(driver, By.id("host-field"));
    hostField.sendKeys(MYSQL_CONTAINER.getHost());

    WebElement portField = waitForVaadinElement(driver, By.id("port-field"));
    portField.sendKeys(MYSQL_CONTAINER.getFirstMappedPort().toString());

    WebElement targetDatabaseField = waitForVaadinElement(driver, By.id("target-database-field"));
    clearField(targetDatabaseField);
    targetDatabaseField.sendKeys(MYSQL_CONTAINER.getDatabaseName());

    WebElement usernameField = waitForVaadinElement(driver, By.id("username-field"));
    usernameField.sendKeys(MYSQL_CONTAINER.getUsername());

    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    passwordField.sendKeys(MYSQL_CONTAINER.getPassword());

    clickElement(nextButton);

    WebElement dataSelectionStep = waitForVaadinElement(driver, By.id("data-selection-step"));
    assertNotNull(dataSelectionStep);

    selectFromVaadinComboBox("table-selection-combo", "Wrestler");

    // Verify next button is present and says "Transfer Data"

    WebElement transferButton = waitForVaadinElement(driver, By.id("next-button"));
    assertNotNull(transferButton);

    assertTrue(
        transferButton.getText().contains("Transfer Data")
            || Objects.requireNonNull(transferButton.getAttribute("innerText"))
                .contains("Transfer Data"));

    clickElement(transferButton);

    // Assert that the data transfer process step is displayed via WebDriver
    WebElement dataTransferProcessStep =
        waitForVaadinElement(driver, By.id("data-transfer-process-step"));
    assertNotNull(dataTransferProcessStep);
  }
}

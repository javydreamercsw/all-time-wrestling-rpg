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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.mysql.MySQLContainer;

@Tag("video")
public class DataTransferE2ETest extends AbstractE2ETest {

  private static final MySQLContainer MYSQL_CONTAINER =
      new MySQLContainer("mysql:8.0")
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass");

  static {
    MYSQL_CONTAINER.start();
  }

  @Test
  public void testNavigateToDataTransferView() {
    navigateTo("data-transfer");
    WebElement dataTransferWizard = waitForVaadinElement(driver, By.id("data-transfer-wizard"));
    assertNotNull(dataTransferWizard);
  }

  @Test
  public void testConnectionConfigurationView() {
    navigateTo("data-transfer");

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
    navigateTo("data-transfer");

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
    setVideoInfo("Admin", "MySQL Data Migration Wizard", "data-transfer-full-wizard");

    System.setProperty("simulateFailure", "false");
    navigateTo("data-transfer");

    WebElement nextButton = waitForVaadinElement(driver, By.id("next-button"));
    captureCaption(
        "Data Transfer Wizard — migrate all application data from H2 to MySQL in three"
            + " steps: connection config, table selection, and transfer. This is the"
            + " recommended path before switching to a production MySQL deployment.",
        4000);
    clickElement(nextButton);

    waitForVaadinClientToLoad();

    // Step 1: Connection Configuration
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
    captureCaption(
        "Step 1 — enter the MySQL connection details: host, port, database name, username,"
            + " and password. Use Test Connection to verify connectivity before proceeding;"
            + " the wizard will not advance if the connection fails.",
        4000);

    // Click the next button to advance to Data Selection step
    clickElement(nextButton);

    // Assert that the data selection step is displayed
    WebElement dataSelectionStep = waitForVaadinElement(driver, By.id("data-selection-step"));
    assertNotNull(dataSelectionStep);
    selectFromVaadinComboBox("table-selection-combo", "All Tables");
    captureCaption(
        "Step 2 — choose which tables to migrate. 'All Tables' transfers the complete"
            + " dataset; individual tables can be selected for targeted or partial migrations"
            + " without touching the rest of the schema.",
        4000);

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
    captureCaption(
        "Step 3 — transfer runs in the background with a live progress indicator."
            + " The process is fully transactional: if anything fails, the entire migration"
            + " rolls back automatically so no partial data is left in MySQL.",
        4500);

    // Wait for completion message
    new WebDriverWait(driver, Duration.ofMinutes(2))
        .until(
            ExpectedConditions.textToBePresentInElementLocated(
                By.id("status-label"), "Data transfer completed successfully."));
    captureCaption(
        "Migration complete — all data is now in MySQL and verified. Restart the"
            + " application with the MySQL Spring profile (spring.profiles.active=mysql)"
            + " to run in production mode against the new database.",
        4000);

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
    navigateTo("data-transfer");

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
    navigateTo("data-transfer");

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
    navigateTo("data-transfer");

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
    navigateTo("data-transfer"); // Wait for page to fully load

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
    navigateTo("data-transfer"); // Wait for page to fully load

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

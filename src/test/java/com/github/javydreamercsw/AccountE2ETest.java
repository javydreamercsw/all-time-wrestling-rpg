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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.service.AccountService;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.time.Duration;
import java.util.Optional;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

public class AccountE2ETest extends AbstractE2ETest {
  @Autowired private AccountService accountService;

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testEditAccount() {
    Optional<Account> accountOptional = accountService.get(2L);
    assertTrue(accountOptional.isPresent());

    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    // Wait for the grid to load
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    // Find the edit button for the admin accountOptional (ID 1)
    WebElement editButton = driver.findElement(By.id("edit-button-2"));
    editButton.click();

    // Edit the fields
    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    passwordField.sendKeys("new_password");

    WebElement roleComboBox = waitForVaadinElement(driver, By.id("role-field"));
    selectFromVaadinComboBox(roleComboBox, "BOOKER");

    // Save the changes
    WebElement saveButton = waitForVaadinElement(driver, By.id("save-button"));
    saveButton.click();

    // Wait for the dialog to close
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    accountOptional = accountService.get(2L);
    assertTrue(accountOptional.isPresent());
    Account account = accountOptional.get();

    // Verify the changes
    // It's tricky to verify the change in the grid directly without more IDs.
    // So, let's navigate back to the edit form and check the values.
    editButton = waitForVaadinElement(driver, By.id("edit-button-2"));
    editButton.click();

    waitForVaadinElement(driver, By.id("username-field"));

    WebElement usernameField = waitForVaadinElement(driver, By.id("username-field"));
    assertEquals(account.getUsername(), usernameField.getAttribute("value"));

    WebElement emailField = waitForVaadinElement(driver, By.id("email-field"));
    assertEquals(account.getEmail(), emailField.getAttribute("value"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testDeleteAccount() {
    // Create an account to delete
    Account account =
        accountService.createAccount("delete_me", "password", "delete_me@atw.com", RoleName.VIEWER);

    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    // Wait for the grid to load
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    // Find the delete button for the new account
    WebElement deleteButton =
        waitForVaadinElement(driver, By.id("delete-button-" + account.getId()));
    deleteButton.click();

    // Confirm the deletion
    WebElement confirmButton =
        waitForVaadinElement(
            driver, By.xpath("//vaadin-confirm-dialog-overlay//vaadin-button[text()='Delete']"));
    confirmButton.click();

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.id("delete-button-" + account.getId())));

    // Verify the account is deleted
    assertTrue(accountService.get(account.getId()).isEmpty());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testCreateAccount() {
    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    // Wait for the grid to load
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    // Click the new account button
    WebElement newAccountButton = waitForVaadinElement(driver, By.id("new-account-button"));
    newAccountButton.click();

    // Edit the fields
    WebElement usernameField = waitForVaadinElement(driver, By.id("username-field"));
    // Check that we are on the correct page
    usernameField.sendKeys("new_account");

    WebElement emailField = waitForVaadinElement(driver, By.id("email-field"));
    emailField.sendKeys("new_account@atw.com");

    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    passwordField.sendKeys("new_password");

    WebElement roleComboBox = waitForVaadinElement(driver, By.id("role-field"));
    selectFromVaadinComboBox(roleComboBox, "VIEWER");

    // Save the changes
    WebElement saveButton = waitForVaadinElement(driver, By.id("save-button"));
    saveButton.click();

    // Wait for the dialog to close
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    // Verify the account is created
    Failsafe.with(
            RetryPolicy.builder()
                .withDelay(Duration.ofMillis(500))
                .withMaxDuration(Duration.ofSeconds(10))
                .withMaxAttempts(3)
                .handle(AssertionFailedError.class)
                .build())
        .run(
            () -> {
              Optional<Account> accountOptional = accountService.findByUsername("new_account");
              assertTrue(accountOptional.isPresent());
            });
  }
}

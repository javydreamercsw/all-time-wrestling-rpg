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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.test.context.support.WithMockUser;

public class AccountE2ETest extends AbstractE2ETest {
  @Autowired
  @Qualifier("managementAccountService") private AccountService accountService;

  @BeforeEach
  public void cleanUpAccounts() {
    accountService.findAll().stream()
        .filter(
            a ->
                a.getUsername().startsWith("delete_me")
                    || a.getUsername().startsWith("new_account")
                    || a.getUsername().startsWith("edit_me"))
        .forEach(a -> accountService.delete(a.getId()));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testEditAccount() {
    String username = "edit_me_" + System.currentTimeMillis();
    Account testAccount =
        accountService.createAccount(
            username, "ValidPassword1!", username + "@atw.com", RoleName.VIEWER);
    Long accountId = testAccount.getId();

    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    // Wait for the grid to load
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    // Find the edit button for the test account
    WebElement editButton = waitForVaadinElement(driver, By.id("edit-button-" + accountId));
    clickElement(editButton);

    // Edit the fields
    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    passwordField.sendKeys("ValidPassword1!");

    WebElement roleComboBox = waitForVaadinElement(driver, By.id("role-field"));
    selectFromVaadinComboBox(roleComboBox, "BOOKER");

    // Save the changes
    WebElement saveButton = waitForVaadinElement(driver, By.id("save-button"));
    clickElement(saveButton);

    // Wait for the dialog to close
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog")));

    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    Optional<Account> accountOptional = accountService.get(accountId);
    assertTrue(accountOptional.isPresent());
    Account account = accountOptional.get();

    // Verify the changes
    editButton = waitForVaadinElement(driver, By.id("edit-button-" + accountId));
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
    String username = "delete_me_" + System.currentTimeMillis();
    Account account =
        accountService.createAccount(
            username, "ValidPassword1!", username + "@atw.com", RoleName.VIEWER);

    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    // Wait for the grid to load
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    // Find the delete button for the new account
    WebElement deleteButton =
        waitForVaadinElement(driver, By.id("delete-button-" + account.getId()));
    clickElement(deleteButton);

    // Confirm the deletion
    WebElement confirmButton =
        waitForVaadinElement(
            driver, By.xpath("//vaadin-confirm-dialog//vaadin-button[text()='Delete']"));
    clickElement(confirmButton);

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.id("delete-button-" + account.getId())));

    // Verify the account is deleted
    assertTrue(accountService.get(account.getId()).isEmpty());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testCreateAccount() {
    String username = "new_account_" + System.currentTimeMillis();
    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    // Wait for the grid to load
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    // Click the new account button
    WebElement newAccountButton = waitForVaadinElement(driver, By.id("new-account-button"));
    clickElement(newAccountButton);

    // Edit the fields
    WebElement usernameField = waitForVaadinElement(driver, By.id("username-field"));
    // Check that we are on the correct page
    usernameField.sendKeys(username);

    WebElement emailField = waitForVaadinElement(driver, By.id("email-field"));
    emailField.sendKeys(username + "@atw.com");

    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    passwordField.sendKeys("ValidPassword1!");

    WebElement roleComboBox = waitForVaadinElement(driver, By.id("role-field"));
    selectFromVaadinComboBox(roleComboBox, "VIEWER");

    // Save the changes
    WebElement saveButton = waitForVaadinElement(driver, By.id("save-button"));
    clickElement(saveButton);

    // Wait for the dialog to close
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog")));

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
              Optional<Account> accountOptional = accountService.findByUsername(username);
              assertTrue(accountOptional.isPresent());
            });
  }
}

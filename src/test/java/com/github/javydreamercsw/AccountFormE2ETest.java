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
import com.github.javydreamercsw.management.service.AccountService;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

public class AccountFormE2ETest extends AbstractE2ETest {
  @Autowired private AccountService accountService;

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testEditAccount() {
    Optional<Account> accountOptional = accountService.get(1L);
    assertTrue(accountOptional.isPresent());

    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    // Wait for the grid to load
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    // Find the edit button for the admin accountOptional (ID 1)
    WebElement editButton = driver.findElement(By.id("edit-button-1"));
    editButton.click();

    // Edit the fields
    WebElement usernameField = waitForVaadinElement(driver, By.id("username-field"));
    // Check that we are on the correct page
    assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).endsWith("/account/1"));
    usernameField.sendKeys("_edited");

    WebElement emailField = waitForVaadinElement(driver, By.id("email-field"));
    emailField.sendKeys("edited_email@atw.com");

    WebElement passwordField = waitForVaadinElement(driver, By.id("password-field"));
    passwordField.sendKeys("new_password");

    WebElement roleComboBox = waitForVaadinElement(driver, By.id("role-field"));
    selectFromVaadinComboBox(roleComboBox, "BOOKER");

    // Save the changes
    WebElement saveButton = waitForVaadinElement(driver, By.id("save-button"));
    saveButton.click();

    // Navigate to the AccountListView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/account-list");

    accountOptional = accountService.get(1L);
    assertTrue(accountOptional.isPresent());
    Account account = accountOptional.get();

    // Verify the changes
    // It's tricky to verify the change in the grid directly without more IDs.
    // So, let's navigate back to the edit form and check the values.
    editButton = waitForVaadinElement(driver, By.id("edit-button-1"));
    editButton.click();

    waitForVaadinElement(driver, By.id("username-field"));

    usernameField = waitForVaadinElement(driver, By.id("username-field"));
    assertEquals(account.getUsername(), usernameField.getAttribute("value"));

    emailField = waitForVaadinElement(driver, By.id("email-field"));
    assertEquals(account.getEmail(), emailField.getAttribute("value"));
  }
}

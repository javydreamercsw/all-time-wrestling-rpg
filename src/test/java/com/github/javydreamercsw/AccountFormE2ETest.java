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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.service.AccountService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.test.context.support.WithMockUser;

public class AccountFormE2ETest extends AbstractE2ETest {
  @Autowired
  @Qualifier("managementAccountService") private AccountService accountService;

  @Test
  @WithMockUser(roles = {"ADMIN"})
  public void testDeleteAccount() {
    // Create an account to delete
    Account account =
        accountService.createAccount(
            "delete_me", "ValidPassword1!", "delete_me@atw.com", RoleName.VIEWER);

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
}

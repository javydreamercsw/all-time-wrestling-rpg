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
package com.github.javydreamercsw.management.ui.view.account;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class AccountListDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testCaptureAccountListScreenshot() {
    navigateTo("account-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.id("account-grid"));

    documentFeature(
        "Admin",
        "Account Management",
        "Admins manage all user accounts from this view. Each row shows the account's"
            + " username, email address, assigned roles, last login timestamp, and"
            + " enabled/disabled status. Use Edit to change roles or reset credentials,"
            + " and Delete to permanently remove an account.",
        "account-list-overview");
  }

  @Test
  void testRecordAccountListWalkthrough() {
    setVideoInfo("Admin", "Account Management Walkthrough", "account-list-walkthrough");

    navigateTo("account-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.id("account-grid"));

    captureCaption(
        "Account List — every user account in the system is shown here. Columns include"
            + " username, email, roles (Admin / Booker / Player / Viewer), last login,"
            + " and enabled status. Only admins can access this view.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 200)");
    sleep(800);

    captureCaption(
        "Each row has Edit and Delete buttons. Edit lets you change the account's email,"
            + " roles, and password. Disabling an account prevents login without deleting"
            + " its data — useful for suspended users.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(600);

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    WebElement newAccountButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("new-account-button")));
    captureCaption(
        "Click New Account to create a user. Assign one or more roles — Admin for full"
            + " system access, Booker for show and segment management, Player for campaign"
            + " and league play, or Viewer for read-only access.",
        4500);
    clickElement(newAccountButton);

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    sleep(600);
    captureCaption(
        "New Account dialog — username and email are required; password is set on first"
            + " login or by the admin. Roles can be combined: a Booker who also plays"
            + " in leagues needs both the Booker and Player roles.",
        4000);

    WebElement cancelButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//vaadin-button[text()='Cancel']")));
    clickElement(cancelButton);

    sleep(1500);
  }
}

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

import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginE2ETest extends AbstractE2ETest {

  @Test
  public void testSuccessfulLogin() {
    // The setup method in AbstractE2ETest already logs in.
    // This test will log out and log back in to verify.
    logout();
    login("admin", "admin123");

    // Verify redirection to the main view
    waitForVaadinElement(driver, By.tagName("vaadin-app-layout"));
    assertTrue(
        Objects.requireNonNull(driver.getCurrentUrl()).endsWith(getContextPath() + "/?continue"),
        driver.getCurrentUrl());
  }

  @Test
  public void testFailedLogin() {
    logout();
    driver.get("http://localhost:" + serverPort + getContextPath() + "/login");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    WebElement loginFormHost =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("vaadinLoginFormWrapper")));

    WebElement usernameField = loginFormHost.findElement(By.id("vaadinLoginUsername"));
    usernameField.sendKeys("invalid");

    WebElement passwordField = loginFormHost.findElement(By.id("vaadinLoginPassword"));
    passwordField.sendKeys("invalid");

    WebElement signInButton =
        loginFormHost.findElement(By.cssSelector("vaadin-button[slot='submit']"));
    clickElement(signInButton);
    assertTrue(
        Objects.requireNonNull(driver.getCurrentUrl()).endsWith(getContextPath() + "/login?error"),
        driver.getCurrentUrl());
  }

  @Test
  public void testLogout() { // The user is already logged in by the setup method.
    logout();

    // Verify redirection to the login view
    waitForVaadinElement(driver, By.id("vaadinLoginFormWrapper"));
    assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains("/login"));
  }
}

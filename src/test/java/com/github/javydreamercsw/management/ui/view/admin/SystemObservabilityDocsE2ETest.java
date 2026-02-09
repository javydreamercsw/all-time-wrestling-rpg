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
package com.github.javydreamercsw.management.ui.view.admin;

import com.github.javydreamercsw.AbstractE2ETest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@Slf4j
class SystemObservabilityDocsE2ETest extends AbstractE2ETest {

  @Test
  void testCaptureObservabilityDashboard() {
    // Navigate to Admin View
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();

    // Click System Observability button
    clickElement(By.id("observability-dashboard"));
    waitForVaadinClientToLoad();

    // 1. Capture Performance Tab (Default)
    documentFeature(
        "Admin",
        "System Performance",
        "Monitor AI response times and resource usage.",
        "admin-observability-performance");

    // 2. Capture Cache Tab
    waitForVaadinElement(driver, By.id("cache-tab"));
    clickElement(By.id("cache-tab"));
    waitForVaadinClientToLoad();
    documentFeature(
        "Admin",
        "Cache Management",
        "Monitor and manage application caches.",
        "admin-observability-cache");

    // 3. Capture Database Tab
    waitForVaadinElement(driver, By.id("database-tab"));
    clickElement(By.id("database-tab"));
    waitForVaadinClientToLoad();
    documentFeature(
        "Admin",
        "Database Management",
        "Monitor database statistics and optimize performance.",
        "admin-observability-database");

    // 4. Capture System Pulse Tab
    waitForVaadinElement(driver, By.id("system-pulse"));
    clickElement(By.id("system-pulse"));
    waitForVaadinClientToLoad();
    documentFeature(
        "Admin",
        "System Pulse",
        "Real-time health status of the application.",
        "admin-observability-pulse");
  }
}

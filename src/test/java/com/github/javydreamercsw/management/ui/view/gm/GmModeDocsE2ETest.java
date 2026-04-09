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
package com.github.javydreamercsw.management.ui.view.gm;

import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.Test;

class GmModeDocsE2ETest extends AbstractDocsE2ETest {

  @Test
  void testCaptureGmDashboard() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/gm-dashboard");
    waitForVaadinClientToLoad();

    documentFeature(
        "General Manager",
        "GM Dashboard",
        "The GM Dashboard provides a high-level overview of brand performance, including total"
            + " budget, roster health, and locker room morale. GMs can track financial trends and"
            + " identify exhausted or unhappy wrestlers at a glance.",
        "admin-gm-dashboard");
  }

  @Test
  void testCaptureContractManagement() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/contracts");
    waitForVaadinClientToLoad();

    documentFeature(
        "General Manager",
        "Contract Management",
        "View and manage active wrestler contracts. Salaries are dynamically tied to fan count,"
            + " forcing GMs to balance the cost of superstars with the development of new talent.",
        "admin-contract-management");
  }
}

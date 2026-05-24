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

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class GmModeDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testCaptureGmDashboard() {
    navigateTo("gm-dashboard");

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
    navigateTo("contracts");

    documentFeature(
        "General Manager",
        "Contract Management",
        "View and manage active wrestler contracts. Salaries are dynamically tied to fan count,"
            + " forcing GMs to balance the cost of superstars with the development of new talent.",
        "admin-contract-management");
  }

  @Test
  void testRecordGmDashboardWalkthrough() {
    setVideoInfo("General Manager", "GM Dashboard Walkthrough", "gm-dashboard-walkthrough");

    navigateTo("gm-dashboard");
    waitForVaadinClientToLoad();

    captureCaption(
        "GM Dashboard — the Total Budget card at the top shows the promotion's financial"
            + " health. Use the League selector to switch between brands and see per-league"
            + " budget and roster data without leaving the page.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 250)");
    sleep(900);

    captureCaption(
        "Roster Health & Morale — each wrestler's current stamina, morale, and fan count"
            + " are shown in the grid. Cells highlight in warning color when stamina drops"
            + " below 40% or morale falls under 50%, flagging wrestlers who need a break.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(600);

    captureCaption(
        "Exhausted or unhappy wrestlers underperform in matches and lose fans over time."
            + " Review this dashboard before booking shows to identify who needs rest"
            + " and who is primed for a featured main-event spot.",
        4000);

    sleep(1500);
  }

  @Test
  void testRecordContractManagementWalkthrough() {
    setVideoInfo(
        "General Manager", "Contract Management Walkthrough", "contract-management-walkthrough");

    navigateTo("contracts");
    waitForVaadinClientToLoad();

    captureCaption(
        "Contract Management — lists every active wrestler contract for the selected league."
            + " Each row shows the wrestler's name, salary per show, duration in weeks,"
            + " contract type (Draft or Free Agent), and whether the contract is still active.",
        5000);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 200)");
    sleep(800);

    captureCaption(
        "Salary scales with a wrestler's fan base — Main Eventers command higher pay than"
            + " Rookies. Monitor total payroll here to avoid overspending on roster depth"
            + " at the expense of developing the next headliner.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(1500);
  }
}

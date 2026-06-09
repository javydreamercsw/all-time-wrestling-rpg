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
package com.github.javydreamercsw.management.ui.view.universe;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.service.universe.InviteService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class MemberOnboardingDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private DataInitializer dataInitializer;
  @Autowired private UniverseService universeService;
  @Autowired private InviteService inviteService;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testCaptureInviteManagementDialog() {
    navigateTo("universe-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    // Click the first 'Invites' button in the grid
    WebElement inviteButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[contains(., 'Invites')]")));
    clickElement(inviteButton);

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    sleep(600);

    documentFeature(
        "Admin",
        "Managing Universe Invites",
        "Admins can generate invite links to share with prospective members. "
            + "Targeted links are single-use with a 7-day expiry; Community links are "
            + "multi-use with no expiry. Active links are listed below with options to revoke.",
        "universe-invite-management");
  }

  @Test
  void testCaptureJoinRequestsDialog() {
    navigateTo("universe-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    // Click the first 'Requests' button in the grid
    WebElement requestsButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[contains(., 'Requests')]")));
    clickElement(requestsButton);

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    sleep(400);

    documentFeature(
        "Admin",
        "Reviewing Join Requests",
        "Pending membership requests appear here. Approve to add the user as a member, "
            + "Reject to decline while allowing them to re-request, or Block to permanently "
            + "prevent future requests from that account.",
        "universe-join-requests");
  }

  @Test
  void testCaptureJoinLandingPage() {
    // Navigate directly to the join page with a fake token to capture the error state
    // (token validation will fail, showing the error layout — which is still a valid doc
    // screenshot)
    navigateTo("join/preview-token");
    waitForVaadinClientToLoad();
    sleep(500);

    documentFeature(
        "Community",
        "Joining a Universe",
        "When you follow a universe invite link, you are taken to this page. "
            + "Existing users can log in and request membership; new users can create a "
            + "free account and submit their request in one step.",
        "universe-join-page");
  }

  @Test
  void testRecordMemberOnboardingWalkthrough() {
    setVideoInfo("Admin", "Inviting Members to a Universe", "universe-member-onboarding");

    navigateTo("universe-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));

    captureCaption(
        "Universe List — each universe has Invites and Requests buttons for managing membership. "
            + "Click Invites to generate shareable links for prospective members.",
        4000);

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    WebElement inviteButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-button[contains(., 'Invites')]")));
    clickElement(inviteButton);
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog")));
    sleep(500);

    captureCaption(
        "Invite Management — choose Targeted (single-use, 7-day expiry) for a specific person "
            + "or Community (unlimited uses) for a public invite link. "
            + "Click Generate Link to create the invite.",
        5000);

    // Generate a link
    WebElement generateBtn =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("generate-invite-button")));
    clickElement(generateBtn);
    sleep(800);

    captureCaption(
        "The generated link appears here and can be copied to the clipboard. "
            + "Share it via Discord, email, or any messaging platform. "
            + "Active links are listed below — revoke any link at any time.",
        5000);

    // Close the dialog
    WebElement closeBtn =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-dialog//vaadin-button[text()='Close']")));
    clickElement(closeBtn);
    sleep(500);

    captureCaption(
        "When someone follows the link and submits a request, click Requests to review it. "
            + "Approve to add them as a member, Reject to decline, or Block to prevent "
            + "future requests from that account.",
        4500);
  }
}

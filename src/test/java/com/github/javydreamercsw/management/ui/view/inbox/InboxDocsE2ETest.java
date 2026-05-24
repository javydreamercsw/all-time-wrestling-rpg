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
package com.github.javydreamercsw.management.ui.view.inbox;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
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
class InboxDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private InboxEventType championshipChange;
  @Autowired private InboxEventType rivalryHeatChange;
  @Autowired private InboxEventType wrestlerInjuryObtained;
  @Autowired private InboxEventType achievementUnlocked;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DataInitializer dataInitializer;

  private Wrestler wrestler;

  @BeforeEach
  void setup() {
    inboxRepository.deleteAll();
    dataInitializer.init();

    wrestler =
        wrestlerRepository.findAll().stream()
            .filter(w -> Boolean.TRUE.equals(w.getIsPlayer()))
            .findFirst()
            .orElseGet(
                () -> {
                  Wrestler w =
                      Wrestler.builder()
                          .name("Docs Wrestler")
                          .startingHealth(100)
                          .startingStamina(100)
                          .isPlayer(true)
                          .active(true)
                          .build();
                  return wrestlerService.save(w);
                });
  }

  @Test
  void testCaptureInboxOverview() {
    // Seed a variety of event types so the screenshot shows a realistic inbox
    seedInboxItem(
        championshipChange,
        "World Title changed hands at SuperShow — new champion crowned after a gruelling"
            + " 30-minute iron man match.",
        wrestler);

    seedInboxItem(
        rivalryHeatChange,
        "Rivalry heat increased: Steel vs. Thunder — post-match brawl spills into the"
            + " crowd, boosting heat to 340.",
        wrestler);

    seedInboxItem(
        wrestlerInjuryObtained,
        "Injury sustained: Thunder suffered a sprained knee during the main event."
            + " Expected recovery: 2 shows.",
        wrestler);

    seedInboxItem(
        achievementUnlocked,
        "Achievement unlocked: 'On a Roll' — 5 consecutive victories earn a bonus"
            + " fan multiplier for the next 3 shows.",
        wrestler);

    navigateTo("inbox");
    waitForVaadinClientToLoad();
    waitForGridToPopulate("inbox-grid");

    documentFeature(
        "Player Dashboard",
        "Inbox — Event Feed",
        "The Inbox surfaces every automated event that affects your wrestlers — championship"
            + " changes, injury reports, rivalry heat shifts, and achievement unlocks."
            + " Filter by event type or wrestler target to focus on what matters most."
            + " Unread items are highlighted; select any row to mark it read or act on it.",
        "inbox-overview");
  }

  @Test
  void testRecordInboxWalkthrough() {
    setVideoInfo("Player Dashboard", "Inbox Walkthrough", "inbox-walkthrough");

    seedInboxItem(
        championshipChange,
        "World Title changed hands at SuperShow — new champion crowned.",
        wrestler);

    seedInboxItem(
        rivalryHeatChange,
        "Rivalry heat increased: Steel vs. Thunder — post-match brawl.",
        wrestler);

    seedInboxItem(
        wrestlerInjuryObtained,
        "Thunder sustained a sprained knee. Expected recovery: 2 shows.",
        wrestler);

    navigateTo("inbox");
    waitForVaadinClientToLoad();
    waitForGridToPopulate("inbox-grid");

    captureCaption(
        "Inbox — automated event feed for your promotion. Every significant game event"
            + " (championship changes, injuries, rivalry heat shifts, achievements)"
            + " generates an inbox item so nothing slips through unnoticed.",
        5000);

    // Scroll to show more rows
    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 250)");
    sleep(1000);
    captureCaption(
        "Each item shows the event type, a plain-English description, timestamp, and"
            + " the wrestlers or accounts targeted by the event. Unread items stand out"
            + " visually so you can prioritise your post-show review.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
    sleep(800);

    // Show the event-type filter
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement filterCombo =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("event-type-filter")));
    captureCaption(
        "Use the Event Type filter to narrow the feed — select 'Championship Change' to"
            + " see only title activity, or 'Wrestler Injury' to review health updates."
            + " The Target filter scopes the feed to a specific wrestler.",
        4500);

    selectFromVaadinComboBox(filterCombo, "Championship Change");
    waitForVaadinClientToLoad();
    sleep(800);
    captureCaption(
        "Filtered to Championship Change events only — the feed now shows just the"
            + " title activity for quick review. Clear the filter to return to the full"
            + " event stream.",
        4000);

    sleep(1500);
  }

  private void seedInboxItem(
      final InboxEventType eventType, final String description, final Wrestler target) {
    InboxItem item = new InboxItem();
    item.setEventType(eventType);
    item.setDescription(description);
    item.setRead(false);
    if (target != null && target.getId() != null) {
      item.addTarget(target.getId().toString(), InboxItemTarget.TargetType.WRESTLER);
    }
    inboxRepository.saveAndFlush(item);
  }
}

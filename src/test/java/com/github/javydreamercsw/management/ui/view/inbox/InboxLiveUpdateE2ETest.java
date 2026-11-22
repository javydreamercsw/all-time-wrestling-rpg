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

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateBroadcaster;
import com.github.javydreamercsw.management.event.inbox.InboxUpdateEvent;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that inbox updates reach an open client without user interaction: the navbar badge
 * updates and an open inbox grid shows the new item via websocket push (or the poll safety net).
 */
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InboxLiveUpdateE2ETest extends AbstractE2ETest {

  @Autowired private InboxEventType fanAdjudication;
  @Autowired private InboxUpdateBroadcaster inboxUpdateBroadcaster;
  @Autowired private AccountRepository accountRepository;

  @BeforeEach
  public void setUp() {
    inboxRepository.deleteAll();
  }

  @Test
  void badgeAndGridUpdateWithoutInteraction() throws Exception {
    Account admin = accountRepository.findByUsername("admin").orElseThrow();

    navigateTo("inbox");
    waitForVaadinToLoad();
    waitForGridToPopulate("inbox-grid");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    // Badge must be absent while there is nothing unread.
    Assertions.assertTrue(
        driver.findElements(By.id("inbox-unread-badge")).isEmpty(),
        "Badge should be hidden with zero unread items");

    // Create the inbox item from a thread outside any Vaadin request, then broadcast. This
    // mirrors production: a background adjudication job creates the item and pushes the update.
    CompletableFuture<Void> creation =
        CompletableFuture.runAsync(
            () -> {
              InboxItem item = new InboxItem();
              item.setDescription("Live Update Probe");
              item.setEventType(fanAdjudication);
              item.setRead(false);
              item.addTarget(admin.getId().toString(), InboxItemTarget.TargetType.ACCOUNT);
              inboxRepository.saveAndFlush(item);
              inboxUpdateBroadcaster.broadcast(new InboxUpdateEvent(this));
            });
    creation.get(10, TimeUnit.SECONDS);

    // Badge appears with zero clicks; the push (or the 60s poll safety net) delivers it.
    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("inbox-unread-badge")));

    // The open grid shows the new row without any navigation or reload.
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.tagName("vaadin-grid"), "Live Update Probe"));
  }
}

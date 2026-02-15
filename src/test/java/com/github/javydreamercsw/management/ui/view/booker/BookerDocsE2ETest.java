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
package com.github.javydreamercsw.management.ui.view.booker;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

class BookerDocsE2ETest extends AbstractE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;

  @Test
  void testCaptureShowPlanningView() {
    // 1. Setup wrestlers for the roster
    if (wrestlerRepository.count() < 5) {
      for (int i = 1; i <= 5; i++) {
        Account wrestlerAccount = new Account();
        String uniqueId = i + "_" + System.currentTimeMillis();
        wrestlerAccount.setUsername("wrestler" + uniqueId);
        wrestlerAccount.setEmail("wrestler" + uniqueId + "@example.com");
        wrestlerAccount.setPassword("password");
        wrestlerAccount = accountRepository.save(wrestlerAccount);

        Wrestler w =
            Wrestler.builder()
                .name("Roster Wrestler " + i)
                .startingHealth(100)
                .startingStamina(100)
                .account(wrestlerAccount)
                .active(true)
                .build();
        wrestlerRepository.save(w);
      }
    }

    // 2. Setup a show to plan
    ShowType weekly =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  st.setExpectedMatches(5);
                  st.setExpectedPromos(2);
                  return showTypeRepository.save(st);
                });

    Show show = new Show();
    show.setName("Show to Plan");
    show.setShowDate(LocalDate.now().plusDays(7));
    show.setDescription("Planning Documentation Show");
    show.setType(weekly);
    show = showRepository.save(show);

    // 3. Navigate to Show Planning
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-planning");
    waitForVaadinClientToLoad();

    // 4. Select the show
    selectFromVaadinComboBox("select-show-combo-box", "Show to Plan");

    // 5. Load Context
    clickButtonByText("Load Context");
    waitForText("Show Planning Context");

    // 6. Propose Segments (Triggers Mock AI)
    clickButtonByText("Propose Segments");

    // Wait for the grid to be populated (Mock AI has 1-3s delay)
    waitForVaadinElement(driver, By.id("proposed-segments-grid"));
    // Wait a bit more for AI simulation
    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
    }

    documentFeature(
        "Booker",
        "Show Planning",
        "The Show Planning interface allows you to book matches and segments for an upcoming show."
            + " Drag and drop wrestlers to create matches, set stipulations, and define segment"
            + " types.",
        "booker-show-planning");
  }

  @Test
  void testCaptureShowListView() {
    // Navigate to Show List (History)
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");
    waitForVaadinClientToLoad();
    waitForText("Shows");

    documentFeature(
        "Booker",
        "Show History",
        "View a complete history of all booked shows. Click on any show to view its detailed"
            + " results, ratings, and match history.",
        "booker-show-history");
  }

  @Test
  void testCaptureMatchNarrationView() {
    // 1. Setup participants
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler w1 =
        wrestlerRepository
            .findByName("Roster Wrestler 1")
            .orElseGet(
                () -> {
                  Account a = new Account();
                  a.setUsername("w1_" + System.currentTimeMillis());
                  a.setEmail("w1_" + System.currentTimeMillis() + "@example.com");
                  a.setPassword("password");
                  a = accountRepository.save(a);
                  return wrestlerRepository.save(
                      Wrestler.builder()
                          .name("Roster Wrestler 1")
                          .startingHealth(100)
                          .startingStamina(100)
                          .account(a)
                          .active(true)
                          .build());
                });

    Wrestler w2 =
        wrestlerRepository
            .findByName("Roster Wrestler 2")
            .orElseGet(
                () -> {
                  Account a = new Account();
                  a.setUsername("w2_" + System.currentTimeMillis());
                  a.setEmail("w2_" + System.currentTimeMillis() + "@example.com");
                  a.setPassword("password");
                  a = accountRepository.save(a);
                  return wrestlerRepository.save(
                      Wrestler.builder()
                          .name("Roster Wrestler 2")
                          .startingHealth(100)
                          .startingStamina(100)
                          .account(a)
                          .active(true)
                          .build());
                });

    // 2. Setup a dummy show and segment
    Show show = new Show();
    show.setName("Docs Weekly Show");
    show.setShowDate(LocalDate.now());
    show.setDescription("Documentation Show");
    show.setType(showTypeRepository.findByName("Weekly").get());
    show = showRepository.save(show);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentTypeRepository.findByName("One on One").get());
    segment.addParticipant(w1);
    segment.addParticipant(w2);
    segment = segmentRepository.save(segment);

    // 3. Navigate to Match View
    driver.get("http://localhost:" + serverPort + getContextPath() + "/match/" + segment.getId());
    waitForVaadinClientToLoad();

    // 4. Trigger AI Narration
    clickButtonByText("Generate with Feedback");

    // Wait for narration to appear (Mock AI has 1-3s delay)
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
    }

    // 5. Verify & Capture
    waitForText("Match Details");
    waitForText("Match Narration");

    documentFeature(
        "Booker",
        "Match Narration",
        "Bring your matches to life with AI-generated or manual narration. The Story Director uses"
            + " match participants, rules, and outcomes to weave a compelling narrative of the"
            + " action. The new structured transcript format separates objective action from"
            + " character-driven commentary.",
        "booker-match-narration");
  }

  @Test
  void testCaptureDynamicCommentaryView() {
    // 1. Setup participants
    Wrestler w1 =
        wrestlerRepository
            .findByName("Roster Wrestler 1")
            .orElseGet(
                () -> {
                  Account a = new Account();
                  a.setUsername("w1_" + System.currentTimeMillis());
                  a.setEmail("w1_" + System.currentTimeMillis() + "@example.com");
                  a.setPassword("password");
                  a = accountRepository.save(a);
                  return wrestlerRepository.save(
                      Wrestler.builder()
                          .name("Roster Wrestler 1")
                          .startingHealth(100)
                          .startingStamina(100)
                          .account(a)
                          .active(true)
                          .build());
                });

    Wrestler w2 =
        wrestlerRepository
            .findByName("Roster Wrestler 2")
            .orElseGet(
                () -> {
                  Account a = new Account();
                  a.setUsername("w2_" + System.currentTimeMillis());
                  a.setEmail("w2_" + System.currentTimeMillis() + "@example.com");
                  a.setPassword("password");
                  a = accountRepository.save(a);
                  return wrestlerRepository.save(
                      Wrestler.builder()
                          .name("Roster Wrestler 2")
                          .startingHealth(100)
                          .startingStamina(100)
                          .account(a)
                          .active(true)
                          .build());
                });

    // 2. Setup a dummy show and segment
    Show show = new Show();
    show.setName("Commentary Docs Show");
    show.setShowDate(LocalDate.now());
    show.setDescription("Documentation Show for Commentary");
    show.setType(showTypeRepository.findByName("Weekly").get());
    show = showRepository.save(show);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentTypeRepository.findByName("One on One").get());
    segment.addParticipant(w1);
    segment.addParticipant(w2);
    segment = segmentRepository.save(segment);

    // 3. Navigate to Match View
    driver.get("http://localhost:" + serverPort + getContextPath() + "/match/" + segment.getId());
    waitForVaadinClientToLoad();

    // 4. Trigger AI Narration
    clickButtonByText("Generate with Feedback");

    // Wait for narration to appear (Mock AI has 1-3s delay)
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
    }

    // 5. Verify & Capture
    waitForText("Match Details");
    waitForText("Match Narration");

    documentFeature(
        "AI Features",
        "Dynamic Commentator Personas",
        "Experience the match through the eyes of unique commentator teams. Each commentator"
            + " brings their own alignment, style, and catchphrases to the broadcast, creating"
            + " an immersive 'sports-entertainment' feel. Face commentators cheer the heroes,"
            + " while Heel commentators favor the rule-breakers.",
        "ai-dynamic-commentary");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

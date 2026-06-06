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
package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Video walkthrough of the show card validation system: the booker proposes segments, encounters a
 * MUST_BOOK advisory for an unbooked rivalry, acknowledges it, and approves the card.
 */
@Tag("video")
class ShowPlanningValidationVideoDocsE2ETest extends AbstractE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private RivalryRepository rivalryRepository;

  private Show testShow;

  @BeforeEach
  void setupVideoTestData() {
    ShowType weekly =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  st.setExpectedMatches(3);
                  st.setExpectedPromos(2);
                  return showTypeRepository.save(st);
                });

    testShow = new Show();
    testShow.setName("Video Docs Show — Card Validation");
    testShow.setShowDate(LocalDate.now().plusDays(7));
    testShow.setDescription("Show used for card validation walkthrough video");
    testShow.setType(weekly);
    testShow.setUniverse(defaultUniverse);
    testShow = showRepository.save(testShow);

    Wrestler w1 = ensureWrestler("Video Feud Alpha");
    Wrestler w2 = ensureWrestler("Video Feud Beta");

    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(w1);
    rivalry.setWrestler2(w2);
    rivalry.setUniverse(defaultUniverse);
    rivalry.setHeat(20);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(Instant.now());
    rivalryRepository.saveAndFlush(rivalry);
  }

  @Test
  void videoShowCardValidationWorkflow() {
    setVideoInfo(
        "Booker",
        "Show Card Validation Walkthrough",
        "booker-show-planning-card-validation-walkthrough");

    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-planning/" + testShow.getId());
    waitForVaadinClientToLoad();

    waitForVaadinElement(driver, By.id("show-planning-context-area"));
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
        .until(d -> !d.findElement(By.id("show-planning-context-area")).getText().isEmpty());

    captureCaption(
        "The Show Planning view gives bookers a full picture of the roster, active rivalries,"
            + " and championship picture before building the card. The AI proposes a segment"
            + " list that the booker can review, edit, and approve.",
        4000);

    // Propose segments
    clickElement(waitForVaadinElement(driver, By.id("propose-segments-button")));
    waitForVaadinElement(driver, By.id("proposed-segments-grid"));
    sleep(4000);

    captureCaption(
        "After clicking Propose Segments, the AI builds a draft card. Each row shows the"
            + " segment type, participants, titles at stake, and match stipulation. The booker"
            + " can edit individual segments or add new ones before approving.",
        4500);

    // Click Approve — MUST_BOOK warning dialog should appear
    clickElement(waitForVaadinElement(driver, By.id("approve-segments-button")));
    sleep(1500);

    captureCaption(
        "When rivalries with heat ≥ 10 are missing from the proposed card, an advisory dialog"
            + " lists each unbooked feud sorted by heat. This is a warning — not a hard error."
            + " The booker can go back and add the matches, or acknowledge and approve anyway."
            + " Unbooked rivalries carry over automatically to future shows.",
        5000);

    // Confirm the warning and let the approval proceed
    By confirmButtonBy =
        By.cssSelector("vaadin-confirm-dialog-overlay vaadin-button[slot='confirm-button']");
    try {
      clickElement(waitForVaadinElement(driver, confirmButtonBy));
    } catch (Exception ignored) {
      // Dialog may not have appeared if no unbooked rivalries — that is also a valid state
    }
    sleep(2000);

    captureCaption(
        "After the booker confirms, the segments are saved to the show. Each approved segment"
            + " enters the adjudication pipeline — the match engine resolves outcomes, updates"
            + " wrestler stats, and the AI generates narration ready for broadcast.",
        4500);
  }

  private Wrestler ensureWrestler(final String name) {
    return wrestlerRepository
        .findByName(name)
        .orElseGet(
            () -> {
              Account account = new Account();
              String uid = name.replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
              account.setUsername(uid);
              account.setEmail(uid + "@example.com");
              account.setPassword("password");
              account = accountRepository.save(account);
              return wrestlerRepository.save(
                  Wrestler.builder()
                      .name(name)
                      .startingHealth(100)
                      .startingStamina(100)
                      .account(account)
                      .active(true)
                      .build());
            });
  }
}

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
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Documents the show card validation UI: the MUST_BOOK confirmation dialog (unbooked rivalries) and
 * the STIPULATION_REQUIRED error dialog (booked rivalry without a match rule).
 */
class ShowPlanningValidationDocsE2ETest extends AbstractE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private RivalryRepository rivalryRepository;

  private Show testShow;
  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  void setupValidationTestData() {
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
    testShow.setName("Validation Docs Show");
    testShow.setShowDate(LocalDate.now().plusDays(7));
    testShow.setDescription("Show for documenting card validation");
    testShow.setType(weekly);
    testShow.setUniverse(defaultUniverse);
    testShow = showRepository.save(testShow);

    wrestler1 = ensureWrestler("Docs Wrestler Alpha");
    wrestler2 = ensureWrestler("Docs Wrestler Beta");
  }

  @Test
  void documentMustBookWarningDialog() {
    // Create a rivalry with heat >= 10 so it triggers a MUST_BOOK warning.
    // The AI will propose segments that do NOT cover this rivalry, causing the
    // confirmation dialog to appear when the booker clicks "Approve".
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setUniverse(defaultUniverse);
    rivalry.setHeat(25);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(Instant.now());
    rivalryRepository.saveAndFlush(rivalry);

    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-planning/" + testShow.getId());
    waitForVaadinClientToLoad();

    waitForVaadinElement(driver, By.id("show-planning-context-area"));
    waitForNonEmptyText(By.id("show-planning-context-area"));

    // Propose segments via Mock AI (won't cover the rivalry above)
    clickElement(waitForVaadinElement(driver, By.id("propose-segments-button")));
    waitForVaadinElement(driver, By.id("proposed-segments-grid"));
    pause(4000);

    documentFeature(
        "Booker",
        "Show Planning — Proposed Card",
        "After the AI proposes segments, the booker reviews the card before approving."
            + " Rivalries with heat ≥ 10 that are not on the card will trigger an advisory"
            + " when the Approve button is clicked.",
        "booker-show-planning-proposed-card");

    // Click approve — expect the MUST_BOOK confirmation dialog
    clickElement(waitForVaadinElement(driver, By.id("approve-segments-button")));
    pause(1500);

    documentFeature(
        "Booker",
        "Show Planning — Unbooked Rivalries Warning",
        "When rivalries with heat ≥ 10 are not on the proposed card, a confirmation dialog"
            + " lists each unbooked feud. The booker can go back and add them, or acknowledge"
            + " and approve anyway — unbooked rivalries carry over to future shows.",
        "booker-show-planning-must-book-warning");
  }

  @Test
  void documentStipulationRequiredErrorDialog() {
    // Create a rivalry with heat >= 30 so that if it IS on the card without a stipulation,
    // a hard error fires. The Mock AI books matches without rules, so a rivalry the AI
    // picks up and books without a match rule will trigger this path.
    //
    // Strategy: create rivalry with heat=35, let AI propose segments covering these wrestlers.
    // Since the Mock AI doesn't set rules, the STIPULATION_REQUIRED error fires on approve.
    Rivalry hotRivalry = new Rivalry();
    hotRivalry.setWrestler1(wrestler1);
    hotRivalry.setWrestler2(wrestler2);
    hotRivalry.setUniverse(defaultUniverse);
    hotRivalry.setHeat(35);
    hotRivalry.setIsActive(true);
    hotRivalry.setStartedDate(Instant.now());
    rivalryRepository.saveAndFlush(hotRivalry);

    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-planning/" + testShow.getId());
    waitForVaadinClientToLoad();

    waitForVaadinElement(driver, By.id("show-planning-context-area"));
    waitForNonEmptyText(By.id("show-planning-context-area"));

    // Load context and propose segments
    clickElement(waitForVaadinElement(driver, By.id("propose-segments-button")));
    waitForVaadinElement(driver, By.id("proposed-segments-grid"));
    pause(4000);

    // Click approve — if AI booked the rivalry without rules, the error dialog fires
    clickElement(waitForVaadinElement(driver, By.id("approve-segments-button")));
    pause(1500);

    documentFeature(
        "Booker",
        "Show Planning — Stipulation Required Error",
        "When a rivalry with heat ≥ 30 is on the card but has no match rule (stipulation),"
            + " approval is blocked. The error dialog lists each affected rivalry."
            + " Edit the segment and add a rule such as Steel Cage or Last Man Standing"
            + " before approving.",
        "booker-show-planning-stipulation-error");
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

  private void waitForNonEmptyText(final By locator) {
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
        .until(d -> !d.findElement(locator).getText().isEmpty());
  }

  private void pause(final long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

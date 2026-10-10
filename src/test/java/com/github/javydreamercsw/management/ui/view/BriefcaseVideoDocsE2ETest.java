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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import java.time.LocalDate;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Video walkthrough of the Money in the Bank-style briefcase cash-in flow (ATW-312z): the profile's
 * Briefcase panel → the Cash In dialog → championship and show picks → the booked title match. A
 * multi-step dialog flow a static screenshot cannot convey.
 */
@Slf4j
@Tag("video")
class BriefcaseVideoDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private TournamentService tournamentService;
  @Autowired private UniverseService universeService;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  void testRecordBriefcaseCashInWalkthrough() throws InterruptedException {
    setVideoInfo(
        "Game Mechanics", "Briefcase Cash-In Walkthrough", "mechanic-briefcase-cash-in-video");

    Universe universe = universeService.findAll().iterator().next();
    Tournament tournament =
        tournamentService.createTournament(
            "Video Time Vault",
            "SINGLE_ELIMINATION",
            universe,
            null,
            LocalDate.now(),
            null,
            null,
            null,
            null,
            null,
            false,
            true);
    tournamentService.seedAuto(tournament, 4, universe.getId());
    tournament =
        tournamentService
            .findByIdWithDetails(tournament.getId())
            .orElseThrow(() -> new IllegalStateException("Tournament vanished"));
    tournament = tournamentService.startTournament(tournament);

    // Same single-transaction manual completion as BriefcaseDocsE2ETest: advance → record →
    // advance again flips COMPLETE and grants the briefcase to the winner.
    final Long tournamentId = tournament.getId();
    transactionTemplate.executeWithoutResult(
        tx -> {
          Tournament inTx =
              tournamentService
                  .findByIdWithDetails(tournamentId)
                  .orElseThrow(() -> new IllegalStateException("Tournament vanished"));
          inTx.getRounds().get(0).getMatches().stream()
              .sorted(Comparator.comparingLong(m -> m.getId() == null ? 0 : m.getId()))
              .forEach(match -> tournamentService.recordMatchResult(match, match.getEntrant1()));
          var finalMatches = tournamentService.advanceToNextRound(inTx);
          finalMatches.forEach(
              match -> tournamentService.recordMatchResult(match, match.getEntrant1()));
          tournamentService.advanceToNextRound(inTx);
        });
    Wrestler winner =
        tournamentService
            .findByIdWithDetails(tournament.getId())
            .orElseThrow()
            .getEntries()
            .stream()
            .filter(e -> e.getStatus() == TournamentEntryStatus.WINNER)
            .map(e -> e.getWrestler())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No winner recorded"));

    // Step 1: the profile's Briefcase panel.
    navigateTo("wrestler-profile/" + winner.getId());
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//*[contains(., 'Career Dashboard')]"));
    WebElement panel =
        driver.findElement(
            By.xpath("//vaadin-accordion-panel[.//text()[contains(., 'Briefcase')]]"));
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].scrollIntoView({block: 'center'});", panel);
    Thread.sleep(800);
    captureCaption(
        "Every wrestler profile now carries a Briefcase panel, right after Championships."
            + " Mukundi-style: win the annual Time Vault and your profile shows the held case"
            + " with its cashable-until date — the prize is visible without leaving the profile.",
        4500);

    // Step 2: open the Briefcase panel (Match Logs is the default-open accordion panel) and
    // click Cash In inside it.
    WebElement panelSummary =
        driver.findElement(
            By.xpath(
                "//vaadin-accordion-panel[.//text()[contains(., 'Briefcase')]]"
                    + "//*[local-name()='summary']/*"));
    clickElement(panelSummary);
    Thread.sleep(800);

    WebElement cashInBtn =
        driver.findElement(
            By.xpath(
                "//vaadin-accordion-panel[.//text()[contains(., 'Briefcase')]]"
                    + "//vaadin-button[normalize-space(.)='Cash In']"));
    clickElement(cashInBtn);
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-dialog-overlay"));
    Thread.sleep(800);
    captureCaption(
        "Clicking Cash In opens the shared dialog: pick the championship to challenge — any"
            + " active title with a reigning champion, division-checked — and the show to book"
            + " on. Remember: the briefcase is spent when the match is booked, win or lose.",
        5000);

    // Step 3: the dialog's two pickers and the warning line.
    ((JavascriptExecutor) driver)
        .executeScript(
            "var overlay = document.querySelector('vaadin-dialog-overlay');"
                + "overlay.scrollIntoView({block: 'center'});");
    Thread.sleep(800);
    captureCaption(
        "The tier gate is deliberately skipped — the briefcase IS the credential. Choose your"
            + " moment wisely: cashing in against a champion on a big show is the classic"
            + " Money in the Bank play, but a failed cash-in still costs you the case.",
        4500);

    // Close the dialog without cashing in — this walkthrough documents the flow; the actual
    // booking needs a reigning champion, which the docs universe may not have mid-capture.
    ((JavascriptExecutor) driver)
        .executeScript(
            "var overlay ="
                + " document.querySelector('vaadin-dialog-overlay');overlay.dispatchEvent(new"
                + " CustomEvent('opened-changed', {detail:{opened:false}}));");
    Thread.sleep(500);
  }
}

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
package com.github.javydreamercsw.management.ui.view.match;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class InteractivePromoE2ETest extends AbstractE2ETest {

  @Autowired private SegmentRepository segmentRepository;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private MatchFulfillmentRepository matchFulfillmentRepository;
  @Autowired private LeagueRepository leagueRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Segment promoSegment;

  @BeforeEach
  public void setupData() throws IOException {
    cleanupLeagues();
    segmentRepository.deleteAll();
    showRepository.deleteAll();

    // Create Player & Account
    Wrestler playerWrestler = createTestWrestler("Promo King");
    wrestlerService.save(playerWrestler);

    if (accountRepository.findByUsername("player1").isEmpty()) {
      Account playerAccount = new Account();
      playerAccount.setUsername("player1");
      playerAccount.setEmail("player1@example.com");
      playerAccount.setPassword(passwordEncoder.encode("password123"));
      playerAccount.setActiveWrestlerId(playerWrestler.getId());
      playerAccount.setRoles(Set.of(roleRepository.findByName(RoleName.PLAYER).orElseThrow()));
      playerAccount = accountRepository.saveAndFlush(playerAccount);

      playerWrestler.setAccount(playerAccount);
      wrestlerService.save(playerWrestler);
    } else {
      Account playerAccount = accountRepository.findByUsername("player1").get();
      playerAccount.setPassword(passwordEncoder.encode("password123"));
      playerAccount.setActiveWrestlerId(playerWrestler.getId());
      playerAccount = accountRepository.saveAndFlush(playerAccount);

      playerWrestler.setAccount(playerAccount);
      wrestlerService.save(playerWrestler);
    }

    // Create Opponent
    Wrestler opponent = createTestWrestler("The Silent One");
    opponent.setDescription("A wrestler who rarely speaks but when he does, it matters.");
    wrestlerService.save(opponent);

    // Create League
    League league = new League();
    league.setName("Promo League");
    Account admin =
        accountRepository
            .findByUsername("admin")
            .orElseGet(
                () -> {
                  Account a = new Account();
                  a.setUsername("admin");
                  a.setPassword(passwordEncoder.encode("admin123"));
                  a.setEmail("admin@example.com");
                  a.setRoles(Set.of(roleRepository.findByName(RoleName.ADMIN).orElseThrow()));
                  return accountRepository.saveAndFlush(a);
                });
    league.setCommissioner(admin);
    leagueRepository.saveAndFlush(league);

    // Create Show
    ShowType weeklyShow =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  return showTypeRepository.save(st);
                });

    Show show = new Show();
    show.setName("Mic Drop");
    show.setType(weeklyShow);
    show = showRepository.saveAndFlush(show);

    // Create Promo Segment
    // Ensure "Promo" type exists or fallback to creating it
    SegmentType promoType =
        segmentTypeRepository
            .findByName("Promo")
            .orElseGet(
                () -> {
                  SegmentType st = new SegmentType();
                  st.setName("Promo");
                  return segmentTypeRepository.save(st);
                });

    promoSegment = new Segment();
    promoSegment.setSegmentType(promoType);
    promoSegment.addParticipant(playerWrestler);
    promoSegment.addParticipant(opponent);
    promoSegment.setShow(show);
    promoSegment = segmentRepository.saveAndFlush(promoSegment);

    // Create MatchFulfillment to make it accessible in player view if needed,
    // though MatchView logic checks segment participants too.
    MatchFulfillment fulfillment = new MatchFulfillment();
    fulfillment.setSegment(promoSegment);
    fulfillment.setLeague(league);
    matchFulfillmentRepository.saveAndFlush(fulfillment);
  }

  @Test
  void testInteractivePromoFlow() {
    logout();
    login("player1", "password123");

    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/match/" + promoSegment.getId());

    // 1. Verify we are in the Promo Interface
    waitForVaadinElement(driver, By.xpath("//h3[text()='Interactive Promo']"));

    // 2. Locate Message Input
    WebElement messageInput = waitForVaadinElement(driver, By.tagName("vaadin-message-input"));

    // 3. Send a message
    String promoText = "I am the greatest of all time!";

    // Vaadin Message Input shadow DOM handling
    WebElement inputField =
        (WebElement)
            ((JavascriptExecutor) driver)
                .executeScript(
                    "return arguments[0].shadowRoot.querySelector('textarea')", messageInput);

    if (inputField == null) {
      // Fallback for newer versions where it might be different, or direct interaction
      messageInput.sendKeys(promoText);
      messageInput.sendKeys(Keys.ENTER);
    } else {
      inputField.sendKeys(promoText);
      WebElement sendButton =
          (WebElement)
              ((JavascriptExecutor) driver)
                  .executeScript(
                      "return arguments[0].shadowRoot.querySelector('vaadin-button')",
                      messageInput);
      assert sendButton != null;
      clickElement(sendButton);
    }

    // 4. Verify Player Message appears
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(d -> Objects.requireNonNull(d.getPageSource()).contains(promoText));

    // 5. Verify AI Retort appears (The Silent One)
    // This depends on LocalAI generation speed. Giving it ample time.
    WebDriverWait aiWait = new WebDriverWait(driver, Duration.ofSeconds(60));
    aiWait.until(d -> Objects.requireNonNull(d.getPageSource()).contains("The Silent One"));

    // 6. Verify Transcript Saved (Backend check)
    // We can check the text area value or query the DB
    WebElement narrationArea = driver.findElement(By.id("narration-area"));
    // Wait for narration area to be updated
    aiWait.until(
        d -> {
          String value = narrationArea.getAttribute("value");
          return value != null && value.contains(promoText) && value.contains("The Silent One");
        });

    String finalTranscript = narrationArea.getAttribute("value");
    assert finalTranscript != null;
    Assertions.assertTrue(finalTranscript.contains(promoText));

    // Verify DB persistence
    assert promoSegment.getId() != null;
    Segment updatedSegment = segmentRepository.findById(promoSegment.getId()).orElseThrow();
    Assertions.assertTrue(updatedSegment.getNarration().contains(promoText));
  }
}

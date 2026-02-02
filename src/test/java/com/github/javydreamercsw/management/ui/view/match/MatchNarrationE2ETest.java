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
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class MatchNarrationE2ETest extends AbstractE2ETest {

  @Autowired private SegmentRepository segmentRepository;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private MatchFulfillmentRepository matchFulfillmentRepository;
  @Autowired private LeagueRepository leagueRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Wrestler playerWrestler;
  private Segment matchSegment;

  @BeforeEach
  public void setupData() throws IOException {
    // Clean up
    cleanupLeagues();
    segmentRepository.deleteAll();
    showRepository.deleteAll();

    // Create Player & Account
    playerWrestler = createTestWrestler("Player One");
    wrestlerService.save(playerWrestler);

    if (accountRepository.findByUsername("player1").isEmpty()) {
      Account playerAccount = new Account();
      playerAccount.setUsername("player1");
      playerAccount.setEmail("player1@example.com");
      playerAccount.setPassword(passwordEncoder.encode("password123"));
      playerAccount.setActiveWrestlerId(playerWrestler.getId());
      playerAccount.setRoles(Set.of(roleRepository.findByName(RoleName.PLAYER).orElseThrow()));
      accountRepository.saveAndFlush(playerAccount);
    } else {
      // Update existing account to ensure known state
      Account playerAccount = accountRepository.findByUsername("player1").get();
      playerAccount.setPassword(passwordEncoder.encode("password123"));
      playerAccount.setActiveWrestlerId(playerWrestler.getId());
      accountRepository.saveAndFlush(playerAccount);
    }

    // Create Opponent
    Wrestler opponent = createTestWrestler("Opponent");
    wrestlerService.save(opponent);

    // Create League
    League league = new League();
    league.setName("Test League");
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
    show.setName("Test Show");
    show.setDescription("Test Show Description");
    show.setType(weeklyShow);
    show = showRepository.saveAndFlush(show);

    // Create Segment
    SegmentType matchType = segmentTypeRepository.findByName("One on One").orElseThrow();
    matchSegment = new Segment();
    matchSegment.setSegmentType(matchType);
    matchSegment.addParticipant(playerWrestler);
    matchSegment.addParticipant(opponent);
    matchSegment.setShow(show);
    matchSegment = segmentRepository.saveAndFlush(matchSegment);

    // Create MatchFulfillment
    MatchFulfillment fulfillment = new MatchFulfillment();
    fulfillment.setSegment(matchSegment);
    fulfillment.setLeague(league);
    matchFulfillmentRepository.saveAndFlush(fulfillment);
  }

  @Test
  void testPlayerCanSeeFeedbackInLeagueMatch() {
    // Logout admin (default) and login as player
    logout();
    login("player1", "password123");

    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/match/" + matchSegment.getId());
    waitForVaadinElement(driver, By.id("match-view-" + matchSegment.getId()));

    // Assert Feedback Area is visible
    Assertions.assertTrue(
        driver.findElement(By.id("feedback-area")).isDisplayed(),
        "Feedback area should be visible to participant player");

    // Assert Generate Button is visible
    Assertions.assertTrue(
        driver.findElement(By.id("ai-generate-narration-button")).isDisplayed(),
        "Generate button should be visible to participant player");
  }
}

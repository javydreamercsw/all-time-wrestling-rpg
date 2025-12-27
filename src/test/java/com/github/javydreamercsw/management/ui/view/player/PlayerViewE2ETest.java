/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class PlayerViewE2ETest extends AbstractE2ETest {

  @Autowired
  @Qualifier("managementAccountService") private AccountService accountService;

  @Autowired private WrestlerService wrestlerService;
  @Autowired private ShowService showService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private SegmentService segmentService;
  @Autowired private RivalryService rivalryService;
  @Autowired private InboxService inboxService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private InboxRepository inboxRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private TitleRepository titleChampionRepository;

  @BeforeEach
  public void setupTest() {
    // It's better to delete in order to avoid constraint violations.
    segmentRepository.deleteAll();
    rivalryRepository.deleteAll();
    inboxRepository.deleteAll();
    showRepository.deleteAll();
    titleChampionRepository.deleteAll();
    wrestlerRepository.deleteAll();
  }

  @Test
  public void testPlayerViewLoads() {
    // Get player account
    Account playerAccount = accountService.findByUsername("player").get();
    assertNotNull(playerAccount);

    // Create a wrestler and assign it to the player
    Wrestler wrestler =
        Wrestler.builder()
            .name("Test Wrestler")
            .isPlayer(true)
            .gender(Gender.MALE)
            .tier(WrestlerTier.MIDCARDER)
            .account(playerAccount)
            .build();
    wrestlerService.save(wrestler);

    Wrestler wrestler2 =
        Wrestler.builder()
            .name("Test Wrestler Opponent")
            .isPlayer(true)
            .gender(Gender.MALE)
            .tier(WrestlerTier.MIDCARDER)
            .build();
    wrestlerService.save(wrestler2);

    Wrestler opponent =
        Wrestler.builder()
            .name("Opponent")
            .isPlayer(false)
            .gender(Gender.MALE)
            .tier(WrestlerTier.MIDCARDER)
            .build();
    wrestlerService.save(opponent);

    rivalryService.createRivalry(wrestler.getId(), opponent.getId(), "Test Rivalry");

    inboxService.addInboxItem(wrestler, "Test Message");

    SegmentType oneOnOne = segmentTypeRepository.findByName("One on One").get();

    // Create a show
    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Show Description");
    show.setShowDate(LocalDate.now().plusDays(1));
    show.setType(showTypeService.findAll().get(0));
    showService.save(show);

    // Create a segment with the wrestler
    Segment segment = segmentService.createSegment(show, oneOnOne, Instant.now(), new HashSet<>());
    segment.addParticipant(wrestler);
    segment.addParticipant(wrestler2);
    segment.setWinners(List.of(wrestler));
    segmentService.updateSegment(segment);

    // Create another show
    Show show2 = new Show();
    show2.setName("Test Show 2");
    show2.setDescription("Test Show 2 Description");
    show2.setShowDate(LocalDate.now().plusDays(2));
    show2.setType(showTypeService.findAll().get(0));
    showService.save(show2);

    // Create another segment with the wrestler
    Segment segment2 =
        segmentService.createSegment(show2, oneOnOne, Instant.now(), new HashSet<>());
    segment2.addParticipant(wrestler);
    segment2.addParticipant(wrestler2);
    segment2.setWinners(List.of(wrestler2));
    segmentService.updateSegment(segment2);

    login("player", "player123");

    // Navigate to the PlayerView
    assertDoesNotThrow(
        () -> {
          driver.get("http://localhost:" + serverPort + getContextPath() + "/player");
          assertEquals(
              "Name: " + wrestler.getName(),
              waitForVaadinElement(driver, By.id("wrestler-name")).getText());
          assertEquals(
              "Tier: " + wrestler.getTier(),
              waitForVaadinElement(driver, By.id("wrestler-tier")).getText());
          assertEquals(
              "Bumps: " + wrestler.getBumps(),
              waitForVaadinElement(driver, By.id("wrestler-bumps")).getText());
          assertEquals("Wins: 1", waitForVaadinElement(driver, By.id("wrestler-wins")).getText());
          assertEquals(
              "Losses: 1", waitForVaadinElement(driver, By.id("wrestler-losses")).getText());

          // Check that the grids have the correct number of rows
          assertEquals(2, getGridRows("upcoming-matches-grid").size());
          assertEquals(1, getGridRows("active-rivalries-grid").size());
          assertEquals(1, getGridRows("inbox-grid").size());

          // Check the content of the grids
          assertGridContains("upcoming-matches-grid", "Test Show");
          assertGridContains("upcoming-matches-grid", "Test Show 2");
          assertGridContains("active-rivalries-grid", "Opponent");
          assertGridContains("inbox-grid", "Test Message");
        });
  }

  @Test
  public void testGoToMatchNavigation() {
    // Get player account
    Account playerAccount = accountService.findByUsername("player").get();
    assertNotNull(playerAccount);

    // Create a wrestler and assign it to the player
    Wrestler wrestler =
        Wrestler.builder()
            .name("Test Wrestler")
            .isPlayer(true)
            .gender(Gender.MALE)
            .tier(WrestlerTier.MIDCARDER)
            .account(playerAccount)
            .build();
    wrestlerService.save(wrestler);
    // Create a show
    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Show Description");
    show.setShowDate(LocalDate.now().plusDays(1));
    show.setType(showTypeService.findAll().get(0));
    showService.save(show);

    // Create a segment with the wrestler
    Segment segment =
        segmentService.createSegment(
            show, segmentTypeService.findAll().get(0), Instant.now(), new HashSet<>());
    segment.addParticipant(wrestler);
    segmentService.updateSegment(segment);

    login("player", "player123");

    // Navigate to the PlayerView
    driver.get("http://localhost:" + serverPort + getContextPath() + "/player");

    waitForVaadinToLoad(driver);

    // Click the "Go to Match" button
    clickElement(By.id("go-to-match-" + segment.getId()));

    // Verify that we navigated to the correct match view
    assertDoesNotThrow(
        () -> {
          waitForVaadinElement(driver, By.id("match-view-" + segment.getId()));
          assertEquals(
              "Show: " + show.getName(),
              waitForVaadinElement(driver, By.id("show-name")).getText());
          assertEquals(
              "Match Type: " + segment.getSegmentType().getName(),
              waitForVaadinElement(driver, By.id("match-type")).getText());
        });
  }
}

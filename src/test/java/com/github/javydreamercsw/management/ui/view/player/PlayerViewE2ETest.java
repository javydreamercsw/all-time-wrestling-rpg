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
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
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
import org.junit.jupiter.api.Test;
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

    // Create another show
    Show show2 = new Show();
    show2.setName("Test Show 2");
    show2.setDescription("Test Show 2 Description");
    show2.setShowDate(LocalDate.now().plusDays(2));
    show2.setType(showTypeService.findAll().get(0));
    showService.save(show2);

    // Create another segment with the wrestler
    Segment segment2 =
        segmentService.createSegment(
            show2, segmentTypeService.findAll().get(0), Instant.now(), new HashSet<>());
    segment2.addParticipant(wrestler);
    segmentService.updateSegment(segment2);

    login("player", "player123");

    // Navigate to the PlayerView
    assertDoesNotThrow(
        () -> {
          driver.get("http://localhost:" + serverPort + getContextPath() + "/player");
          assertEquals(2, getGridRows("upcoming-matches-grid").size());
          assertEquals(1, getGridRows("active-rivalries-grid").size());
          assertEquals(1, getGridRows("inbox-grid").size());
        });
  }
}

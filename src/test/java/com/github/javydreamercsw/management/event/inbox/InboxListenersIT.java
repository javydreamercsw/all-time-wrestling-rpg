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
package com.github.javydreamercsw.management.event.inbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class InboxListenersIT extends ManagementIntegrationTest {

  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private WrestlerService wrestlerService;

  @Autowired
  private com.github.javydreamercsw.management.domain.universe.UniverseRepository
      universeRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository
      wrestlerStateRepository;

  @Autowired
  @Qualifier("fanAdjudication") private InboxEventType fanAdjudication;

  @Autowired
  @Qualifier("rivalryHeatChange") private InboxEventType rivalryHeatChange;

  @Autowired
  @Qualifier("adjudicationCompleted") private InboxEventType adjudicationCompleted;

  @Autowired
  @Qualifier("championshipChange") private InboxEventType championshipChange;

  @Autowired
  @Qualifier("championshipDefended") private InboxEventType championshipDefended;

  @Autowired
  @Qualifier("factionHeatChange") private InboxEventType factionHeatChange;

  @Autowired
  @Qualifier("feudHeatChange") private InboxEventType feudHeatChange;

  @Autowired
  @Qualifier("feudResolved") private InboxEventType feudResolved;

  @Autowired
  @Qualifier("rivalryCompleted") private InboxEventType rivalryCompleted;

  @Autowired
  @Qualifier("rivalryContinues") private InboxEventType rivalryContinues;

  @Autowired
  @Qualifier("segmentsApproved") private InboxEventType segmentsApproved;

  @Autowired
  @Qualifier("wrestlerInjuryHealed") private InboxEventType wrestlerInjuryHealed;

  @Autowired
  @Qualifier("wrestlerInjuryObtained") private InboxEventType wrestlerInjuryObtained;

  @Autowired
  @Qualifier("wrestlerBump") private InboxEventType wrestlerBump;

  @Autowired
  @Qualifier("wrestlerBumpHealed") private InboxEventType wrestlerBumpHealed;

  @MockitoBean private InboxService inboxService;
  @MockitoBean private InboxUpdateBroadcaster inboxUpdateBroadcaster;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private WrestlerState state1;
  private Show show;
  private Title title;
  private Faction faction1;
  private Injury injury;
  private Universe universe;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
    // Reset mock before each test
    Mockito.reset(inboxService);

    universe =
        universeRepository
            .findById(1L)
            .orElseGet(
                () ->
                    universeRepository.save(
                        Universe.builder().id(1L).name("Default Universe").build()));

    wrestler1 = Wrestler.builder().name("Wrestler A").build();
    wrestler1 = wrestlerRepository.save(wrestler1);
    state1 = wrestlerService.getOrCreateState(wrestler1.getId(), universe.getId());
    state1.setFans(1000L);
    wrestlerStateRepository.saveAndFlush(state1);

    wrestler2 = Wrestler.builder().name("Wrestler B").build();
    wrestler2 = wrestlerRepository.save(wrestler2);
    WrestlerState state2 = wrestlerService.getOrCreateState(wrestler2.getId(), universe.getId());
    state2.setFans(500L);
    wrestlerStateRepository.saveAndFlush(state2);

    show = new Show();
    show.setId(100L);
    show.setName("Test Show");
    show.setUniverse(universe);

    title = new Title();
    title.setId(200L);
    title.setName("World Championship");
    title.setUniverse(universe);

    faction1 = Faction.builder().id(1L).name("Faction Alpha").universe(universe).build();

    injury = new Injury();
    injury.setId(500L);
    injury.setDescription("Broken Arm");
    injury.setName("Arm Injury");
    injury.setUniverse(universe);
  }

  @Test
  void testFanAwardedEventCreatesInboxItem() {
    Long fanChange = 200L;
    FanAwardedEvent event = new FanAwardedEvent(this, state1, fanChange);
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItemTarget.TargetType> typeCaptor =
        ArgumentCaptor.forClass(InboxItemTarget.TargetType.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            messageCaptor.capture(),
            referenceIdCaptor.capture(),
            typeCaptor.capture());

    assertEquals(fanAdjudication, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Wrestler %s gained %d fans. New total: %d",
            wrestler1.getName(), fanChange, state1.getFans());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.WRESTLER, typeCaptor.getValue());
  }

  @Test
  void testAdjudicationCompletedEventCreatesInboxItem() {
    AdjudicationCompletedEvent event = new AdjudicationCompletedEvent(this, show);
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItemTarget.TargetType> typeCaptor =
        ArgumentCaptor.forClass(InboxItemTarget.TargetType.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            messageCaptor.capture(),
            referenceIdCaptor.capture(),
            typeCaptor.capture());

    assertEquals(adjudicationCompleted, eventTypeCaptor.getValue());
    String expectedMessage = String.format("Adjudication completed for show: %s", show.getName());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(show.getId());
    assertEquals(show.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.SHOW, typeCaptor.getValue());
  }

  @Test
  void testChampionshipChangeEventCreatesInboxItem() {
    ChampionshipChangeEvent event =
        new ChampionshipChangeEvent(this, title, List.of(wrestler1), List.of(wrestler2));
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItemTarget.TargetType> typeCaptor =
        ArgumentCaptor.forClass(InboxItemTarget.TargetType.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            messageCaptor.capture(),
            referenceIdCaptor.capture(),
            typeCaptor.capture());

    assertEquals(championshipChange, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Championship change for title ID %d. New champions: %s (formerly %s)",
            title.getId(), wrestler1.getName(), wrestler2.getName());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(title.getId());
    assertEquals(title.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.TITLE, typeCaptor.getValue());
  }

  @Test
  void testWrestlerBumpEventCreatesInboxItem() {
    state1.setBumps(5);
    WrestlerBumpEvent event = new WrestlerBumpEvent(this, state1);
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItemTarget.TargetType> typeCaptor =
        ArgumentCaptor.forClass(InboxItemTarget.TargetType.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            messageCaptor.capture(),
            referenceIdCaptor.capture(),
            typeCaptor.capture());

    assertEquals(wrestlerBump, eventTypeCaptor.getValue());

    String expectedMessage =
        String.format(
            "Wrestler %s received a bump. Total bumps: %d", wrestler1.getName(), state1.getBumps());

    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.WRESTLER, typeCaptor.getValue());
  }

  @Test
  void testWrestlerBumpHealedEventCreatesInboxItem() {
    state1.setBumps(0);
    WrestlerBumpHealedEvent event = new WrestlerBumpHealedEvent(this, state1);
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItemTarget.TargetType> typeCaptor =
        ArgumentCaptor.forClass(InboxItemTarget.TargetType.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            messageCaptor.capture(),
            referenceIdCaptor.capture(),
            typeCaptor.capture());

    assertEquals(wrestlerBumpHealed, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Wrestler %s's bumps have healed. New total: %d",
            wrestler1.getName(), state1.getBumps());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.WRESTLER, typeCaptor.getValue());
  }

  @Test
  void testWrestlerInjuryEventCreatesInboxItem() {
    WrestlerInjuryEvent event = new WrestlerInjuryEvent(this, state1, injury);
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItemTarget.TargetType> typeCaptor =
        ArgumentCaptor.forClass(InboxItemTarget.TargetType.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            messageCaptor.capture(),
            referenceIdCaptor.capture(),
            typeCaptor.capture());

    assertEquals(wrestlerInjuryObtained, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Wrestler %s sustained a %s injury.", wrestler1.getName(), injury.getDescription());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.WRESTLER, typeCaptor.getValue());
  }

  @Test
  void testWrestlerInjuryHealedEventCreatesInboxItem() {
    WrestlerInjuryHealedEvent event = new WrestlerInjuryHealedEvent(this, state1, injury);
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItemTarget.TargetType> typeCaptor =
        ArgumentCaptor.forClass(InboxItemTarget.TargetType.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            messageCaptor.capture(),
            referenceIdCaptor.capture(),
            typeCaptor.capture());

    assertEquals(wrestlerInjuryHealed, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Wrestler %s's %s injury has healed.", wrestler1.getName(), injury.getDescription());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.WRESTLER, typeCaptor.getValue());
  }
}

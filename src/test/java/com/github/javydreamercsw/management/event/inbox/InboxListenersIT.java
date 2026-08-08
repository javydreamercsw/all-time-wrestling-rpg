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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
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
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;

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
  private Injury injury;
  private Universe universe;
  private Account ownerAccount;

  @BeforeEach
  public void setUp() {
    clearAllRepositories();
    Mockito.reset(inboxService);

    // Stub the 5-arg (List-based) createInboxItem used by wrestler-event listeners
    when(inboxService.createInboxItem(any(), any(), any(), any(InboxItem.Urgency.class), anyList()))
        .thenReturn(new InboxItem());
    // Stub the 6-arg (single-target) createInboxItem used by show/title listeners
    when(inboxService.createInboxItem(
            any(), any(), any(), any(InboxItem.Urgency.class), any(), any()))
        .thenReturn(new InboxItem());
    when(inboxService.save(any())).thenReturn(new InboxItem());

    universe =
        universeRepository
            .findById(1L)
            .orElseGet(
                () -> universeRepository.save(Universe.builder().name("Default Universe").build()));

    ownerAccount = accountRepository.save(new Account("player1", "password1", "player1@test.com"));

    wrestler1 = Wrestler.builder().name("Wrestler A").account(ownerAccount).build();
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

    injury = new Injury();
    injury.setId(500L);
    injury.setDescription("Broken Arm");
    injury.setName("Arm Injury");
    injury.setUniverse(universe);
  }

  @Test
  void testFanAwardedEventCreatesInboxItemWithAccountTarget() {
    Long fanChange = 200L;
    FanAwardedEvent event = new FanAwardedEvent(this, state1, fanChange);
    eventPublisher.publishEvent(event);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<InboxService.TargetInfo>> targetsCaptor =
        ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItem.Urgency> urgencyCaptor =
        ArgumentCaptor.forClass(InboxItem.Urgency.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            subjectCaptor.capture(),
            messageCaptor.capture(),
            urgencyCaptor.capture(),
            targetsCaptor.capture());

    assertEquals(fanAdjudication, eventTypeCaptor.getValue());
    String expectedMessage =
        "Wrestler %s gained %d fans. New total: %d"
            .formatted(wrestler1.getName(), fanChange, state1.getFans());
    assertEquals(expectedMessage, messageCaptor.getValue());

    List<InboxService.TargetInfo> targets = targetsCaptor.getValue();
    assertContainsWrestlerTarget(targets, wrestler1);
    assertContainsAccountTarget(targets, ownerAccount);
  }

  @Test
  void testAdjudicationCompletedEventCreatesInboxItem() {
    AdjudicationCompletedEvent event = new AdjudicationCompletedEvent(this, show);
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItem.Urgency> urgencyCaptor =
        ArgumentCaptor.forClass(InboxItem.Urgency.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItemTarget.TargetType> typeCaptor =
        ArgumentCaptor.forClass(InboxItemTarget.TargetType.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            subjectCaptor.capture(),
            messageCaptor.capture(),
            urgencyCaptor.capture(),
            referenceIdCaptor.capture(),
            typeCaptor.capture());

    assertEquals(adjudicationCompleted, eventTypeCaptor.getValue());
    String expectedMessage = "Adjudication completed for show: %s".formatted(show.getName());
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
    ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItem.Urgency> urgencyCaptor =
        ArgumentCaptor.forClass(InboxItem.Urgency.class);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<InboxItemTarget.TargetType> typeCaptor =
        ArgumentCaptor.forClass(InboxItemTarget.TargetType.class);

    // Multiple listeners (e.g. AchievementInboxListener) may also fire on this event
    verify(inboxService, atLeastOnce())
        .createInboxItem(
            eventTypeCaptor.capture(),
            subjectCaptor.capture(),
            messageCaptor.capture(),
            urgencyCaptor.capture(),
            referenceIdCaptor.capture(),
            typeCaptor.capture());

    // Find the invocation from ChampionshipChangeInboxListener specifically
    int idx = eventTypeCaptor.getAllValues().indexOf(championshipChange);
    assertTrue(
        idx >= 0, "Expected createInboxItem to be called with championshipChange event type");
    String expectedMessage =
        "Championship change for title ID %d. New champions: %s (formerly %s)"
            .formatted(title.getId(), wrestler1.getName(), wrestler2.getName());
    assertEquals(expectedMessage, messageCaptor.getAllValues().get(idx));
    Assertions.assertNotNull(title.getId());
    assertEquals(title.getId().toString(), referenceIdCaptor.getAllValues().get(idx));
    assertEquals(InboxItemTarget.TargetType.TITLE, typeCaptor.getAllValues().get(idx));
  }

  @Test
  void testWrestlerBumpEventCreatesInboxItemWithAccountTarget() {
    state1.setBumps(5);
    WrestlerBumpEvent event =
        new WrestlerBumpEvent(
            this,
            state1,
            com.github.javydreamercsw.management.domain.show.segment.rule.BumpSource.MANUAL);
    eventPublisher.publishEvent(event);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<InboxService.TargetInfo>> targetsCaptor =
        ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            any(),
            messageCaptor.capture(),
            any(InboxItem.Urgency.class),
            targetsCaptor.capture());

    assertEquals(wrestlerBump, eventTypeCaptor.getValue());
    String expectedMessage =
        "Wrestler %s received a bump from manual assignment. Total bumps: %d"
            .formatted(wrestler1.getName(), state1.getBumps());
    assertEquals(expectedMessage, messageCaptor.getValue());

    List<InboxService.TargetInfo> targets = targetsCaptor.getValue();
    assertContainsWrestlerTarget(targets, wrestler1);
    assertContainsAccountTarget(targets, ownerAccount);
  }

  @Test
  void testWrestlerBumpHealedEventCreatesInboxItemWithAccountTarget() {
    state1.setBumps(0);
    WrestlerBumpHealedEvent event = new WrestlerBumpHealedEvent(this, state1);
    eventPublisher.publishEvent(event);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<InboxService.TargetInfo>> targetsCaptor =
        ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            any(),
            messageCaptor.capture(),
            any(InboxItem.Urgency.class),
            targetsCaptor.capture());

    assertEquals(wrestlerBumpHealed, eventTypeCaptor.getValue());
    String expectedMessage =
        "Wrestler %s's bumps have healed. New total: %d"
            .formatted(wrestler1.getName(), state1.getBumps());
    assertEquals(expectedMessage, messageCaptor.getValue());

    List<InboxService.TargetInfo> targets = targetsCaptor.getValue();
    assertContainsWrestlerTarget(targets, wrestler1);
    assertContainsAccountTarget(targets, ownerAccount);
  }

  @Test
  void testWrestlerInjuryEventCreatesInboxItemWithAccountTarget() {
    WrestlerInjuryEvent event = new WrestlerInjuryEvent(this, state1, injury);
    eventPublisher.publishEvent(event);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<InboxService.TargetInfo>> targetsCaptor =
        ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            any(),
            messageCaptor.capture(),
            any(InboxItem.Urgency.class),
            targetsCaptor.capture());

    assertEquals(wrestlerInjuryObtained, eventTypeCaptor.getValue());
    String expectedMessage =
        "Wrestler %s sustained a %s injury."
            .formatted(wrestler1.getName(), injury.getDescription());
    assertEquals(expectedMessage, messageCaptor.getValue());

    List<InboxService.TargetInfo> targets = targetsCaptor.getValue();
    assertContainsWrestlerTarget(targets, wrestler1);
    assertContainsAccountTarget(targets, ownerAccount);
  }

  @Test
  void testWrestlerInjuryHealedEventCreatesInboxItemWithAccountTarget() {
    WrestlerInjuryHealedEvent event = new WrestlerInjuryHealedEvent(this, state1, injury);
    eventPublisher.publishEvent(event);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<InboxService.TargetInfo>> targetsCaptor =
        ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(),
            any(),
            messageCaptor.capture(),
            any(InboxItem.Urgency.class),
            targetsCaptor.capture());

    assertEquals(wrestlerInjuryHealed, eventTypeCaptor.getValue());
    String expectedMessage =
        "Wrestler %s's %s injury has healed."
            .formatted(wrestler1.getName(), injury.getDescription());
    assertEquals(expectedMessage, messageCaptor.getValue());

    List<InboxService.TargetInfo> targets = targetsCaptor.getValue();
    assertContainsWrestlerTarget(targets, wrestler1);
    assertContainsAccountTarget(targets, ownerAccount);
  }

  @Test
  void wrestlerTargetsIncludesAccountTargetForOwnedWrestler() {
    List<InboxService.TargetInfo> targets = InboxService.wrestlerTargets(wrestler1);

    assertContainsWrestlerTarget(targets, wrestler1);
    assertContainsAccountTarget(targets, ownerAccount);
  }

  @Test
  void wrestlerTargetsOmitsAccountTargetForNpcWrestler() {
    List<InboxService.TargetInfo> targets = InboxService.wrestlerTargets(wrestler2);

    assertContainsWrestlerTarget(targets, wrestler2);
    assertFalse(
        targets.stream().anyMatch(t -> t.type() == InboxItemTarget.TargetType.ACCOUNT),
        "NPC wrestler (no account) must not produce an ACCOUNT target");
  }

  private static void assertContainsWrestlerTarget(
      List<InboxService.TargetInfo> targets, Wrestler wrestler) {
    assertTrue(
        targets.stream()
            .anyMatch(
                t ->
                    t.type() == InboxItemTarget.TargetType.WRESTLER
                        && t.targetId().equals(wrestler.getId().toString())),
        "Expected WRESTLER target for id " + wrestler.getId());
  }

  private static void assertContainsAccountTarget(
      List<InboxService.TargetInfo> targets, Account account) {
    assertTrue(
        targets.stream()
            .anyMatch(
                t ->
                    t.type() == InboxItemTarget.TargetType.ACCOUNT
                        && t.targetId().equals(account.getId().toString())),
        "Expected ACCOUNT target for id "
            + account.getId()
            + " — inbox items without an ACCOUNT target are invisible in the inbox view");
  }
}

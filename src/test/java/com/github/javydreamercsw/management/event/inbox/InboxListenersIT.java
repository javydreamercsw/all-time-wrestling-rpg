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

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import com.github.javydreamercsw.management.event.FactionHeatChangeEvent;
import com.github.javydreamercsw.management.event.FeudHeatChangeEvent;
import com.github.javydreamercsw.management.event.FeudResolvedEvent;
import com.github.javydreamercsw.management.event.HeatChangeEvent;
import com.github.javydreamercsw.management.event.RivalryCompletedEvent;
import com.github.javydreamercsw.management.event.RivalryContinuesEvent;
import com.github.javydreamercsw.management.event.SegmentsApprovedEvent;
import com.github.javydreamercsw.management.event.dto.FanAwardedEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerInjuryHealedEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class InboxListenersIT extends AbstractIntegrationTest {

  @Autowired private ApplicationEventPublisher eventPublisher;

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
  private Rivalry rivalry;
  private Show show;
  private Title title;
  private Faction faction1;
  private Faction faction2;
  private FactionRivalry factionRivalry;
  private MultiWrestlerFeud feud;
  private Injury injury;

  @BeforeEach
  void setUp() {
    // Reset mock before each test
    Mockito.reset(inboxService);

    wrestler1 = Wrestler.builder().id(1L).name("Wrestler A").fans(1000L).build();
    wrestler2 = Wrestler.builder().id(2L).name("Wrestler B").fans(500L).build();

    rivalry = new Rivalry();
    rivalry.setId(10L);
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(50);

    show = new Show();
    show.setId(100L);
    show.setName("Test Show");

    title = new Title();
    title.setId(200L);
    title.setName("World Championship");

    faction1 = Faction.builder().id(1L).name("Faction Alpha").build();
    faction2 = Faction.builder().id(2L).name("Faction Beta").build();

    factionRivalry = new FactionRivalry();
    factionRivalry.setId(300L);
    factionRivalry.setFaction1(faction1);
    factionRivalry.setFaction2(faction2);
    factionRivalry.setHeat(30);

    feud = new MultiWrestlerFeud();
    feud.setId(400L);
    feud.setName("Epic Feud");
    feud.setHeat(60);

    injury = new Injury();
    injury.setId(500L);
    injury.setDescription("Broken Arm");
    injury.setName("Arm Injury");
  }

  @Test
  void testFanAwardedEventCreatesInboxItem() {
    Long fanChange = 200L;
    FanAwardedEvent event = new FanAwardedEvent(this, wrestler1, fanChange);
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
            wrestler1.getName(), fanChange, wrestler1.getFans());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.WRESTLER, typeCaptor.getValue());
  }

  @Test
  void testHeatChangeEventCreatesInboxItem() {
    int oldHeat = 50;
    int heatChange = 10; // The actual change in heat
    int newHeat = oldHeat + heatChange; // The expected new total heat
    String reason = "From segment: Singles Match";

    // Update the rivalry object's heat before creating the event
    rivalry.setHeat(newHeat); // Set the rivalry's heat to the new total

    HeatChangeEvent event =
        new HeatChangeEvent(this, rivalry, oldHeat, reason, List.of(wrestler1, wrestler2));
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

    assertEquals(rivalryHeatChange, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Rivalry between %s and %s gained %d heat. New total: %d. Reason: %s",
            wrestler1.getName(),
            wrestler2.getName(),
            heatChange,
            newHeat,
            reason); // Use heatChange here
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(rivalry.getId());
    assertEquals(rivalry.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.RIVALRY, typeCaptor.getValue());
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
  void testChampionshipDefendedEventCreatesInboxItem() {
    ChampionshipDefendedEvent event =
        new ChampionshipDefendedEvent(this, title, List.of(wrestler1), List.of(wrestler2));
    eventPublisher.publishEvent(event);

    ArgumentCaptor<InboxEventType> eventTypeCaptor = ArgumentCaptor.forClass(InboxEventType.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<InboxService.TargetInfo>> targetsCaptor =
        ArgumentCaptor.forClass(List.class);

    verify(inboxService, times(1))
        .createInboxItem(
            eventTypeCaptor.capture(), messageCaptor.capture(), targetsCaptor.capture());

    assertEquals(championshipDefended, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Champion(s) %s successfully defended the %s title against %s!",
            wrestler1.getName(), title.getName(), wrestler2.getName());
    assertEquals(expectedMessage, messageCaptor.getValue());
    assertEquals(3, targetsCaptor.getValue().size());
    assert title.getId() != null;
    assertEquals(title.getId().toString(), targetsCaptor.getValue().getFirst().targetId());
    assertEquals(InboxItemTarget.TargetType.TITLE, targetsCaptor.getValue().getFirst().type());
  }

  @Test
  void testFactionHeatChangeEventCreatesInboxItem() {
    int oldHeat = 30;
    int heatChange = 5;
    factionRivalry.setHeat(oldHeat + heatChange);
    FactionHeatChangeEvent event =
        new FactionHeatChangeEvent(
            this, factionRivalry, oldHeat, "Promo", List.of(wrestler1, wrestler2));
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

    assertEquals(factionHeatChange, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Faction rivalry between %s and %s gained %d heat. New total: %d. Reason: %s",
            faction1.getName(), faction2.getName(), heatChange, factionRivalry.getHeat(), "Promo");
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(factionRivalry.getId());
    assertEquals(factionRivalry.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.FACTION, typeCaptor.getValue());
  }

  @Test
  void testFeudHeatChangeEventCreatesInboxItem() {
    int oldHeat = 60;
    int heatChange = 10;
    feud.setHeat(oldHeat + heatChange);
    FeudHeatChangeEvent event =
        new FeudHeatChangeEvent(this, feud, oldHeat, "Match Result", List.of(wrestler1, wrestler2));
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

    assertEquals(feudHeatChange, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format(
            "Feud '%s' involving %s, %s gained %d heat. New total: %d. Reason: %s",
            feud.getName(),
            wrestler1.getName(),
            wrestler2.getName(),
            heatChange,
            feud.getHeat(),
            "Match Result");
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(feud.getId());
    assertEquals(feud.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.FEUD, typeCaptor.getValue());
  }

  @Test
  void testFeudResolvedEventCreatesInboxItem() {
    FeudResolvedEvent event = new FeudResolvedEvent(this, feud);
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

    assertEquals(feudResolved, eventTypeCaptor.getValue());
    String expectedMessage = String.format("Feud '%s' has been resolved.", feud.getName());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(feud.getId());
    assertEquals(feud.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.FEUD, typeCaptor.getValue());
  }

  @Test
  void testRivalryCompletedEventCreatesInboxItem() {
    RivalryCompletedEvent event = new RivalryCompletedEvent(this, rivalry);
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

    assertEquals(rivalryCompleted, eventTypeCaptor.getValue());
    String expectedMessage =
        String.format("Rivalry '%s' has been completed.", rivalry.getDisplayName());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(rivalry.getId());
    assertEquals(rivalry.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.RIVALRY, typeCaptor.getValue());
  }

  @Test
  void testRivalryContinuesEventCreatesInboxItem() {
    RivalryContinuesEvent event = new RivalryContinuesEvent(this, rivalry);
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

    assertEquals(rivalryContinues, eventTypeCaptor.getValue());
    String expectedMessage = String.format("Rivalry '%s' continues.", rivalry.getDisplayName());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(rivalry.getId());
    assertEquals(rivalry.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.RIVALRY, typeCaptor.getValue());
  }

  @Test
  void testSegmentsApprovedEventCreatesInboxItem() {
    SegmentsApprovedEvent event = new SegmentsApprovedEvent(this, show);
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

    assertEquals(segmentsApproved, eventTypeCaptor.getValue());
    String expectedMessage = String.format("Segments approved for show: %s", show.getName());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(show.getId());
    assertEquals(show.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.SHOW, typeCaptor.getValue());
  }

  @Test
  void testWrestlerBumpEventCreatesInboxItem() {
    wrestler1.setBumps(5); // Set bumps before creating the event
    WrestlerBumpEvent event = new WrestlerBumpEvent(this, wrestler1); // Corrected constructor call
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
            "Wrestler %s received a bump. Total bumps: %d",
            wrestler1.getName(), wrestler1.getBumps());

    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.WRESTLER, typeCaptor.getValue());
  }

  @Test
  void testWrestlerBumpHealedEventCreatesInboxItem() {
    wrestler1.setBumps(0); // Assuming bumps are reset on healing for testing purposes
    WrestlerBumpHealedEvent event = new WrestlerBumpHealedEvent(this, wrestler1);
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
            wrestler1.getName(), wrestler1.getBumps());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.WRESTLER, typeCaptor.getValue());
  }

  @Test
  void testWrestlerInjuryEventCreatesInboxItem() {
    WrestlerInjuryEvent event = new WrestlerInjuryEvent(this, wrestler1, injury);
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
    wrestler1.setInjuries(
        List.of()); // Assuming injuries are cleared on healing for testing purposes
    WrestlerInjuryHealedEvent event = new WrestlerInjuryHealedEvent(this, wrestler1, injury);
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
            "Wrestler %s's %s injury has healed. New total: %d",
            wrestler1.getName(), injury.getDescription(), wrestler1.getInjuries().size());
    assertEquals(expectedMessage, messageCaptor.getValue());
    Assertions.assertNotNull(wrestler1.getId());
    assertEquals(wrestler1.getId().toString(), referenceIdCaptor.getValue());
    assertEquals(InboxItemTarget.TargetType.WRESTLER, typeCaptor.getValue());
  }
}

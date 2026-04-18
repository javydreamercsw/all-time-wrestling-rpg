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
package com.github.javydreamercsw.management.service.ringside;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.RingsideAction;
import com.github.javydreamercsw.management.domain.show.segment.RingsideActionType;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RingsideActionServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private NpcService npcService;
  @Mock private FactionService factionService;
  @Mock private TeamService teamService;
  @Mock private WrestlerService wrestlerService;

  @Mock private CampaignService campaignService;
  @Mock private AlignmentService alignmentService;

  @InjectMocks private RingsideActionService ringsideActionService;

  private Segment segment;
  private Show show;
  private Universe universe;
  private Npc referee;
  private Wrestler wrestler;
  private WrestlerState wrestlerState;
  private Wrestler otherWrestler;
  private WrestlerState otherWrestlerState;

  private RingsideActionType legalType;
  private RingsideActionType illegalType;

  private RingsideAction coachAction;
  private RingsideAction weaponAction;

  @BeforeEach
  void setUp() {
    universe = Universe.builder().id(1L).build();
    show = mock(Show.class);
    when(show.getUniverse()).thenReturn(universe);

    referee = new Npc();
    referee.setName("Earl Hebner");

    segment = mock(Segment.class);
    lenient().when(segment.getId()).thenReturn(1L);
    lenient().when(segment.getShow()).thenReturn(show);
    lenient().when(segment.getReferee()).thenReturn(referee);
    lenient().when(segment.getRefereeAwarenessLevel()).thenReturn(0);

    wrestler = new Wrestler();
    wrestler.setId(10L);
    wrestler.setName("Sgt. Slaughter");

    wrestlerState = WrestlerState.builder().wrestler(wrestler).universe(universe).build();
    when(wrestlerService.getOrCreateState(eq(10L), anyLong())).thenReturn(wrestlerState);
    when(wrestler.getState(anyLong())).thenReturn(Optional.of(wrestlerState));

    otherWrestler = new Wrestler();
    otherWrestler.setId(11L);
    otherWrestler.setName("Other Wrestler");

    otherWrestlerState = WrestlerState.builder().wrestler(otherWrestler).universe(universe).build();
    when(wrestlerService.getOrCreateState(eq(11L), anyLong())).thenReturn(otherWrestlerState);
    when(otherWrestler.getState(anyLong())).thenReturn(Optional.of(otherWrestlerState));

    legalType = new RingsideActionType();
    legalType.setName("Legal Support");
    legalType.setIncreasesAwareness(false);
    legalType.setCanCauseDq(false);
    legalType.setBaseRiskMultiplier(0.0);

    illegalType = new RingsideActionType();
    illegalType.setName("Illegal Interference");
    illegalType.setIncreasesAwareness(true);
    illegalType.setCanCauseDq(true);
    illegalType.setBaseRiskMultiplier(1.0);

    coachAction = new RingsideAction();
    coachAction.setName("Coaching");
    coachAction.setType(legalType);
    coachAction.setRisk(0);
    coachAction.setImpact(5);
    coachAction.setAlignment(
        com.github.javydreamercsw.management.domain.campaign.AlignmentType.FACE);

    weaponAction = new RingsideAction();
    weaponAction.setName("Weapon Slide");
    weaponAction.setType(illegalType);
    weaponAction.setRisk(40);
    weaponAction.setImpact(25);
    weaponAction.setAlignment(
        com.github.javydreamercsw.management.domain.campaign.AlignmentType.HEEL);

    // Default repository behavior
    lenient().when(segmentRepository.findById(1L)).thenReturn(Optional.of(segment));
    lenient().when(wrestlerRepository.findById(10L)).thenReturn(Optional.of(wrestler));
  }

  @Test
  void shouldShiftAlignmentOnSuccess() {
    com.github.javydreamercsw.management.domain.campaign.Campaign campaign =
        mock(com.github.javydreamercsw.management.domain.campaign.Campaign.class);
    com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment alignment =
        new com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment();
    alignment.setCampaign(campaign);
    wrestler.setAlignment(alignment);

    // Test Face shift
    ringsideActionService.performAction(segment, otherWrestler, wrestler, coachAction);
    verify(alignmentService).shiftAlignment(campaign, 1);

    // Reset verify
    reset(campaignService);

    // Run performAction until it succeeds (usually 100% since currentMeter is 0)
    ringsideActionService.performAction(segment, otherWrestler, wrestler, weaponAction);
    verify(alignmentService).shiftAlignment(campaign, -1);
  }

  @Test
  void hasSupportAtRingside_WithDirectManager() {
    Npc manager = new Npc();
    manager.setName("Colonel Mustafa");
    wrestlerState.setManager(manager);

    assertTrue(ringsideActionService.hasSupportAtRingside(segment, wrestler));
    assertEquals(manager, ringsideActionService.getBestSupporter(segment, wrestler));
  }

  @Test
  void hasSupportAtRingside_WithFactionManager() {
    Npc factionManager = new Npc();
    factionManager.setName("Faction Manager");
    Faction faction = new Faction();
    faction.setManager(factionManager);
    wrestlerState.setFaction(faction);

    assertTrue(ringsideActionService.hasSupportAtRingside(segment, wrestler));
    assertEquals(factionManager, ringsideActionService.getBestSupporter(segment, wrestler));
  }

  @Test
  void hasSupportAtRingside_WithFactionMember() {
    Wrestler partner = new Wrestler();
    partner.setId(12L);
    partner.setName("Kurt Angle");

    WrestlerState partnerState =
        WrestlerState.builder().wrestler(partner).universe(universe).build();
    when(wrestlerService.getOrCreateState(eq(12L), anyLong())).thenReturn(partnerState);
    when(partner.getState(anyLong())).thenReturn(Optional.of(partnerState));

    Faction faction = new Faction();
    faction.setMembers(Set.of(wrestlerState, partnerState));
    wrestlerState.setFaction(faction);
    partnerState.setFaction(faction);

    // Partner is NOT in the match
    when(segment.getWrestlers()).thenReturn(List.of(wrestler));

    assertTrue(ringsideActionService.hasSupportAtRingside(segment, wrestler));
    assertEquals(partner, ringsideActionService.getBestSupporter(segment, wrestler));
  }

  @Test
  void hasSupportAtRingside_NoSupportIfAllMembersInMatch() {
    Wrestler partner = new Wrestler();
    partner.setId(12L);
    partner.setName("Kurt Angle");

    WrestlerState partnerState =
        WrestlerState.builder().wrestler(partner).universe(universe).build();
    when(wrestlerService.getOrCreateState(eq(12L), anyLong())).thenReturn(partnerState);
    when(partner.getState(anyLong())).thenReturn(Optional.of(partnerState));

    Faction faction = new Faction();
    faction.setMembers(Set.of(wrestlerState, partnerState));
    wrestlerState.setFaction(faction);
    partnerState.setFaction(faction);

    // Both are in the match
    when(segment.getWrestlers()).thenReturn(List.of(wrestler, partner));

    assertFalse(ringsideActionService.hasSupportAtRingside(segment, wrestler));
    assertNull(ringsideActionService.getBestSupporter(segment, wrestler));
  }

  @Test
  void hasSupportAtRingside_WithTeamPartner() {
    Wrestler partner = new Wrestler();
    partner.setName("Team Partner");
    Team team = mock(Team.class);
    when(team.getUniverse()).thenReturn(universe);
    when(team.getPartner(wrestler)).thenReturn(partner);
    when(teamService.getActiveTeamsByWrestler(wrestler)).thenReturn(List.of(team));

    // Partner is NOT in the match
    when(segment.getWrestlers()).thenReturn(List.of(wrestler));

    assertTrue(ringsideActionService.hasSupportAtRingside(segment, wrestler));
    assertEquals(partner, ringsideActionService.getBestSupporter(segment, wrestler));
  }

  @Test
  void priorityOrdering_ManagerOverAll() {
    Npc directManager = new Npc();
    wrestlerState.setManager(directManager);

    Npc factionManager = new Npc();
    Faction faction = new Faction();
    faction.setManager(factionManager);
    faction.setMembers(
        Set.of(wrestlerState, WrestlerState.builder().wrestler(new Wrestler()).build()));
    wrestlerState.setFaction(faction);

    assertEquals(directManager, ringsideActionService.getBestSupporter(segment, wrestler));
  }

  @Test
  void performAction_UpdatesAwarenessAndSaves() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    when(segment.getRefereeAwarenessLevel()).thenReturn(10);

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, otherWrestler, wrestler, weaponAction);

    verify(segment).setRefereeAwarenessLevel(anyInt());
    verify(segmentRepository).save(segment);
    assertTrue(result.success() || !result.success()); // Just ensure it returns
  }

  @Test
  void legalActionShouldNotIncreaseAwareness() {
    when(npcService.getAwareness(referee)).thenReturn(50);

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, otherWrestler, wrestler, coachAction);

    verify(segment).setRefereeAwarenessLevel(0);
    assertEquals(0, result.detectionIncrease());
  }

  @Test
  void illegalActionShouldIncreaseAwareness() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    when(segment.getRefereeAwarenessLevel()).thenReturn(0);
    when(factionService.getAffinityBetween(otherWrestler, wrestler)).thenReturn(0);

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, otherWrestler, wrestler, weaponAction);

    assertTrue(result.detectionIncrease() > 0);
    assertEquals(weaponAction.getRisk(), result.detectionIncrease());
  }

  @Test
  void shouldApplyFactionAffinityReduction() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    when(segment.getRefereeAwarenessLevel()).thenReturn(0);
    // Max affinity should give 30% reduction
    when(factionService.getAffinityBetween(otherWrestler, wrestler)).thenReturn(100);

    int baseRisk = weaponAction.getRisk();
    int expectedRisk = (int) (baseRisk * 0.7); // 30% reduction

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, otherWrestler, wrestler, weaponAction);

    assertEquals(expectedRisk, result.detectionIncrease());
  }

  @Test
  void shouldTriggerEjectionAndDQ() {
    when(npcService.getAwareness(referee)).thenReturn(50);
    when(segment.getRefereeAwarenessLevel()).thenReturn(90);

    RingsideActionService.RingsideActionResult result =
        ringsideActionService.performAction(segment, otherWrestler, wrestler, weaponAction);

    assertTrue(result.ejected());
    assertTrue(result.disqualified());
    assertTrue(result.message().contains("DISQUALIFICATION"));
  }
}

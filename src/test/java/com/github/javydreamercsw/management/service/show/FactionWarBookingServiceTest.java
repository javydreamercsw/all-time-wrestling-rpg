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
package com.github.javydreamercsw.management.service.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.feud.FeudParticipant;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeud;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FactionWarBookingServiceTest {

  @Mock private FactionRivalryService factionRivalryService;
  @Mock private FactionService factionService;
  @Mock private MultiWrestlerFeudService multiWrestlerFeudService;
  @Mock private SegmentTypeRepository segmentTypeRepository;
  @Mock private NPCSegmentResolutionService npcSegmentResolutionService;

  private FactionWarBookingService service;

  private SegmentType oneOnOne;
  private SegmentType tagTeam;
  private Show show;

  @BeforeEach
  void setUp() {
    service =
        new FactionWarBookingService(
            factionRivalryService,
            factionService,
            multiWrestlerFeudService,
            segmentTypeRepository,
            npcSegmentResolutionService);

    oneOnOne = new SegmentType();
    oneOnOne.setName("One on One");
    oneOnOne.setCode(WellKnownSegmentType.ONE_ON_ONE.getCode());

    tagTeam = new SegmentType();
    tagTeam.setName("Tag Team");
    tagTeam.setCode(WellKnownSegmentType.TAG_TEAM.getCode());

    show = new Show();
    show.setName("Faction War PLE");

    when(segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()))
        .thenReturn(Optional.of(oneOnOne));
    when(segmentTypeRepository.findByCode(WellKnownSegmentType.TAG_TEAM.getCode()))
        .thenReturn(Optional.of(tagTeam));
    when(factionRivalryService.getActiveFactionRivalries()).thenReturn(List.of());
    when(multiWrestlerFeudService.getInterFactionFeuds()).thenReturn(List.of());
    when(factionService.findAll()).thenReturn(List.of());
  }

  @Test
  void generateFactionWarSegments_returnsEmptyWhenNoOneOnOneType() {
    when(segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()))
        .thenReturn(Optional.empty());

    List<Segment> result = service.generateFactionWarSegments(show, 5);

    assertThat(result).isEmpty();
    verify(npcSegmentResolutionService, never())
        .resolveTeamSegment(any(), any(), any(), any(), any());
  }

  @Test
  void generateFactionWarSegments_booksSinglesFromRivalryWhenSingleFactionMembers() {
    Wrestler leader1 = wrestler(1L, "Leader Alpha");
    Wrestler leader2 = wrestler(2L, "Leader Beta");
    Faction f1 = faction(1L, "Alpha", leader1, Set.of(state(leader1)));
    Faction f2 = faction(2L, "Beta", leader2, Set.of(state(leader2)));
    FactionRivalry rivalry = rivalry(f1, f2, 10);

    when(factionRivalryService.getActiveFactionRivalries()).thenReturn(List.of(rivalry));
    Segment resolved = new Segment();
    when(npcSegmentResolutionService.resolveTeamSegment(
            any(), any(), eq(oneOnOne), eq(show), any()))
        .thenReturn(resolved);

    List<Segment> result = service.generateFactionWarSegments(show, 5);

    assertThat(result).contains(resolved);
    verify(npcSegmentResolutionService)
        .resolveTeamSegment(
            any(SegmentTeam.class), any(SegmentTeam.class), eq(oneOnOne), eq(show), any());
  }

  @Test
  void generateFactionWarSegments_booksTagTeamWhenBothFactionsHaveTwoOrMoreMembers() {
    Wrestler w1a = wrestler(1L, "Alpha 1");
    Wrestler w1b = wrestler(2L, "Alpha 2");
    Wrestler w2a = wrestler(3L, "Beta 1");
    Wrestler w2b = wrestler(4L, "Beta 2");
    Faction f1 = faction(1L, "Alpha", null, Set.of(state(w1a), state(w1b)));
    Faction f2 = faction(2L, "Beta", null, Set.of(state(w2a), state(w2b)));
    FactionRivalry rivalry = rivalry(f1, f2, 25);

    when(factionRivalryService.getActiveFactionRivalries()).thenReturn(List.of(rivalry));
    Segment resolved = new Segment();
    when(npcSegmentResolutionService.resolveTeamSegment(any(), any(), eq(tagTeam), eq(show), any()))
        .thenReturn(resolved);

    List<Segment> result = service.generateFactionWarSegments(show, 5);

    assertThat(result).contains(resolved);
    verify(npcSegmentResolutionService)
        .resolveTeamSegment(
            any(SegmentTeam.class), any(SegmentTeam.class), eq(tagTeam), eq(show), any());
  }

  @Test
  void generateFactionWarSegments_fillsCrossFactionsWhenNoRivalries() {
    Wrestler wa = wrestler(1L, "Indie A");
    Wrestler wb = wrestler(2L, "Indie B");
    Faction fa = faction(1L, "Faction A", wa, Set.of(state(wa)));
    Faction fb = faction(2L, "Faction B", wb, Set.of(state(wb)));

    when(factionService.findAll()).thenReturn(List.of(fa, fb));
    Segment resolved = new Segment();
    when(npcSegmentResolutionService.resolveTeamSegment(
            any(), any(), eq(oneOnOne), eq(show), any()))
        .thenReturn(resolved);

    List<Segment> result = service.generateFactionWarSegments(show, 5);

    assertThat(result).contains(resolved);
  }

  @Test
  void generateFactionWarSegments_booksFromInterFactionFeud() {
    Wrestler w1 = wrestler(1L, "Feud W1");
    Wrestler w2 = wrestler(2L, "Feud W2");

    FeudParticipant p1 = new FeudParticipant();
    p1.setWrestler(w1);
    p1.setIsActive(true);
    FeudParticipant p2 = new FeudParticipant();
    p2.setWrestler(w2);
    p2.setIsActive(true);

    MultiWrestlerFeud feud = new MultiWrestlerFeud();
    feud.setName("Alpha vs Beta Feud");
    feud.setParticipants(List.of(p1, p2));

    when(multiWrestlerFeudService.getInterFactionFeuds()).thenReturn(List.of(feud));
    Segment resolved = new Segment();
    when(npcSegmentResolutionService.resolveTeamSegment(
            any(), any(), eq(oneOnOne), eq(show), any()))
        .thenReturn(resolved);

    List<Segment> result = service.generateFactionWarSegments(show, 5);

    assertThat(result).contains(resolved);
  }

  @Test
  void generateFactionWarSegments_skipsRivalryWithInactiveFaction() {
    Wrestler leader1 = wrestler(1L, "Leader A");
    Faction f1 = faction(1L, "Active", leader1, Set.of(state(leader1)));
    Faction f2 =
        Faction.builder()
            .id(2L)
            .name("Inactive")
            .isActive(false)
            .formedDate(Instant.now())
            .members(new HashSet<>())
            .build();
    FactionRivalry rivalry = rivalry(f1, f2, 10);

    when(factionRivalryService.getActiveFactionRivalries()).thenReturn(List.of(rivalry));

    List<Segment> result = service.generateFactionWarSegments(show, 5);

    assertThat(result).isEmpty();
    verify(npcSegmentResolutionService, never())
        .resolveTeamSegment(any(), any(), any(), any(), any());
  }

  @Test
  void generateFactionWarSegments_skipsRivalryWhenFactionHasNoBookableMembers() {
    Faction emptyFaction = faction(1L, "Empty", null, Set.of());
    Wrestler leader2 = wrestler(2L, "Leader B");
    Faction f2 = faction(2L, "Beta", leader2, Set.of(state(leader2)));
    FactionRivalry rivalry = rivalry(emptyFaction, f2, 10);

    when(factionRivalryService.getActiveFactionRivalries()).thenReturn(List.of(rivalry));

    List<Segment> result = service.generateFactionWarSegments(show, 5);

    assertThat(result).isEmpty();
    verify(npcSegmentResolutionService, never())
        .resolveTeamSegment(any(), any(), any(), any(), any());
  }

  @Test
  void generateFactionWarSegments_handlesExceptionInSegmentResolution() {
    Wrestler leader1 = wrestler(1L, "Leader Alpha");
    Wrestler leader2 = wrestler(2L, "Leader Beta");
    Faction f1 = faction(1L, "Alpha", leader1, Set.of(state(leader1)));
    Faction f2 = faction(2L, "Beta", leader2, Set.of(state(leader2)));
    FactionRivalry rivalry = rivalry(f1, f2, 10);

    when(factionRivalryService.getActiveFactionRivalries()).thenReturn(List.of(rivalry));
    when(npcSegmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Segment resolution failed"));

    List<Segment> result = service.generateFactionWarSegments(show, 5);

    assertThat(result).isEmpty();
  }

  // ==================== BUILDER HELPERS ====================

  private static Wrestler wrestler(long id, String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    return w;
  }

  private static WrestlerState state(Wrestler wrestler) {
    WrestlerState ws = new WrestlerState();
    ws.setWrestler(wrestler);
    return ws;
  }

  private static Faction faction(
      long id, String name, Wrestler leader, Set<WrestlerState> members) {
    Faction f =
        Faction.builder()
            .id(id)
            .name(name)
            .leader(leader)
            .members(new HashSet<>(members))
            .isActive(true)
            .formedDate(Instant.now())
            .build();
    members.forEach(m -> m.setFaction(f));
    return f;
  }

  private static FactionRivalry rivalry(Faction f1, Faction f2, int heat) {
    FactionRivalry r = new FactionRivalry();
    r.setFaction1(f1);
    r.setFaction2(f2);
    r.setHeat(heat);
    r.setIsActive(true);
    return r;
  }
}

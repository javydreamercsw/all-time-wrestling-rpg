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
package com.github.javydreamercsw.management.domain.show.segment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class SegmentRepositoryIT extends ManagementIntegrationTest {

  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private NpcRepository npcRepository;
  @Autowired private RingsideActionService ringsideActionService;

  @Test
  @Transactional
  void testFindByIdWithDetails() {
    ShowType showType = new ShowType();
    showType.setName("Weekly IT");
    showType.setDescription("Weekly show for IT");
    showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("Test Show IT");
    show.setDescription("Test show for IT");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    showRepository.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Match IT");
    segmentType.setDescription("Match for IT");
    segmentTypeRepository.save(segmentType);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment = segmentRepository.save(segment);

    Long id = segment.getId();

    // This should trigger MultipleBagFetchException if the bug exists
    Optional<Segment> result = segmentRepository.findByIdWithDetails(id);

    assertTrue(result.isPresent());
  }

  @Test
  @Transactional
  void testHasSupportAtRingside_WithFaction() {
    ShowType showType = new ShowType();
    showType.setName("Weekly Faction");
    showType.setDescription("Weekly show for Faction");
    showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("Faction Show");
    show.setDescription("Faction show description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    showRepository.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Match Faction");
    segmentType.setDescription("Match for Faction");
    segmentTypeRepository.save(segmentType);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment = segmentRepository.save(segment);

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Wrestler 1");
    wrestler1.setDeckSize(15);
    wrestler1.setStartingHealth(10);
    wrestler1.setStartingStamina(10);
    wrestler1 = wrestlerRepository.save(wrestler1);

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setName("Wrestler 2");
    wrestler2.setDeckSize(15);
    wrestler2.setStartingHealth(10);
    wrestler2.setStartingStamina(10);
    wrestler2 = wrestlerRepository.save(wrestler2);

    Faction faction = new Faction();
    faction.setName("Test Faction");
    faction.addMember(wrestler1);
    faction.addMember(wrestler2);
    factionRepository.save(faction);

    // Segment with only wrestler1
    segment.addParticipant(wrestler1);
    segment = segmentRepository.save(segment);

    // Now call the service.
    assertTrue(ringsideActionService.hasSupportAtRingside(segment, wrestler1));
  }

  @Test
  @Transactional
  void testHasSupportAtRingside_WithFactionManager() {
    ShowType showType = new ShowType();
    showType.setName("Weekly FM");
    showType.setDescription("Weekly show for FM");
    showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("FM Show");
    show.setDescription("FM show description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    showRepository.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Match FM");
    segmentType.setDescription("Match for FM");
    segmentTypeRepository.save(segmentType);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment = segmentRepository.save(segment);

    Wrestler wrestler = new Wrestler();
    wrestler.setName("Wrestler FM");
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(10);
    wrestler.setStartingStamina(10);
    wrestler = wrestlerRepository.save(wrestler);

    Npc manager = new Npc();
    manager.setName("Faction Manager");
    manager.setNpcType("Manager");
    manager = npcRepository.save(manager);

    Faction faction = new Faction();
    faction.setName("FM Faction");
    faction.setManager(manager);
    faction.addMember(wrestler);
    factionRepository.save(faction);

    segment.addParticipant(wrestler);
    segment = segmentRepository.save(segment);

    assertTrue(ringsideActionService.hasSupportAtRingside(segment, wrestler));
  }
}

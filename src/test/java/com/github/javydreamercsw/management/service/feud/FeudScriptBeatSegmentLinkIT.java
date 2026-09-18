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
package com.github.javydreamercsw.management.service.feud;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * DB-level verification for {@link FeudScriptBeatRepository#findByActualSegment}. A production
 * incident (approving a show whose card included a story-arc beat) threw {@code
 * LazyInitializationException} on {@code beat.getScript().getName()} inside {@code
 * ShowDetailView}'s segments grid renderer — a Vaadin lambda invoked well after the repository call
 * returns, outside any session. Mocked unit tests (see {@code FeudScriptServiceTest}) cannot
 * reproduce this: a mocked {@code FeudScriptBeat} has no lazy proxy to violate. This IT exercises
 * the real fetch-join fix against a live database.
 */
@TestPropertySource(properties = "data.initializer.enabled=false")
class FeudScriptBeatSegmentLinkIT extends ManagementIntegrationTest {

  @Autowired private FeudScriptBeatRepository beatRepository;
  @Autowired private FeudScriptRepository feudScriptRepository;
  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private SegmentRepository segmentRepository;

  @BeforeEach
  void setUpUniverse() {
    if (defaultUniverse == null) {
      defaultUniverse =
          universeRepository.saveAndFlush(
              Universe.builder()
                  .name("Feud Beat Segment Link IT Universe")
                  .type(Universe.UniverseType.GLOBAL)
                  .build());
    }
    universeContextService.setCurrentUniverse(defaultUniverse);
  }

  private Wrestler wrestler(String name) {
    Wrestler w = new Wrestler();
    w.setName(name);
    w.setGender(Gender.MALE);
    return wrestlerRepository.save(w);
  }

  private Segment segment(Wrestler w1, Wrestler w2) {
    ShowType showType = new ShowType();
    showType.setName("Beat Link IT Show Type " + System.nanoTime());
    showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("Beat Link IT Show");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    showRepository.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Beat Link IT Match " + System.nanoTime());
    segmentTypeRepository.save(segmentType);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment.addParticipant(w1);
    segment.addParticipant(w2);
    return segmentRepository.save(segment);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void findByActualSegment_scriptInitializedOutsideSession() {
    Wrestler w1 = wrestler("Bobby Lashley");
    Wrestler w2 = wrestler("Shelton Benjamin");

    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(w1);
    rivalry.setWrestler2(w2);
    rivalry.setUniverse(defaultUniverse);
    rivalry = rivalryRepository.save(rivalry);

    FeudScript script = new FeudScript();
    script.setName("Bobby Lashley vs Shelton Benjamin Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setRivalry(rivalry);
    script = feudScriptRepository.save(script);

    Segment segment = segment(w1, w2);

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setScript(script);
    beat.setSegmentType("Singles Match");
    beat.setBeatOrder(2);
    beat.setActualSegment(segment);
    beat = beatRepository.save(beat);

    // Reload OUTSIDE any transaction/session — exactly the state ShowDetailView's grid renderer
    // sees when it runs. Before the fetch-join fix, beat.getScript().getName() below threw
    // LazyInitializationException here.
    FeudScriptBeat found = beatRepository.findByActualSegment(segment).orElseThrow();
    assertThat(found.getId()).isEqualTo(beat.getId());
    assertThat(found.getScript().getName()).isEqualTo("Bobby Lashley vs Shelton Benjamin Arc");
    assertThat(found.getBeatOrder()).isEqualTo(2);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void findByActualSegment_noLinkedBeat_returnsEmpty() {
    Wrestler w1 = wrestler("Randy Orton");
    Wrestler w2 = wrestler("Edge");
    Segment segment = segment(w1, w2);

    Optional<FeudScriptBeat> found = beatRepository.findByActualSegment(segment);

    assertThat(found).isEmpty();
  }
}

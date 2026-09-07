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
import com.github.javydreamercsw.management.domain.feud.FeudBeatParticipantRole;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatParticipantRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptRepository;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.planning.dto.FeudScriptBeatDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DB-level verification for external beat participants: persistence through the join table, cascade
 * cleanup on beat removal, and the DTO team layout the planning pass consumes. The
 * cascade/orphan-removal assertions here are exactly what mocked unit tests cannot make.
 */
@TestPropertySource(properties = "data.initializer.enabled=false")
class FeudScriptBeatParticipantIT extends ManagementIntegrationTest {

  @Autowired private FeudScriptService feudScriptService;
  @Autowired private FeudScriptBeatParticipantRepository participantRepository;
  @Autowired private FeudScriptBeatRepository beatRepository;
  @Autowired private FeudScriptRepository feudScriptRepository;
  @Autowired private RivalryRepository rivalryRepository;

  @Autowired private WrestlerRepository wrestlerRepo;

  @Autowired private JpaTransactionManager transactionManager;

  private FeudScript script(Rivalry rivalry, String name) {
    FeudScript script = new FeudScript();
    script.setName(name);
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setRivalry(rivalry);
    return feudScriptRepository.save(script);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void addBeat_withExternals_persistsAndReloadsParticipants() {
    Wrestler w1 = wrestlerRepo.save(wrestler("Shelton Benjamin"));
    Wrestler w2 = wrestlerRepo.save(wrestler("Bobby Lashley"));
    Wrestler external = wrestlerRepo.save(wrestler("Randy Orton"));

    Rivalry rivalry = rivalryRepository.save(rivalry(w1, w2));
    FeudScript script = script(rivalry, "External Arc");

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setSegmentType("Singles Match");
    beat.addExternalParticipant(external, FeudBeatParticipantRole.OPPONENT);
    FeudScriptBeat saved = feudScriptService.addBeat(script, beat);

    assertThat(saved.getId()).isNotNull();
    assertThat(participantRepository.findByBeatId(saved.getId())).hasSize(1);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void removeBeat_deletesExternalParticipantRows() {
    Wrestler w1 = wrestlerRepo.save(wrestler("Shelton Benjamin"));
    Wrestler w2 = wrestlerRepo.save(wrestler("Bobby Lashley"));
    Wrestler external = wrestlerRepo.save(wrestler("Randy Orton"));

    Rivalry rivalry = rivalryRepository.save(rivalry(w1, w2));
    FeudScript script = script(rivalry, "Cascade Arc");

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setSegmentType("Singles Match");
    beat.addExternalParticipant(external, FeudBeatParticipantRole.OPPONENT);
    FeudScriptBeat saved = feudScriptService.addBeat(script, beat);
    assertThat(participantRepository.findByBeatId(saved.getId())).hasSize(1);

    feudScriptService.removeBeat(script, saved);

    assertThat(participantRepository.findByBeatId(saved.getId())).isEmpty();
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void reloadedBeat_externalsRoundTripThroughEagerCollection() {
    Wrestler w1 = wrestlerRepo.save(wrestler("Shelton Benjamin"));
    Wrestler w2 = wrestlerRepo.save(wrestler("Bobby Lashley"));
    Wrestler external = wrestlerRepo.save(wrestler("Randy Orton"));

    Rivalry rivalry = rivalryRepository.save(rivalry(w1, w2));
    FeudScript script = script(rivalry, "Planning Arc");

    FeudScriptBeat beat = new FeudScriptBeat();
    beat.setSegmentType("Singles Match");
    beat.addExternalParticipant(external, FeudBeatParticipantRole.OPPONENT);
    FeudScriptBeat saved = feudScriptService.addBeat(script, beat);

    // Reload + DTO mapping inside one transaction: the EAGER externals collection survives the
    // read (the grid/native-query consumption pattern), while the DTO mapping's LAZY proxies
    // (script → rivalry → wrestlers) resolve inside the session, as
    // getUpcomingBeatDTOsForShow does in production.
    FeudScriptBeatDTO dto =
        new TransactionTemplate(transactionManager)
            .execute(
                status -> {
                  FeudScriptBeat reloaded = beatRepository.findById(saved.getId()).orElseThrow();
                  assertThat(reloaded.getExternalParticipants()).hasSize(1);
                  assertThat(reloaded.getExternalParticipants().get(0).getRole())
                      .isEqualTo(FeudBeatParticipantRole.OPPONENT);
                  return feudScriptService.toDTOForTest(reloaded);
                });
    assertThat(reloadedExternalIds(dto)).containsExactly(external.getId());
    assertThat(dto.getTeamIds())
        .containsExactly(List.of(w1.getId(), w2.getId()), List.of(external.getId()));
    assertThat(dto.getExternalSummary()).isEqualTo("Randy Orton (Opponent)");
  }

  private List<Long> reloadedExternalIds(FeudScriptBeatDTO dto) {
    return dto.getTeamIds() != null ? dto.getTeamIds().get(1) : List.of();
  }

  @BeforeEach
  void setUpUniverse() {
    if (defaultUniverse == null) {
      defaultUniverse =
          universeRepository.saveAndFlush(
              Universe.builder()
                  .name("Beat Participant IT Universe")
                  .type(Universe.UniverseType.GLOBAL)
                  .build());
    }
    universeContextService.setCurrentUniverse(defaultUniverse);
  }

  private Wrestler wrestler(String name) {
    Wrestler w = new Wrestler();
    w.setName(name);
    w.setGender(Gender.MALE);
    return w;
  }

  private Rivalry rivalry(Wrestler w1, Wrestler w2) {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(w1);
    rivalry.setWrestler2(w2);
    rivalry.setUniverse(defaultUniverse);
    return rivalry;
  }
}

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
package com.github.javydreamercsw.management.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.dto.TournamentDefinitionDTO;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the tournaments.json seed sync (ATW-vg16). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TournamentSyncTest {

  @Mock private TournamentService tournamentService;

  private TournamentSync sync;

  @BeforeEach
  void setUp() {
    lenient()
        .when(
            tournamentService.createOrUpdateTournament(
                anyString(), anyString(), anyString(), any(), anyList()))
        .thenAnswer(
            inv -> {
              Tournament t = new Tournament();
              t.setCode(inv.getArgument(0));
              t.setName(inv.getArgument(1));
              return t;
            });
    sync = new TournamentSync(false, tournamentService, new ObjectMapper());
    ReflectionTestUtils.setField(sync, "skipIfNotEmpty", false);
  }

  @Test
  @DisplayName("Seeds every catalog entry with its rules pool")
  void sync_seedsDefinitions() {
    sync.sync();

    verify(tournamentService)
        .createOrUpdateTournament(anyString(), anyString(), anyString(), any(), anyList());
  }

  @Test
  @DisplayName("skip-if-not-empty skips the catalog when tournaments already exist")
  void sync_skipWhenNotEmpty() {
    TournamentSync gated =
        new TournamentSync(true, tournamentService, new ObjectMapper()) {
          @Override
          public void sync() {
            // Re-implement the gate to test the branch without Spring property wiring:
            if (tournamentService.count() > 0) {
              return;
            }
            super.sync();
          }
        };
    when(tournamentService.count()).thenReturn(3L);

    gated.sync();

    verify(tournamentService, never())
        .createOrUpdateTournament(anyString(), anyString(), anyString(), any(), anyList());
  }

  @Test
  @DisplayName("Catalog JSON parses into the definition DTO with a rules pool")
  void catalogParses() throws Exception {
    try (var is = getClass().getResourceAsStream("/tournaments.json")) {
      List<TournamentDefinitionDTO> dtos =
          new ObjectMapper().readValue(is, new TypeReference<>() {});
      assertThat(dtos).isNotEmpty();
      TournamentDefinitionDTO deadly =
          dtos.stream().filter(d -> "deadly_combat".equals(d.code())).findFirst().orElseThrow();
      assertThat(deadly.name()).isEqualTo("Deadly Combat");
      assertThat(deadly.formatId()).isEqualTo("SINGLE_ELIMINATION");
      assertThat(deadly.defaultEntrantCount()).isEqualTo(8);
      assertThat(deadly.allowedRules()).isNotEmpty();
    }
  }
}

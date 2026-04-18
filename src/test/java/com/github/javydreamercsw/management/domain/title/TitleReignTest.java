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
package com.github.javydreamercsw.management.domain.title;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for TitleReign entity. Tests the ATW RPG title reign tracking functionality. */
@DisplayName("TitleReign Tests")
class TitleReignTest {

  private TitleReign titleReign;
  private Title title;
  private Wrestler wrestler;
  private WrestlerState state;
  private Universe universe;

  @BeforeEach
  void setUp() {
    universe = Universe.builder().id(1L).name("Default").build();

    title = new Title();
    title.setName("Test Championship");
    title.setTier(WrestlerTier.MAIN_EVENTER);
    title.setUniverse(universe);

    wrestler = Wrestler.builder().build();
    wrestler.setName("Test Champion");
    wrestler.setStartingHealth(15);

    state = WrestlerState.builder().wrestler(wrestler).universe(universe).fans(120000L).build();

    titleReign = new TitleReign();
    titleReign.setTitle(title);
    titleReign.getChampions().add(wrestler);
    titleReign.setReignNumber(1);
    titleReign.setStartDate(Instant.now());
  }

  @Test
  @DisplayName("Should initialize as current reign")
  void shouldInitializeAsCurrentReign() {
    assertThat(titleReign.isCurrentReign()).isTrue();
    assertThat(titleReign.getEndDate()).isNull();
  }

  @Test
  @DisplayName("Should end reign properly")
  void shouldEndReignProperly() {
    assertThat(titleReign.isCurrentReign()).isTrue();

    Instant endTime = Instant.now();
    titleReign.endReign(endTime);

    assertThat(titleReign.isCurrentReign()).isFalse();
    assertThat(titleReign.getEndDate()).isEqualTo(endTime);
  }

  @Test
  @DisplayName("Should maintain relationships correctly")
  void shouldMaintainRelationshipsCorrectly() {
    assertThat(titleReign.getTitle()).isEqualTo(title);
    assertThat(titleReign.getChampions()).containsExactly(wrestler);
    assertThat(titleReign.getTitle().getName()).isEqualTo("Test Championship");
    assertThat(titleReign.getChampions().get(0).getName()).isEqualTo("Test Champion");
  }
}

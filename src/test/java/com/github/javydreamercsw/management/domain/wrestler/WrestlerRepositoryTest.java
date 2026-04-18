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
package com.github.javydreamercsw.management.domain.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class WrestlerRepositoryTest extends AbstractJpaTest {
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private UniverseRepository universeRepository;

  private Universe defaultUniverse;

  @BeforeEach
  void setUp() {
    defaultUniverse = universeRepository.save(Universe.builder().name("Default Universe").build());
  }

  @Test
  void testFindAllByPagination() {
    Wrestler wrestler1 = Wrestler.builder().build();
    wrestler1.setName("Wrestler One");
    wrestler1.setDeckSize(15);
    wrestler1.setLowHealth(0);
    wrestler1.setLowStamina(0);
    wrestler1.setStartingHealth(15);
    wrestler1.setStartingStamina(0);
    wrestler1.setCreationDate(java.time.Instant.now());
    wrestler1.setIsPlayer(true);
    wrestler1.setCurrentHealth(15);
    wrestler1.setGender(Gender.MALE);
    wrestler1 = wrestlerRepository.save(wrestler1);

    WrestlerState state1 =
        WrestlerState.builder()
            .wrestler(wrestler1)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .bumps(0)
            .build();
    wrestlerStateRepository.save(state1);

    Wrestler wrestler2 = Wrestler.builder().build();
    wrestler2.setName("Wrestler Two");
    wrestler2.setDeckSize(15);
    wrestler2.setLowHealth(0);
    wrestler2.setLowStamina(0);
    wrestler2.setStartingHealth(15);
    wrestler2.setStartingStamina(0);
    wrestler2.setCreationDate(java.time.Instant.now());
    wrestler2.setIsPlayer(true);
    wrestler2.setCurrentHealth(15);
    wrestler2.setGender(Gender.MALE);
    wrestler2 = wrestlerRepository.save(wrestler2);

    WrestlerState state2 =
        WrestlerState.builder()
            .wrestler(wrestler2)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .bumps(0)
            .build();
    wrestlerStateRepository.save(state2);

    var page = wrestlerRepository.findAllBy(PageRequest.of(0, 1));
    assertThat(page.getContent()).hasSize(1);
    assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void testFindByName() {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName("Decked Wrestler");
    wrestler.setDeckSize(15);
    wrestler.setLowHealth(0);
    wrestler.setLowStamina(0);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(0);
    wrestler.setCreationDate(java.time.Instant.now());
    wrestler.setIsPlayer(true);
    wrestler.setCurrentHealth(15);
    wrestler.setGender(Gender.MALE);
    wrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .bumps(0)
            .build();
    wrestlerStateRepository.save(state);

    Optional<Wrestler> found = wrestlerRepository.findByName("Decked Wrestler");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Decked Wrestler");
  }

  @Test
  void testFindByExternalId() {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName("External Wrestler");
    wrestler.setExternalId("ext-123");
    wrestler.setDeckSize(15);
    wrestler.setLowHealth(0);
    wrestler.setLowStamina(0);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(0);
    wrestler.setCreationDate(java.time.Instant.now());
    wrestler.setIsPlayer(true);
    wrestler.setCurrentHealth(15);
    wrestler.setGender(Gender.MALE);
    wrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .bumps(0)
            .build();
    wrestlerStateRepository.save(state);

    Optional<Wrestler> found = wrestlerRepository.findByExternalId("ext-123");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("External Wrestler");
  }
}

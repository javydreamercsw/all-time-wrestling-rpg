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
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class WrestlerRepositoryTest extends AbstractJpaTest {
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private FactionRepository factionRepository;

  @Override
  @BeforeEach
  public void baseSetUp() throws Exception {
    super.baseSetUp();
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
    wrestler1.setGender(Gender.MALE);
    wrestler1 = wrestlerRepository.save(wrestler1);

    WrestlerState state1 =
        WrestlerState.builder()
            .wrestler(wrestler1)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .bumps(0)
            .currentHealth(15)
            .build();
    state1 = wrestlerStateRepository.save(state1);
    wrestler1.getWrestlerStates().add(state1);

    Wrestler wrestler2 = Wrestler.builder().build();
    wrestler2.setName("Wrestler Two");
    wrestler2.setDeckSize(15);
    wrestler2.setLowHealth(0);
    wrestler2.setLowStamina(0);
    wrestler2.setStartingHealth(15);
    wrestler2.setStartingStamina(0);
    wrestler2.setCreationDate(java.time.Instant.now());
    wrestler2.setIsPlayer(true);
    wrestler2.setGender(Gender.MALE);
    wrestler2 = wrestlerRepository.save(wrestler2);

    WrestlerState state2 =
        WrestlerState.builder()
            .wrestler(wrestler2)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .bumps(0)
            .currentHealth(15)
            .build();
    state2 = wrestlerStateRepository.save(state2);
    wrestler2.getWrestlerStates().add(state2);

    var page = wrestlerRepository.findAllBy(PageRequest.of(0, 100));
    assertThat(page.getContent())
        .extracting(Wrestler::getName)
        .contains("Wrestler One", "Wrestler Two");
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
    wrestler.setGender(Gender.MALE);
    wrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .bumps(0)
            .currentHealth(15)
            .build();
    state = wrestlerStateRepository.save(state);
    wrestler.getWrestlerStates().add(state);

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
    wrestler.setGender(Gender.MALE);
    wrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .bumps(0)
            .currentHealth(15)
            .build();
    state = wrestlerStateRepository.save(state);
    wrestler.getWrestlerStates().add(state);

    Optional<Wrestler> found = wrestlerRepository.findByExternalId("ext-123");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("External Wrestler");
  }

  @Test
  void testFindByNameWithFaction() {
    Faction faction = Faction.builder().name("Test Faction").affinity(50).isActive(true).build();
    faction = factionRepository.save(faction);

    Wrestler wrestler =
        Wrestler.builder()
            .name("Faction Wrestler")
            .deckSize(15)
            .startingHealth(15)
            .gender(Gender.MALE)
            .active(true)
            .creationDate(java.time.Instant.now())
            .build();
    wrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .faction(faction)
            .bumps(0)
            .currentHealth(15)
            .build();
    state = wrestlerStateRepository.save(state);
    wrestler.getWrestlerStates().add(state);

    Optional<Wrestler> found = wrestlerRepository.findByName("Faction Wrestler");
    assertThat(found).isPresent();
    Wrestler w = found.get();
    assertThat(w.getState(defaultUniverse.getId())).isPresent();
    assertThat(w.getState(defaultUniverse.getId()).get().getFaction()).isNotNull();
    assertThat(w.getState(defaultUniverse.getId()).get().getFaction().getName())
        .isEqualTo("Test Faction");
    assertThat(w.getState(defaultUniverse.getId()).get().getFaction().getAffinity()).isEqualTo(50);
  }

  @Test
  void testFindAllByActiveTrueWithFaction() {
    Faction faction = Faction.builder().name("Active Faction").affinity(30).isActive(true).build();
    faction = factionRepository.save(faction);

    Wrestler wrestler =
        Wrestler.builder()
            .name("Active Faction Wrestler")
            .deckSize(15)
            .startingHealth(15)
            .gender(Gender.MALE)
            .active(true)
            .creationDate(java.time.Instant.now())
            .build();
    wrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .faction(faction)
            .bumps(0)
            .currentHealth(15)
            .build();
    state = wrestlerStateRepository.save(state);
    wrestler.getWrestlerStates().add(state);

    var activeWrestlers = wrestlerRepository.findAllByActiveTrue();
    assertThat(activeWrestlers).anyMatch(w -> "Active Faction Wrestler".equals(w.getName()));
    Wrestler found =
        activeWrestlers.stream()
            .filter(w -> "Active Faction Wrestler".equals(w.getName()))
            .findFirst()
            .orElseThrow();

    assertThat(found.getState(defaultUniverse.getId())).isPresent();
    assertThat(found.getState(defaultUniverse.getId()).get().getFaction()).isNotNull();
    assertThat(found.getState(defaultUniverse.getId()).get().getFaction().getAffinity())
        .isEqualTo(30);
  }

  @Test
  void testFindAllByPaginationWithFaction() {
    Faction faction = Faction.builder().name("Paged Faction").affinity(20).isActive(true).build();
    faction = factionRepository.save(faction);

    Wrestler wrestler =
        Wrestler.builder()
            .name("Paged Wrestler")
            .deckSize(15)
            .startingHealth(15)
            .gender(Gender.MALE)
            .active(true)
            .creationDate(java.time.Instant.now())
            .build();
    wrestler = wrestlerRepository.save(wrestler);

    WrestlerState state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(defaultUniverse)
            .fans(100L)
            .tier(WrestlerTier.ROOKIE)
            .faction(faction)
            .bumps(0)
            .currentHealth(15)
            .build();
    state = wrestlerStateRepository.save(state);
    wrestler.getWrestlerStates().add(state);

    var page = wrestlerRepository.findAllBy(PageRequest.of(0, 100));
    Wrestler pagedWrestler =
        page.getContent().stream()
            .filter(w -> "Paged Wrestler".equals(w.getName()))
            .findFirst()
            .orElseThrow();

    assertThat(pagedWrestler.getState(defaultUniverse.getId())).isPresent();
    assertThat(pagedWrestler.getState(defaultUniverse.getId()).get().getFaction()).isNotNull();
    assertThat(pagedWrestler.getState(defaultUniverse.getId()).get().getFaction().getAffinity())
        .isEqualTo(20);
  }

  @Test
  void testFindByFansQueries() {
    Wrestler lowFans =
        Wrestler.builder().name("Low Fans").creationDate(java.time.Instant.now()).build();
    lowFans = wrestlerRepository.save(lowFans);
    WrestlerState s1 =
        WrestlerState.builder().wrestler(lowFans).universe(defaultUniverse).fans(10L).build();
    s1 = wrestlerStateRepository.save(s1);
    lowFans.getWrestlerStates().add(s1);

    Wrestler midFans =
        Wrestler.builder().name("Mid Fans").creationDate(java.time.Instant.now()).build();
    midFans = wrestlerRepository.save(midFans);
    WrestlerState s2 =
        WrestlerState.builder().wrestler(midFans).universe(defaultUniverse).fans(50L).build();
    s2 = wrestlerStateRepository.save(s2);
    midFans.getWrestlerStates().add(s2);

    Wrestler highFans =
        Wrestler.builder().name("High Fans").creationDate(java.time.Instant.now()).build();
    highFans = wrestlerRepository.save(highFans);
    WrestlerState s3 =
        WrestlerState.builder().wrestler(highFans).universe(defaultUniverse).fans(100L).build();
    s3 = wrestlerStateRepository.save(s3);
    highFans.getWrestlerStates().add(s3);

    assertThat(wrestlerRepository.findByFansBetween(40, 60))
        .extracting(Wrestler::getName)
        .containsExactly("Mid Fans");
    assertThat(wrestlerRepository.findByFansGreaterThanEqual(80))
        .extracting(Wrestler::getName)
        .containsExactly("High Fans");
  }
}

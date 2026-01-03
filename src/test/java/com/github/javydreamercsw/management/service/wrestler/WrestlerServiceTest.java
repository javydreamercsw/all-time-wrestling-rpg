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
package com.github.javydreamercsw.management.service.wrestler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundary;
import com.github.javydreamercsw.base.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpEvent;
import com.github.javydreamercsw.management.event.dto.WrestlerBumpHealedEvent;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class WrestlerServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private TierBoundaryRepository tierBoundaryRepository;

  @InjectMocks private WrestlerService wrestlerService;

  private Wrestler wrestler;
  private List<Wrestler> wrestlers;

  @Mock private DiceBag diceBag;

  @BeforeEach
  void setUp() {
    init();
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
    wrestler.setGender(Gender.MALE);
    wrestler.setActive(true);
  }

  private void init() {
    wrestlers =
        new ArrayList<>(
            List.of(
                Wrestler.builder()
                    .id(1L)
                    .name("Active Player")
                    .active(true)
                    .isPlayer(true)
                    .tier(WrestlerTier.MAIN_EVENTER)
                    .fans(1000L)
                    .build(),
                Wrestler.builder()
                    .id(2L)
                    .name("Active NPC")
                    .active(true)
                    .isPlayer(false)
                    .tier(WrestlerTier.MAIN_EVENTER)
                    .fans(900L)
                    .build(),
                Wrestler.builder()
                    .id(3L)
                    .name("Inactive Player")
                    .active(false)
                    .isPlayer(true)
                    .tier(WrestlerTier.MIDCARDER)
                    .fans(800L)
                    .build(),
                Wrestler.builder()
                    .id(4L)
                    .name("Inactive NPC")
                    .active(false)
                    .isPlayer(false)
                    .tier(WrestlerTier.MIDCARDER)
                    .fans(700L)
                    .build(),
                Wrestler.builder()
                    .id(5L)
                    .name("Active Midcarder")
                    .active(true)
                    .isPlayer(false)
                    .tier(WrestlerTier.MIDCARDER)
                    .fans(600L)
                    .build()));
    wrestlers.sort(Comparator.comparing(Wrestler::getFans).reversed());
  }

  @Test
  void testAddBump_PublishesEvent() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);

    // When
    wrestlerService.addBump(1L);

    // Then
    ArgumentCaptor<WrestlerBumpEvent> eventCaptor =
        ArgumentCaptor.forClass(WrestlerBumpEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
  }

  @Test
  void testHealChance_PublishesEvent() {
    // Given
    wrestler.setBumps(1);
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));
    when(wrestlerRepository.saveAndFlush(wrestler)).thenReturn(wrestler);
    when(diceBag.roll()).thenReturn(4); // Ensure bump is healed

    // When
    wrestlerService.healChance(1L, diceBag);

    // Then
    ArgumentCaptor<WrestlerBumpHealedEvent> eventCaptor =
        ArgumentCaptor.forClass(WrestlerBumpHealedEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
  }

  @Test
  void testRecalibrateFanCounts() {
    // Given
    WrestlerTier tier = WrestlerTier.CONTENDER;
    wrestler.setTier(tier);
    wrestler.setFans(500_000L);

    TierBoundary boundary = new TierBoundary();
    boundary.setTier(tier);
    boundary.setMinFans(40000L);
    boundary.setGender(Gender.MALE);

    when(wrestlerRepository.findAll()).thenReturn(Collections.singletonList(wrestler));
    when(tierBoundaryRepository.findAll()).thenReturn(Collections.singletonList(boundary));

    // When
    wrestlerService.recalibrateFanCounts();

    // Then
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Wrestler>> captor = ArgumentCaptor.forClass(List.class);
    verify(wrestlerRepository).saveAll(captor.capture());
    List<Wrestler> savedWrestlers = captor.getValue();
    assertEquals(1, savedWrestlers.size());
    assertEquals(40000L, savedWrestlers.get(0).getFans());
  }

  @Test
  void testRecalibrateFanCountsForIcon() {
    // Given
    wrestler.setTier(WrestlerTier.ICON);
    wrestler.setFans(1_000_000L);

    TierBoundary mainEventerBoundary = new TierBoundary();
    mainEventerBoundary.setTier(WrestlerTier.MAIN_EVENTER);
    mainEventerBoundary.setMinFans(500_000L);
    mainEventerBoundary.setGender(Gender.MALE);

    when(wrestlerRepository.findAll()).thenReturn(Collections.singletonList(wrestler));
    when(tierBoundaryRepository.findAll())
        .thenReturn(Collections.singletonList(mainEventerBoundary));

    // When
    wrestlerService.recalibrateFanCounts();

    // Then
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Wrestler>> captor = ArgumentCaptor.forClass(List.class);
    verify(wrestlerRepository).saveAll(captor.capture());

    List<Wrestler> savedWrestlers = captor.getValue();
    assertEquals(1, savedWrestlers.size());
    assertEquals(WrestlerTier.MAIN_EVENTER, savedWrestlers.get(0).getTier());
    assertEquals(500_000L, savedWrestlers.get(0).getFans());
  }

  @Test
  void testFindAll() {
    // Given
    when(wrestlerRepository.findAllByActiveTrue())
        .thenReturn(wrestlers.stream().filter(Wrestler::getActive).toList());

    // When
    List<Wrestler> result = wrestlerService.findAll();

    // Then
    assertEquals(3, result.size());
    result.forEach(w -> assertEquals(true, w.getActive()));
  }

  @Test
  void testFindAllIncludingInactive() {
    // Given
    when(wrestlerRepository.findAll(any(org.springframework.data.domain.Sort.class)))
        .thenReturn(wrestlers);

    // When
    List<Wrestler> result = wrestlerService.findAllIncludingInactive();

    // Then
    assertEquals(5, result.size());
  }

  @Test
  void testGetPlayerWrestlers() {
    // Given
    when(wrestlerRepository.findAllByActiveTrue())
        .thenReturn(wrestlers.stream().filter(Wrestler::getActive).toList());

    // When
    List<Wrestler> result = wrestlerService.getPlayerWrestlers();

    // Then
    assertEquals(1, result.size());
    assertEquals("Active Player", result.get(0).getName());
  }

  @Test
  void testGetNpcWrestlers() {
    // Given
    when(wrestlerRepository.findAllByActiveTrue())
        .thenReturn(wrestlers.stream().filter(Wrestler::getActive).toList());

    // When
    List<Wrestler> result = wrestlerService.getNpcWrestlers();

    // Then
    assertEquals(2, result.size());
    result.forEach(w -> assertEquals(false, w.getIsPlayer()));
  }

  @Test
  void testGetWrestlersByTier() {
    // Given
    when(wrestlerRepository.findAllByActiveTrue())
        .thenReturn(wrestlers.stream().filter(Wrestler::getActive).toList());

    // When
    List<Wrestler> result = wrestlerService.getWrestlersByTier(WrestlerTier.MAIN_EVENTER);

    // Then
    assertEquals(2, result.size());
    result.forEach(w -> assertEquals(WrestlerTier.MAIN_EVENTER, w.getTier()));
  }
}

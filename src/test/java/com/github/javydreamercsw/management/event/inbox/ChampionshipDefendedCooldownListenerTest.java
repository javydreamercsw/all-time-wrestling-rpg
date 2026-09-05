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
package com.github.javydreamercsw.management.event.inbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldown;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTitleCooldownRepository;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChampionshipDefendedCooldownListenerTest {

  private TitleRepository titleRepository;
  private WrestlerTitleCooldownRepository cooldownRepository;
  private WrestlerService wrestlerService;
  private ChampionshipDefendedCooldownListener listener;

  private Title title;
  private Wrestler challenger;
  private WrestlerState state;

  @BeforeEach
  void setUp() {
    titleRepository = mock(TitleRepository.class);
    cooldownRepository = mock(WrestlerTitleCooldownRepository.class);
    wrestlerService = mock(WrestlerService.class);
    listener =
        new ChampionshipDefendedCooldownListener(
            titleRepository, cooldownRepository, wrestlerService);

    title = new Title();
    title.setId(10L);
    title.setName("ATW Championship");
    title.setDefenseCount(5L); // primitive long
    when(titleRepository.findById(10L)).thenReturn(Optional.of(title));

    challenger = new Wrestler();
    challenger.setId(5L);
    challenger.setName("Challenger One");

    state = new WrestlerState();
    state.setId(77L);
    when(wrestlerService.getOrCreateState(anyLong(), anyLong())).thenReturn(state);
    // Default: the update-then-insert path finds no existing row.
    when(cooldownRepository.updateDefenseCount(anyLong(), anyLong(), anyLong())).thenReturn(0);
  }

  private ChampionshipDefendedEvent event(Wrestler... challengers) {
    return new ChampionshipDefendedEvent(this, title, List.of(), List.of(challengers));
  }

  @Test
  void emptyChallengerListIsNoOp() {
    listener.onApplicationEvent(event());

    verify(titleRepository, never()).incrementDefenseCount(anyLong());
    verify(cooldownRepository, never()).save(any());
  }

  @Test
  void missingTitleThrows() {
    when(titleRepository.findById(10L)).thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class, () -> listener.onApplicationEvent(event(challenger)));
  }

  @Test
  void firstTimeChallengerGetsCooldownRecord() {
    listener.onApplicationEvent(event(challenger));

    verify(titleRepository).incrementDefenseCount(10L);
    ArgumentCaptor<WrestlerTitleCooldown> captor =
        ArgumentCaptor.forClass(WrestlerTitleCooldown.class);
    verify(cooldownRepository).save(captor.capture());
    WrestlerTitleCooldown saved = captor.getValue();
    assertEquals(state, saved.getWrestlerState());
    assertEquals(title, saved.getTitle());
    assertEquals(5L, saved.getDefenseCountAtChallenge());
  }

  @Test
  void returningChallengerUpdatesExistingRowWithoutInsert() {
    when(cooldownRepository.updateDefenseCount(77L, 10L, 5L)).thenReturn(1);

    listener.onApplicationEvent(event(challenger));

    verify(cooldownRepository).updateDefenseCount(77L, 10L, 5L);
    verify(cooldownRepository, never()).save(any());
  }

  @Test
  void multipleChallengersEachGetACooldown() {
    Wrestler second = new Wrestler();
    second.setId(6L);
    second.setName("Challenger Two");

    listener.onApplicationEvent(event(challenger, second));

    verify(cooldownRepository, org.mockito.Mockito.times(2)).save(any());
  }

  @Test
  void snapshotIsTakenAfterIncrement() {
    // The reload after increment must observe the bumped defense count.
    title.setDefenseCount(9L);
    when(cooldownRepository.updateDefenseCount(anyLong(), anyLong(), anyLong()))
        .thenReturn(1); // rows exist; update path taken for snapshot assertion below

    listener.onApplicationEvent(event(challenger));

    // Reload happened twice: once for the title, once post-increment.
    verify(titleRepository, org.mockito.Mockito.times(2)).findById(10L);
    verify(cooldownRepository).updateDefenseCount(77L, 10L, 9L);
    assertTrue(title.getDefenseCount() == 9L);
  }
}

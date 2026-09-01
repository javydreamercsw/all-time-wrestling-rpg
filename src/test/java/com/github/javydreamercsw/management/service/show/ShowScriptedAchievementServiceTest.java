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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.event.dto.ShowFinalizedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.achievement.ScriptedAchievementEvaluator;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowScriptedAchievementServiceTest {

  @Mock private ScriptedAchievementEvaluator evaluator;
  @Mock private LegacyService legacyService;
  @Mock private GameSettingService gameSettingService;

  private ShowScriptedAchievementService service;

  private Account account;
  private Wrestler wrestler;
  private Show show;

  @BeforeEach
  void setUp() {
    when(gameSettingService.getCurrentGameDate()).thenReturn(LocalDate.now());
    service = new ShowScriptedAchievementService(evaluator, legacyService, gameSettingService);

    account = new Account();
    account.setId(1L);
    account.setAchievements(Set.of());

    wrestler = mock(Wrestler.class);
    when(wrestler.getAccount()).thenReturn(account);

    show = new Show();
    when(evaluator.resolveNewlyUnlockedKeys(any(), any())).thenReturn(List.of());
  }

  @Test
  void onShowFinalized_withEmptySegments_doesNothing() {
    service.onShowFinalized(new ShowFinalizedEvent(this, show, List.of()));

    verify(evaluator, never()).resolveNewlyUnlockedKeys(any(), any());
  }

  @Test
  void onShowFinalized_withNoAccountsOnWrestlers_doesNothing() {
    Wrestler npc = mock(Wrestler.class);
    when(npc.getAccount()).thenReturn(null);
    Segment seg = matchSegment(List.of(npc), false);

    service.onShowFinalized(new ShowFinalizedEvent(this, show, List.of(seg)));

    verify(evaluator, never()).resolveNewlyUnlockedKeys(any(), any());
  }

  @Test
  void onShowFinalized_callsEvaluatorForEachAccount() {
    Account account2 = new Account();
    account2.setId(2L);
    account2.setAchievements(Set.of());
    Wrestler wrestler2 = mock(Wrestler.class);
    when(wrestler2.getAccount()).thenReturn(account2);

    Segment seg = matchSegment(List.of(wrestler, wrestler2), true);

    service.onShowFinalized(new ShowFinalizedEvent(this, show, List.of(seg)));

    verify(evaluator).resolveNewlyUnlockedKeys(eq(account), any());
    verify(evaluator).resolveNewlyUnlockedKeys(eq(account2), any());
  }

  @Test
  void onShowFinalized_mainEventDetectedByIsMainEventFlag() {
    Segment first = matchSegment(List.of(wrestler), false);
    Segment mainEvent = matchSegment(List.of(wrestler), true);

    service.onShowFinalized(new ShowFinalizedEvent(this, show, List.of(first, mainEvent)));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(evaluator).resolveNewlyUnlockedKeys(eq(account), captor.capture());
    List<Wrestler> mainEventParticipants =
        (List<Wrestler>) captor.getValue().get("mainEventParticipants");
    assertThat(mainEventParticipants).containsExactly(wrestler);
  }

  @Test
  void onShowFinalized_fallsBackToLastSegmentWhenNoMainEventFlagged() {
    Wrestler otherWrestler = mock(Wrestler.class);
    when(otherWrestler.getAccount()).thenReturn(account);
    Segment first = matchSegment(List.of(wrestler), false);
    Segment last = matchSegment(List.of(otherWrestler), false);

    service.onShowFinalized(new ShowFinalizedEvent(this, show, List.of(first, last)));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(evaluator).resolveNewlyUnlockedKeys(eq(account), captor.capture());
    List<Wrestler> mainEventParticipants =
        (List<Wrestler>) captor.getValue().get("mainEventParticipants");
    assertThat(mainEventParticipants).containsExactly(otherWrestler);
  }

  @Test
  void onShowFinalized_collectsPromoParticipantsFromPromoSegmentsOnly() {
    Wrestler promoWrestler = mock(Wrestler.class);
    when(promoWrestler.getAccount()).thenReturn(account);
    Segment match = matchSegment(List.of(wrestler), true);
    Segment promo = promoSegment(List.of(promoWrestler));

    service.onShowFinalized(new ShowFinalizedEvent(this, show, List.of(promo, match)));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(evaluator).resolveNewlyUnlockedKeys(eq(account), captor.capture());
    Map<String, Object> ctx = captor.getValue();
    List<Wrestler> promoParticipants = (List<Wrestler>) ctx.get("promoParticipants");
    assertThat(promoParticipants).containsExactly(promoWrestler);
    List<Segment> promoSegments = (List<Segment>) ctx.get("promoSegments");
    assertThat(promoSegments).containsExactly(promo);
  }

  @Test
  void onShowFinalized_scopesParticipantsToTheAccountsOwnWrestlers() {
    // Johnny (account 2) cuts a promo; account 1 also has a wrestler on the show.
    // Account 1's context must NOT contain Johnny — otherwise wrestler-specific
    // achievements (e.g. Mayor of Slamtown) unlock for everyone on the card.
    // Account.equals() compares usernames, so both need distinct ones to stay
    // separate entries in the service's account set.
    account.setUsername("player1");
    Account account2 = new Account();
    account2.setId(2L);
    account2.setUsername("player2");
    account2.setAchievements(Set.of());
    Wrestler johnny = mock(Wrestler.class);
    when(johnny.getAccount()).thenReturn(account2);

    Segment match = matchSegment(List.of(wrestler, johnny), true);
    Segment promo = promoSegment(List.of(johnny));

    service.onShowFinalized(new ShowFinalizedEvent(this, show, List.of(promo, match)));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(evaluator).resolveNewlyUnlockedKeys(eq(account), captor.capture());
    Map<String, Object> account1Ctx = captor.getValue();
    assertThat((List<Wrestler>) account1Ctx.get("promoParticipants")).isEmpty();
    assertThat((List<Wrestler>) account1Ctx.get("mainEventParticipants")).containsExactly(wrestler);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor2 = ArgumentCaptor.forClass(Map.class);
    verify(evaluator).resolveNewlyUnlockedKeys(eq(account2), captor2.capture());
    Map<String, Object> account2Ctx = captor2.getValue();
    assertThat((List<Wrestler>) account2Ctx.get("promoParticipants")).containsExactly(johnny);
    assertThat((List<Wrestler>) account2Ctx.get("mainEventParticipants")).containsExactly(johnny);
  }

  @Test
  void onShowFinalized_unlocksAchievementsReturnedByEvaluator() {
    when(evaluator.resolveNewlyUnlockedKeys(eq(account), any()))
        .thenReturn(List.of("RVD_WHOLE_DAMN_SHOW"));
    Segment seg = matchSegment(List.of(wrestler), true);

    service.onShowFinalized(new ShowFinalizedEvent(this, show, List.of(seg)));

    verify(legacyService).unlockAchievement(account, "RVD_WHOLE_DAMN_SHOW");
  }

  // ─── helpers ────────────────────────────────────────────────────────────────

  private Segment matchSegment(final List<Wrestler> wrestlers, final boolean isMainEvent) {
    Segment seg = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    when(type.getCode()).thenReturn(WellKnownSegmentType.ONE_ON_ONE.getCode());
    when(seg.getSegmentType()).thenReturn(type);
    when(seg.getWrestlers()).thenReturn(wrestlers);
    when(seg.isMainEvent()).thenReturn(isMainEvent);
    return seg;
  }

  private Segment promoSegment(final List<Wrestler> wrestlers) {
    Segment seg = mock(Segment.class);
    SegmentType type = mock(SegmentType.class);
    when(type.getCode()).thenReturn(WellKnownSegmentType.PROMO.getCode());
    when(seg.getSegmentType()).thenReturn(type);
    when(seg.getWrestlers()).thenReturn(wrestlers);
    when(seg.isMainEvent()).thenReturn(false);
    return seg;
  }
}

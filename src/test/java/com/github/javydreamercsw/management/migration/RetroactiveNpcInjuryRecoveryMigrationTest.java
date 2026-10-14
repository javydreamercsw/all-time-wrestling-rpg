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
package com.github.javydreamercsw.management.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameClock;
import com.github.javydreamercsw.utils.DiceBag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for the ATW-3qug one-time repair: retroactive NPC injury recovery. Dice outcomes are
 * driven by a deterministic DiceBag injected through the package-private {@code recoveryDiceBag}
 * hook — real dice logic, controlled faces.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetroactiveNpcInjuryRecoveryMigrationTest {

  @Mock private InjuryRepository injuryRepository;
  @Mock private ShowRepository showRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private CampaignRepository campaignRepository;
  @Mock private GameClock gameClock;

  @Spy @InjectMocks private RetroactiveNpcInjuryRecoveryMigration migration;

  private Universe universe;
  private Wrestler wrestler;
  private Injury injury;

  @BeforeEach
  void setUp() {
    universe = new Universe();
    universe.setId(1L);
    wrestler = new Wrestler();
    wrestler.setId(9L);
    wrestler.setName("The British Bulldog");
    wrestler.setResilience(3);
    injury = new Injury();
    injury.setId(292L);
    injury.setWrestler(wrestler);
    injury.setUniverse(universe);
    injury.setInjuryDate(LocalDate.of(2026, 6, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    // Default: no campaign anywhere, no missed shows
    lenient().when(campaignRepository.existsByUniverse(any())).thenReturn(false);
    lenient().when(showRepository.findAll()).thenReturn(List.of());
    lenient().when(injuryRepository.findHealableInjuries()).thenReturn(List.of(injury));
    lenient().when(gameClock.gameNowStaggeredOn(any())).thenReturn(Instant.now());
  }

  @Test
  void migrate_wrestlerRestedOnShowsSinceInjury_luckyRollsHealInjuryOnFirstShow() {
    // Given — all dice show 6: 3 successes on the first replayed show heals the injury
    Show show1 = show(383L, LocalDate.of(2026, 7, 13));
    Show show2 = show(333L, LocalDate.of(2026, 7, 17));
    when(showRepository.findAll()).thenReturn(List.of(show1, show2));
    when(segmentRepository.findNonPromoParticipationAfter(wrestler, LocalDate.of(2026, 6, 1)))
        .thenReturn(List.of());
    Instant expectedHealDate = show1.getShowDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    when(gameClock.gameNowStaggeredOn(show1.getShowDate())).thenReturn(expectedHealDate);
    allDiceShow(6);

    migration.migrate();

    verify(injuryRepository).save(injury);
    assertThat(injury.getHealedDate()).isEqualTo(expectedHealDate);
  }

  @Test
  void migrate_allDiceFail_injuryStaysActive() {
    // Given — every die shows 2: zero successes on every replayed show
    Show show1 = show(383L, LocalDate.of(2026, 7, 13));
    Show show2 = show(333L, LocalDate.of(2026, 7, 17));
    Show show3 = show(295L, LocalDate.of(2026, 7, 26));
    when(showRepository.findAll()).thenReturn(List.of(show1, show2, show3));
    when(segmentRepository.findNonPromoParticipationAfter(wrestler, LocalDate.of(2026, 6, 1)))
        .thenReturn(List.of());
    allDiceShow(2);

    migration.migrate();

    verify(injuryRepository, never()).save(any());
    assertThat(injury.getHealedDate()).isNull();
  }

  @Test
  void migrate_oneSuccessPerShow_bumpOnlyOutcome_neverHealsInjury() {
    // Given — every die shows 3: zero successes per roll (1-success outcomes remove bumps only,
    // which the migration deliberately does not replay, so the injury must stay untouched)
    Show show1 = show(383L, LocalDate.of(2026, 7, 13));
    Show show2 = show(333L, LocalDate.of(2026, 7, 17));
    when(showRepository.findAll()).thenReturn(List.of(show1, show2));
    when(segmentRepository.findNonPromoParticipationAfter(wrestler, LocalDate.of(2026, 6, 1)))
        .thenReturn(List.of());
    allDiceShow(3);

    migration.migrate();

    verify(injuryRepository, never()).save(any());
    assertThat(injury.getHealedDate()).isNull();
  }

  @Test
  void migrate_wrestlerParticipatedAfterInjury_onlyRestedShowsCount() {
    // Given — the wrestler worked a non-promo segment on the later show (not resting there)
    Show show1 = show(383L, LocalDate.of(2026, 7, 13));
    Show show2 = show(333L, LocalDate.of(2026, 7, 17));
    when(showRepository.findAll()).thenReturn(List.of(show1, show2));

    Segment workedSegment = new Segment();
    workedSegment.setShow(show2);
    when(segmentRepository.findNonPromoParticipationAfter(wrestler, LocalDate.of(2026, 6, 1)))
        .thenReturn(List.of(workedSegment));

    Instant expectedHealDate = show1.getShowDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    when(gameClock.gameNowStaggeredOn(show1.getShowDate())).thenReturn(expectedHealDate);
    allDiceShow(6);

    migration.migrate();

    verify(injuryRepository, times(1)).save(injury);
    assertThat(injury.getHealedDate()).isEqualTo(expectedHealDate);
  }

  @Test
  void migrate_campaignUniverse_showsSkipped() {
    // Given — the show belongs to a campaign universe (RECOVERY backstage actions govern those)
    Show show1 = show(383L, LocalDate.of(2026, 7, 13));
    when(showRepository.findAll()).thenReturn(List.of(show1));
    when(segmentRepository.findNonPromoParticipationAfter(wrestler, LocalDate.of(2026, 6, 1)))
        .thenReturn(List.of());
    when(campaignRepository.existsByUniverse(show1.getUniverse())).thenReturn(true);
    allDiceShow(6);

    migration.migrate();

    verify(injuryRepository, never()).save(any());
    assertThat(injury.getHealedDate()).isNull();
  }

  @Test
  void migrate_noActiveInjuries_nothingRuns() {
    when(injuryRepository.findHealableInjuries()).thenReturn(List.of());

    migration.migrate();

    verify(injuryRepository, never()).save(any());
    verify(showRepository, never()).findAll();
  }

  @Test
  void migrate_injuryDateAfterAllShows_noReplay() {
    // Given — injury is newer than every show; no missed opportunities
    injury.setInjuryDate(LocalDate.of(2026, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
    Show show1 = show(383L, LocalDate.of(2026, 7, 13));
    when(showRepository.findAll()).thenReturn(List.of(show1));
    when(segmentRepository.findNonPromoParticipationAfter(wrestler, LocalDate.of(2026, 8, 1)))
        .thenReturn(List.of());
    allDiceShow(6);

    migration.migrate();

    verify(injuryRepository, never()).save(any());
    assertThat(injury.getHealedDate()).isNull();
  }

  @Test
  void migrate_usesInjuryDateAsReplayStart() {
    // Argument sanity: the participation scan starts from the injury's own date
    Show show1 = show(383L, LocalDate.of(2026, 7, 13));
    when(showRepository.findAll()).thenReturn(List.of(show1));
    when(segmentRepository.findNonPromoParticipationAfter(any(), any())).thenReturn(List.of());
    allDiceShow(6);

    migration.migrate();

    ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    verify(segmentRepository)
        .findNonPromoParticipationAfter(any(Wrestler.class), dateCaptor.capture());
    assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.of(2026, 6, 1));
  }

  @Test
  void migrate_healedInjuryStopsReplayingShowsForIt() {
    // Given — two shows; the injury heals on the first (all-6 roll); the second show must NOT
    // trigger another heal attempt (heal stamps would overwrite; contract says stop)
    Show show1 = show(383L, LocalDate.of(2026, 7, 13));
    Show show2 = show(333L, LocalDate.of(2026, 7, 17));
    when(showRepository.findAll()).thenReturn(List.of(show1, show2));
    when(segmentRepository.findNonPromoParticipationAfter(wrestler, LocalDate.of(2026, 6, 1)))
        .thenReturn(List.of());
    Instant firstShowHeal = show1.getShowDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    when(gameClock.gameNowStaggeredOn(show1.getShowDate())).thenReturn(firstShowHeal);
    allDiceShow(6);

    migration.migrate();

    // The heal date must be the FIRST missed show's date, not the second's
    assertThat(injury.getHealedDate()).isEqualTo(firstShowHeal);
    verify(injuryRepository, times(1)).save(injury);
  }

  /** Rigs every future recovery roll to show the given face on all dice. */
  private void allDiceShow(int face) {
    lenient()
        .when(migration.recoveryDiceBag(ArgumentMatchers.anyInt()))
        .thenAnswer(
            invocation -> {
              int diceCount = invocation.getArgument(0);
              int[] dice = new int[Math.max(1, diceCount)];
              Arrays.fill(dice, face);
              // DiceBag uses random.nextInt(6)+1; a stub Random always yielding face-1
              // produces exactly that face on every die.
              return new DiceBag(
                  new Random() {
                    @Override
                    public int nextInt(int bound) {
                      return face - 1;
                    }
                  },
                  dice);
            });
  }

  /** A show with the given id, date, and the shared universe. */
  private Show show(long id, LocalDate date) {
    Show show = new Show();
    show.setId(id);
    show.setName("Show " + id);
    show.setShowDate(date);
    show.setUniverse(universe);
    return show;
  }
}

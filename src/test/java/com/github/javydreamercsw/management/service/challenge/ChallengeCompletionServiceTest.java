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
package com.github.javydreamercsw.management.service.challenge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.campaign.Difficulty;
import com.github.javydreamercsw.management.domain.challenge.AccountChallengeCompletion;
import com.github.javydreamercsw.management.domain.challenge.AccountChallengeCompletionRepository;
import com.github.javydreamercsw.management.domain.challenge.ChallengeCompletionStatus;
import com.github.javydreamercsw.management.dto.challenge.ChallengeDTO;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChallengeCompletionServiceTest {

  @Mock private AccountChallengeCompletionRepository repository;
  @Mock private ChallengeService challengeService;
  @Mock private LegacyService legacyService;

  private ChallengeCompletionService service;
  private Account account;

  @BeforeEach
  void setUp() {
    service = new ChallengeCompletionService(repository, challengeService, legacyService);
    account = new Account();
  }

  // ── seasonAchievementKey ──────────────────────────────────────────────────

  @Test
  void seasonAchievementKey_season1() {
    assertEquals(
        "CHALLENGE_SEASON_1_COMPLETE", ChallengeCompletionService.seasonAchievementKey("Season 1"));
  }

  @Test
  void seasonAchievementKey_season12() {
    assertEquals(
        "CHALLENGE_SEASON_12_COMPLETE",
        ChallengeCompletionService.seasonAchievementKey("Season 12"));
  }

  // ── markComplete — first completion ──────────────────────────────────────

  @Test
  void markComplete_firstTime_setsCompletedAt() {
    ChallengeDTO challenge = entryChallenge("ch1", "ACH_CH1", "Season 1");
    when(challengeService.getChallenge("ch1")).thenReturn(Optional.of(challenge));
    // getAllChallenges() is only called for HARD; getActiveChallenges() is called for season check
    when(challengeService.getActiveChallenges()).thenReturn(List.of(challenge));
    when(repository.findByAccountAndChallengeId(account, "ch1")).thenReturn(Optional.empty());

    AccountChallengeCompletion saved =
        completionFor(account, "ch1", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now());
    when(repository.save(any())).thenReturn(saved);
    when(repository.findByAccount(account)).thenReturn(List.of(saved));
    when(repository.findByAccountAndStatus(account, ChallengeCompletionStatus.COMPLETED))
        .thenReturn(List.of(saved));

    AccountChallengeCompletion result = service.markComplete(account, "ch1", "notes", null);

    assertNotNull(result.getCompletedAt());
    verify(legacyService).unlockAchievement(account, "ACH_CH1");
  }

  @Test
  void markComplete_editSave_doesNotCheckAchievements() {
    AccountChallengeCompletion existing =
        completionFor(
            account, "ch1", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now().minusDays(1));
    when(repository.findByAccountAndChallengeId(account, "ch1")).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenReturn(existing);

    service.markComplete(account, "ch1", "updated notes", null);

    verify(legacyService, never()).unlockAchievement(any(), any());
    verify(challengeService, never()).getChallenge(any());
  }

  @Test
  void markComplete_editSave_preservesOriginalCompletedAt() {
    LocalDateTime original = LocalDateTime.now().minusDays(3);
    AccountChallengeCompletion existing =
        completionFor(account, "ch1", ChallengeCompletionStatus.COMPLETED, original);
    when(repository.findByAccountAndChallengeId(account, "ch1")).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenReturn(existing);

    service.markComplete(account, "ch1", "new notes", null);

    assertEquals(original, existing.getCompletedAt());
  }

  // ── Per-challenge achievement ─────────────────────────────────────────────

  @Test
  void markComplete_noAchievementKey_noPerChallengeUnlock() {
    ChallengeDTO challenge = entryChallenge("ch1", null, "Season 1");
    when(challengeService.getChallenge("ch1")).thenReturn(Optional.of(challenge));
    when(challengeService.getActiveChallenges()).thenReturn(List.of(challenge));
    when(repository.findByAccountAndChallengeId(account, "ch1")).thenReturn(Optional.empty());

    AccountChallengeCompletion saved =
        completionFor(account, "ch1", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now());
    when(repository.save(any())).thenReturn(saved);
    when(repository.findByAccount(account)).thenReturn(List.of(saved));
    when(repository.findByAccountAndStatus(account, ChallengeCompletionStatus.COMPLETED))
        .thenReturn(List.of(saved));

    service.markComplete(account, "ch1", null, null);

    verify(legacyService, never()).unlockAchievement(eq(account), eq("ACH_CH1"));
  }

  // ── HARD milestone ────────────────────────────────────────────────────────

  @Test
  void markComplete_firstHard_unlocksHardMilestone() {
    ChallengeDTO hard = hardChallenge("hard1", "ACH_HARD1", "Season 1");
    when(challengeService.getChallenge("hard1")).thenReturn(Optional.of(hard));
    when(challengeService.getAllChallenges()).thenReturn(List.of(hard));
    when(challengeService.getActiveChallenges()).thenReturn(List.of(hard));
    when(repository.findByAccountAndChallengeId(account, "hard1")).thenReturn(Optional.empty());

    AccountChallengeCompletion saved =
        completionFor(account, "hard1", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now());
    when(repository.save(any())).thenReturn(saved);
    when(repository.findByAccount(account)).thenReturn(List.of(saved));
    when(repository.findByAccountAndStatus(account, ChallengeCompletionStatus.COMPLETED))
        .thenReturn(List.of(saved));

    service.markComplete(account, "hard1", null, null);

    verify(legacyService).unlockAchievement(account, "CHALLENGE_FIRST_HARD");
  }

  @Test
  void markComplete_secondHard_doesNotUnlockHardMilestoneAgain() {
    ChallengeDTO hard1 = hardChallenge("hard1", "ACH_H1", "Season 1");
    ChallengeDTO hard2 = hardChallenge("hard2", "ACH_H2", "Season 1");
    when(challengeService.getChallenge("hard2")).thenReturn(Optional.of(hard2));
    when(challengeService.getAllChallenges()).thenReturn(List.of(hard1, hard2));
    when(challengeService.getActiveChallenges()).thenReturn(List.of(hard1, hard2));
    when(repository.findByAccountAndChallengeId(account, "hard2")).thenReturn(Optional.empty());

    AccountChallengeCompletion prev =
        completionFor(
            account,
            "hard1",
            ChallengeCompletionStatus.COMPLETED,
            LocalDateTime.now().minusDays(1));
    AccountChallengeCompletion saved =
        completionFor(account, "hard2", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now());
    when(repository.save(any())).thenReturn(saved);
    when(repository.findByAccount(account)).thenReturn(List.of(prev, saved));
    when(repository.findByAccountAndStatus(account, ChallengeCompletionStatus.COMPLETED))
        .thenReturn(List.of(prev, saved));

    service.markComplete(account, "hard2", null, null);

    // unlockAchievement is still called by legacyService — it's idempotent — but hardCount > 1
    // so CHALLENGE_FIRST_HARD should NOT be called
    verify(legacyService, never()).unlockAchievement(account, "CHALLENGE_FIRST_HARD");
  }

  // ── Season milestone ──────────────────────────────────────────────────────

  @Test
  void markComplete_completesAllSeasonChallenges_unlocksSeasonAchievement() {
    ChallengeDTO ch1 = entryChallenge("ch1", "ACH_1", "Season 1");
    ChallengeDTO ch2 = entryChallenge("ch2", "ACH_2", "Season 1");
    when(challengeService.getChallenge("ch2")).thenReturn(Optional.of(ch2));
    when(challengeService.getActiveChallenges()).thenReturn(List.of(ch1, ch2));
    when(repository.findByAccountAndChallengeId(account, "ch2")).thenReturn(Optional.empty());

    AccountChallengeCompletion prev =
        completionFor(
            account, "ch1", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now().minusDays(1));
    AccountChallengeCompletion saved =
        completionFor(account, "ch2", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now());
    when(repository.save(any())).thenReturn(saved);
    when(repository.findByAccount(account)).thenReturn(List.of(prev, saved));
    when(repository.findByAccountAndStatus(account, ChallengeCompletionStatus.COMPLETED))
        .thenReturn(List.of(prev, saved));

    service.markComplete(account, "ch2", null, null);

    verify(legacyService).unlockAchievement(account, "CHALLENGE_SEASON_1_COMPLETE");
  }

  @Test
  void markComplete_seasonIncomplete_doesNotUnlockSeasonAchievement() {
    ChallengeDTO ch1 = entryChallenge("ch1", "ACH_1", "Season 1");
    ChallengeDTO ch2 = entryChallenge("ch2", "ACH_2", "Season 1");
    when(challengeService.getChallenge("ch1")).thenReturn(Optional.of(ch1));
    when(challengeService.getActiveChallenges()).thenReturn(List.of(ch1, ch2));
    when(repository.findByAccountAndChallengeId(account, "ch1")).thenReturn(Optional.empty());

    AccountChallengeCompletion saved =
        completionFor(account, "ch1", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now());
    when(repository.save(any())).thenReturn(saved);
    when(repository.findByAccount(account)).thenReturn(List.of(saved));
    when(repository.findByAccountAndStatus(account, ChallengeCompletionStatus.COMPLETED))
        .thenReturn(List.of(saved));

    service.markComplete(account, "ch1", null, null);

    verify(legacyService, never()).unlockAchievement(account, "CHALLENGE_SEASON_1_COMPLETE");
  }

  // ── Cumulative count milestones ───────────────────────────────────────────

  @Test
  void markComplete_5thCompletion_unlocks5Milestone() {
    ChallengeDTO challenge = entryChallenge("ch5", null, null);
    when(challengeService.getChallenge("ch5")).thenReturn(Optional.of(challenge));
    // season is null → getActiveChallenges() not called; ENTRY → getAllChallenges() not called
    when(repository.findByAccountAndChallengeId(account, "ch5")).thenReturn(Optional.empty());

    List<AccountChallengeCompletion> fiveCompletions =
        List.of(
            completionFor(account, "ch1", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "ch2", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "ch3", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "ch4", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(
                account, "ch5", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()));
    AccountChallengeCompletion saved = fiveCompletions.get(4);
    when(repository.save(any())).thenReturn(saved);
    when(repository.findByAccount(account)).thenReturn(fiveCompletions);

    service.markComplete(account, "ch5", null, null);

    verify(legacyService).unlockAchievement(account, "CHALLENGE_5_COMPLETE");
    verify(legacyService, never()).unlockAchievement(account, "CHALLENGE_10_COMPLETE");
  }

  @Test
  void markComplete_10thCompletion_unlocksBothCountMilestones() {
    ChallengeDTO challenge = entryChallenge("ch10", null, null);
    when(challengeService.getChallenge("ch10")).thenReturn(Optional.of(challenge));
    when(repository.findByAccountAndChallengeId(account, "ch10")).thenReturn(Optional.empty());

    List<AccountChallengeCompletion> tenCompletions =
        List.of(
            completionFor(account, "c1", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "c2", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "c3", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "c4", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "c5", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "c6", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "c7", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "c8", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(account, "c9", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()),
            completionFor(
                account, "ch10", ChallengeCompletionStatus.COMPLETED, LocalDateTime.now()));
    when(repository.save(any())).thenReturn(tenCompletions.get(9));
    when(repository.findByAccount(account)).thenReturn(tenCompletions);

    service.markComplete(account, "ch10", null, null);

    verify(legacyService).unlockAchievement(account, "CHALLENGE_5_COMPLETE");
    verify(legacyService).unlockAchievement(account, "CHALLENGE_10_COMPLETE");
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static ChallengeDTO entryChallenge(
      final String id, final String achievementKey, final String season) {
    return ChallengeDTO.builder()
        .id(id)
        .title("Challenge " + id)
        .difficulty(Difficulty.ENTRY)
        .achievementKey(achievementKey)
        .season(season)
        .active(true)
        .build();
  }

  private static ChallengeDTO hardChallenge(
      final String id, final String achievementKey, final String season) {
    return ChallengeDTO.builder()
        .id(id)
        .title("Hard " + id)
        .difficulty(Difficulty.HARD)
        .achievementKey(achievementKey)
        .season(season)
        .active(true)
        .build();
  }

  private static AccountChallengeCompletion completionFor(
      final Account account,
      final String challengeId,
      final ChallengeCompletionStatus status,
      final LocalDateTime completedAt) {
    AccountChallengeCompletion c = new AccountChallengeCompletion();
    c.setAccount(account);
    c.setChallengeId(challengeId);
    c.setStatus(status);
    c.setCompletedAt(completedAt);
    return c;
  }
}

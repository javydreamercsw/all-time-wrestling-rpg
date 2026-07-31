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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.campaign.Difficulty;
import com.github.javydreamercsw.management.domain.challenge.AccountChallengeCompletion;
import com.github.javydreamercsw.management.domain.challenge.AccountChallengeCompletionRepository;
import com.github.javydreamercsw.management.domain.challenge.ChallengeCompletionStatus;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChallengeCompletionService {

  private final AccountChallengeCompletionRepository repository;
  private final ChallengeService challengeService;
  private final LegacyService legacyService;

  @Transactional(readOnly = true)
  public Optional<AccountChallengeCompletion> find(
      final Account account, final String challengeId) {
    return repository.findByAccountAndChallengeId(account, challengeId);
  }

  @Transactional(readOnly = true)
  public boolean isCompleted(final Account account, final String challengeId) {
    return repository
        .findByAccountAndChallengeId(account, challengeId)
        .map(c -> ChallengeCompletionStatus.COMPLETED == c.getStatus())
        .orElse(false);
  }

  public AccountChallengeCompletion markComplete(
      final Account account,
      final String challengeId,
      final String playerNotes,
      final String proofImageUrl) {
    AccountChallengeCompletion completion =
        repository
            .findByAccountAndChallengeId(account, challengeId)
            .orElseGet(
                () -> {
                  AccountChallengeCompletion c = new AccountChallengeCompletion();
                  c.setAccount(account);
                  c.setChallengeId(challengeId);
                  return c;
                });

    boolean firstCompletion = completion.getCompletedAt() == null;

    completion.setStatus(ChallengeCompletionStatus.COMPLETED);
    if (firstCompletion) {
      completion.setCompletedAt(LocalDateTime.now());
    }
    completion.setPlayerNotes(playerNotes);
    completion.setProofImageUrl(proofImageUrl);
    repository.save(completion);

    if (firstCompletion) {
      checkAchievements(account, challengeId);
    }

    return completion;
  }

  @Transactional(readOnly = true)
  public List<AccountChallengeCompletion> getCompletions(final Account account) {
    return repository.findByAccount(account);
  }

  private void checkAchievements(final Account account, final String challengeId) {
    challengeService
        .getChallenge(challengeId)
        .ifPresent(
            challenge -> {
              // Per-challenge achievement
              if (challenge.getAchievementKey() != null
                  && !challenge.getAchievementKey().isBlank()) {
                legacyService.unlockAchievement(account, challenge.getAchievementKey());
              }

              // First HARD difficulty completion
              if (Difficulty.HARD == challenge.getDifficulty()) {
                Set<String> completedIds = completedChallengeIds(account);
                long hardCount =
                    challengeService.getAllChallenges().stream()
                        .filter(c -> Difficulty.HARD == c.getDifficulty())
                        .filter(c -> completedIds.contains(c.getId()))
                        .count();
                if (hardCount == 1) {
                  legacyService.unlockAchievement(account, "CHALLENGE_FIRST_HARD");
                }
              }

              // Season completion
              String season = challenge.getSeason();
              if (season != null && !season.isBlank()) {
                Set<String> completedIds = completedChallengeIds(account);
                boolean seasonDone =
                    challengeService.getActiveChallenges().stream()
                        .filter(c -> season.equals(c.getSeason()))
                        .allMatch(c -> completedIds.contains(c.getId()));
                if (seasonDone) {
                  String seasonKey = seasonAchievementKey(season);
                  legacyService.unlockAchievement(account, seasonKey);
                }
              }

              // Cumulative count milestones
              long total =
                  repository.findByAccount(account).stream()
                      .filter(c -> ChallengeCompletionStatus.COMPLETED == c.getStatus())
                      .count();
              if (total >= 10) {
                legacyService.unlockAchievement(account, "CHALLENGE_10_COMPLETE");
              }
              if (total >= 5) {
                legacyService.unlockAchievement(account, "CHALLENGE_5_COMPLETE");
              }
            });
  }

  private Set<String> completedChallengeIds(final Account account) {
    return repository.findByAccountAndStatus(account, ChallengeCompletionStatus.COMPLETED).stream()
        .map(AccountChallengeCompletion::getChallengeId)
        .collect(Collectors.toSet());
  }

  /** Derives the season achievement key from the season display label. */
  static String seasonAchievementKey(final String season) {
    // "Season 1" → "CHALLENGE_S1_COMPLETE", "Season 12" → "CHALLENGE_S12_COMPLETE"
    return "CHALLENGE_" + season.toUpperCase().replace(" ", "_") + "_COMPLETE";
  }
}

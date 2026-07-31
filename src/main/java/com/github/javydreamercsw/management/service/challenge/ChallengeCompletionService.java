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
import com.github.javydreamercsw.management.domain.challenge.AccountChallengeCompletion;
import com.github.javydreamercsw.management.domain.challenge.AccountChallengeCompletionRepository;
import com.github.javydreamercsw.management.domain.challenge.ChallengeCompletionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeCompletionService {

  private final AccountChallengeCompletionRepository repository;

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

    completion.setStatus(ChallengeCompletionStatus.COMPLETED);
    if (completion.getCompletedAt() == null) {
      completion.setCompletedAt(LocalDateTime.now());
    }
    completion.setPlayerNotes(playerNotes);
    completion.setProofImageUrl(proofImageUrl);
    return repository.save(completion);
  }

  @Transactional(readOnly = true)
  public List<AccountChallengeCompletion> getCompletions(final Account account) {
    return repository.findByAccount(account);
  }
}

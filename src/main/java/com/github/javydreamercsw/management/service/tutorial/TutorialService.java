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
package com.github.javydreamercsw.management.service.tutorial;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.tutorial.AccountTutorialCompletion;
import com.github.javydreamercsw.management.domain.tutorial.AccountTutorialCompletionRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseMembership;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseMembershipService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorialService {

  private final AccountTutorialCompletionRepository completionRepository;
  private final GameSettingService gameSettingService;
  private final AccountRepository accountRepository;
  private final List<TutorialDefinition> definitions;
  private final UniverseService universeService;
  private final UniverseMembershipService universeMembershipService;
  private final UniverseContextService universeContextService;

  /**
   * Returns the tutorial universe for the given player username if one was previously created, or
   * empty if the player has not yet completed the mode-selection/setup step.
   */
  @PreAuthorize("permitAll()")
  public java.util.Optional<Universe> findTutorialUniverse(@NonNull final String username) {
    return universeService.findByName("Tutorial – " + username);
  }

  /** Returns the tutorial definition for the given universe mode. */
  public TutorialDefinition getDefinition(@NonNull final Universe.UniverseType type) {
    return definitions.stream()
        .filter(d -> d.getMode() == type)
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("No tutorial definition for mode: " + type));
  }

  /**
   * Returns {@code true} when the tutorial should be shown to this account for the given mode: the
   * feature flag is on AND the player has not yet fully completed or skipped the tutorial.
   */
  @PreAuthorize("permitAll()")
  public boolean shouldShowTutorial(
      @NonNull final Account account, @NonNull final Universe.UniverseType type) {
    if (!gameSettingService.isTutorialEnabled(type)) {
      return false;
    }
    return completionRepository
        .findByAccountIdAndUniverseType(account.getId(), type)
        .map(c -> c.getCompletedAt() == null)
        .orElse(true);
  }

  /** Returns the 0-based index of the current tutorial step for this account and mode. */
  @PreAuthorize("permitAll()")
  public int getCurrentStep(final Long accountId, @NonNull final Universe.UniverseType type) {
    return completionRepository
        .findByAccountIdAndUniverseType(accountId, type)
        .map(AccountTutorialCompletion::getCurrentStep)
        .orElse(0);
  }

  /**
   * Advances (or retreats) the step index. When {@code newStep} equals {@code totalSteps} the
   * tutorial is marked fully complete.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('PLAYER','ADMIN','BOOKER')")
  public void advanceStep(
      @NonNull final Long accountId,
      @NonNull final Universe.UniverseType type,
      final int newStep,
      final int totalSteps) {
    Account account = accountRepository.getReferenceById(accountId);
    AccountTutorialCompletion record =
        completionRepository
            .findByAccountIdAndUniverseType(accountId, type)
            .orElseGet(
                () -> {
                  AccountTutorialCompletion r = new AccountTutorialCompletion();
                  r.setAccount(account);
                  r.setUniverseType(type);
                  return r;
                });
    record.setCurrentStep(newStep);
    if (newStep >= totalSteps) {
      record.setCompletedAt(LocalDateTime.now());
    }
    completionRepository.save(record);
  }

  /**
   * Returns {@code true} if the player has already been redirected to the tutorial at least once (a
   * completion record exists, regardless of whether they finished it).
   */
  @PreAuthorize("permitAll()")
  public boolean hasBeenRedirected(
      @NonNull final Account account, @NonNull final Universe.UniverseType type) {
    return completionRepository.existsByAccountIdAndUniverseType(account.getId(), type);
  }

  /**
   * Creates the tutorial completion record with step 0, marking that the player has been shown the
   * tutorial redirect. Subsequent logins will show a notification instead of redirecting.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('PLAYER','ADMIN','BOOKER')")
  public void recordFirstRedirect(
      @NonNull final Account account, @NonNull final Universe.UniverseType type) {
    if (completionRepository.existsByAccountIdAndUniverseType(account.getId(), type)) {
      return;
    }
    AccountTutorialCompletion record = new AccountTutorialCompletion();
    record.setAccount(account);
    record.setUniverseType(type);
    record.setCurrentStep(0);
    completionRepository.save(record);
  }

  /** Marks the tutorial as skipped (treated as completed so the player is not redirected again). */
  @Transactional
  @PreAuthorize("hasAnyRole('PLAYER','ADMIN','BOOKER')")
  public void markSkipped(
      final Long accountId, @NonNull final Universe.UniverseType type, final int totalSteps) {
    advanceStep(accountId, type, totalSteps, totalSteps);
  }

  /**
   * Creates a new tutorial universe of the given type for the player, applies the requested feature
   * settings, adds the player as OWNER, and sets it as the active universe for the current session.
   * Runs entirely under admin security context since universe creation and game-setting writes
   * require elevated authority.
   *
   * <p>{@code featureSettings} is a map from {@link GameSettingService} key constants to their
   * desired boolean values (e.g. {@code WEAR_AND_TEAR_ENABLED_KEY → true}).
   */
  @Transactional
  @PreAuthorize("hasAnyRole('PLAYER','ADMIN','BOOKER')")
  public Universe createTutorialUniverse(
      @NonNull final Account account,
      @NonNull final Universe.UniverseType type,
      @NonNull final Map<String, Boolean> featureSettings) {
    return GeneralSecurityUtils.runAsAdmin(
        () -> {
          String name = "Tutorial – " + account.getUsername();
          // Return the existing universe if one was already created (idempotent).
          Universe existing = universeService.findByName(name).orElse(null);
          if (existing != null) {
            universeContextService.setCurrentUniverse(existing);
            return existing;
          }
          Universe universe = new Universe();
          universe.setName(name);
          universe.setType(type);
          Universe saved = universeService.save(universe);

          universeMembershipService.addMember(
              saved, account, UniverseMembership.UniverseMemberRole.OWNER);

          // Set as active universe BEFORE applying settings so saveInternal scopes to it.
          universeContextService.setCurrentUniverse(saved);

          featureSettings.forEach(
              (key, enabled) -> gameSettingService.save(key, String.valueOf(enabled)));

          return saved;
        });
  }

  /** Deletes the completion record so the tutorial will be shown again on next login. */
  @Transactional
  @PreAuthorize("hasAnyRole('PLAYER','ADMIN','BOOKER')")
  public void markIncomplete(final Long accountId, @NonNull final Universe.UniverseType type) {
    completionRepository.deleteByAccountIdAndUniverseType(accountId, type);
  }

  /**
   * Invokes {@link TutorialStep#beforeStep} under admin security context. Must be idempotent —
   * called every time a step is rendered, including after backwards navigation.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('PLAYER','ADMIN','BOOKER')")
  public void runBeforeStep(
      @NonNull final Account account,
      @NonNull final Universe.UniverseType type,
      final int stepIndex) {
    TutorialStep step = getDefinition(type).getSteps().get(stepIndex);
    GeneralSecurityUtils.runAsAdmin(() -> step.beforeStep(account));
  }

  /**
   * Invokes {@link TutorialStep#afterStep} under admin security context. Called after successful
   * validation, before the step index is advanced.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('PLAYER','ADMIN','BOOKER')")
  public void runAfterStep(
      @NonNull final Account account,
      @NonNull final Universe.UniverseType type,
      final int stepIndex) {
    TutorialStep step = getDefinition(type).getSteps().get(stepIndex);
    GeneralSecurityUtils.runAsAdmin(() -> step.afterStep(account));
  }

  /**
   * Invokes {@link TutorialStep#validate} under admin security context and within a read-only
   * transaction so that validation logic can safely access lazy JPA collections.
   *
   * @return {@code null} on success; an error message string when the step is not yet satisfied.
   */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('PLAYER','ADMIN','BOOKER')")
  public String validateStep(
      @NonNull final Account account,
      @NonNull final Universe.UniverseType type,
      final int stepIndex) {
    TutorialStep step = getDefinition(type).getSteps().get(stepIndex);
    return GeneralSecurityUtils.runAsAdmin(() -> step.validate(account));
  }
}

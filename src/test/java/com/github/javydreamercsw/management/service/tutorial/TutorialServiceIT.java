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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.tutorial.AccountTutorialCompletion;
import com.github.javydreamercsw.management.domain.tutorial.AccountTutorialCompletionRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link TutorialService} and the wrestler-picker method that feeds it.
 *
 * <p>These tests use a real H2 database and real JPA entities. They exist specifically to catch
 * {@code LazyInitializationException}s that mock-based unit tests cannot detect — particularly the
 * case where {@link
 * com.github.javydreamercsw.management.service.wrestler.WrestlerService#findAllActiveWithAlignments()}
 * must return fully-loaded entities whose {@code alignments} collection can be accessed without an
 * open Hibernate session.
 */
class TutorialServiceIT extends ManagementIntegrationTest {

  @Autowired private TutorialService tutorialService;
  @Autowired private AccountTutorialCompletionRepository completionRepository;

  private Account adminAccount;

  @BeforeEach
  void setUp() {
    adminAccount = accountRepository.findByUsername("admin").orElseThrow();
    // Reset any completion record so tests start clean
    completionRepository.deleteByAccountIdAndUniverseType(
        adminAccount.getId(), Universe.UniverseType.GLOBAL);
  }

  // ── findAllActiveWithAlignments — LazyInitializationException regression ──

  @Test
  @DisplayName(
      "findAllActiveWithAlignments: returns wrestlers with alignments accessible outside a"
          + " transaction (LazyInitializationException regression)")
  void findAllActiveWithAlignments_alignmentAccessibleOutsideTransaction() {
    // Seed at least one wrestler so the query has something to return.
    // The seeded data initialiser creates wrestlers; rely on that or create one here.
    List<Wrestler> wrestlers =
        com.github.javydreamercsw.base.security.GeneralSecurityUtils.runAsAdmin(
            () -> wrestlerService.findAllActiveWithAlignments());

    // The key assertion: accessing getAlignment() on each returned entity must NOT throw
    // LazyInitializationException regardless of transaction boundaries. Before the fix,
    // this exploded because getAllWrestlers() had no @Transactional and the session closed
    // before the caller touched the lazy alignments collection.
    assertThatNoException()
        .isThrownBy(
            () -> {
              for (Wrestler w : wrestlers) {
                // Accessing getAlignment() traverses the lazy Set<WrestlerAlignment>
                var alignment = w.getAlignment();
                if (alignment != null) {
                  // Accessing getAlignmentType() must not throw — proof the collection loaded
                  alignment.getAlignmentType();
                }
              }
            });
  }

  @Test
  @DisplayName("findAllActiveWithAlignments: only returns active wrestlers")
  void findAllActiveWithAlignments_onlyReturnsActiveWrestlers() {
    List<Wrestler> wrestlers =
        com.github.javydreamercsw.base.security.GeneralSecurityUtils.runAsAdmin(
            () -> wrestlerService.findAllActiveWithAlignments());

    assertThat(wrestlers).allMatch(w -> Boolean.TRUE.equals(w.getActive()));
  }

  // ── shouldShowTutorial ────────────────────────────────────────────────────

  @Test
  @DisplayName("shouldShowTutorial: returns true when tutorial is enabled and no record exists")
  void shouldShowTutorial_noRecord_returnsTrue() {
    boolean result = tutorialService.shouldShowTutorial(adminAccount, Universe.UniverseType.GLOBAL);
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("shouldShowTutorial: returns false after tutorial is fully completed")
  @Transactional
  void shouldShowTutorial_afterCompletion_returnsFalse() {
    int totalSteps = tutorialService.getDefinition(Universe.UniverseType.GLOBAL).getSteps().size();
    tutorialService.advanceStep(
        adminAccount.getId(), Universe.UniverseType.GLOBAL, totalSteps, totalSteps);

    assertThat(tutorialService.shouldShowTutorial(adminAccount, Universe.UniverseType.GLOBAL))
        .isFalse();
  }

  // ── advanceStep / getCurrentStep round-trip ───────────────────────────────

  @Test
  @DisplayName("advanceStep: persists step index and getCurrentStep reads it back")
  @Transactional
  void advanceStep_persistsAndCurrentStepReadsBack() {
    tutorialService.advanceStep(adminAccount.getId(), Universe.UniverseType.GLOBAL, 1, 3);

    int step = tutorialService.getCurrentStep(adminAccount.getId(), Universe.UniverseType.GLOBAL);
    assertThat(step).isEqualTo(1);
  }

  @Test
  @DisplayName("advanceStep: sets completedAt when newStep equals totalSteps")
  @Transactional
  void advanceStep_finalStep_setsCompletedAt() {
    tutorialService.advanceStep(adminAccount.getId(), Universe.UniverseType.GLOBAL, 3, 3);

    Optional<AccountTutorialCompletion> record =
        completionRepository.findByAccountIdAndUniverseType(
            adminAccount.getId(), Universe.UniverseType.GLOBAL);
    assertThat(record).isPresent();
    assertThat(record.get().getCompletedAt()).isNotNull();
  }

  // ── markSkipped ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("markSkipped: marks tutorial as completed so shouldShowTutorial returns false")
  @Transactional
  void markSkipped_tutorialNotShownAgain() {
    int totalSteps = tutorialService.getDefinition(Universe.UniverseType.GLOBAL).getSteps().size();
    tutorialService.markSkipped(adminAccount.getId(), Universe.UniverseType.GLOBAL, totalSteps);

    assertThat(tutorialService.shouldShowTutorial(adminAccount, Universe.UniverseType.GLOBAL))
        .isFalse();
  }

  // ── markIncomplete ────────────────────────────────────────────────────────

  @Test
  @DisplayName("markIncomplete: deletes record so shouldShowTutorial returns true again")
  @Transactional
  void markIncomplete_resetsTutorial() {
    int totalSteps = tutorialService.getDefinition(Universe.UniverseType.GLOBAL).getSteps().size();
    tutorialService.markSkipped(adminAccount.getId(), Universe.UniverseType.GLOBAL, totalSteps);
    tutorialService.markIncomplete(adminAccount.getId(), Universe.UniverseType.GLOBAL);

    assertThat(tutorialService.shouldShowTutorial(adminAccount, Universe.UniverseType.GLOBAL))
        .isTrue();
  }

  // ── getDefinition ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("getDefinition: returns a definition for each UniverseType")
  void getDefinition_returnsDefinitionForEachMode() {
    for (Universe.UniverseType type : Universe.UniverseType.values()) {
      TutorialDefinition def = tutorialService.getDefinition(type);
      assertThat(def).isNotNull();
      assertThat(def.getSteps()).isNotEmpty();
      assertThat(def.getMode()).isEqualTo(type);
    }
  }
}

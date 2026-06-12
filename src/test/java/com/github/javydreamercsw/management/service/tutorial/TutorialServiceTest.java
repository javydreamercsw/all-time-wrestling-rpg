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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.tutorial.AccountTutorialCompletion;
import com.github.javydreamercsw.management.domain.tutorial.AccountTutorialCompletionRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
class TutorialServiceTest {

  @Mock private AccountTutorialCompletionRepository completionRepository;
  @Mock private GameSettingService gameSettingService;
  @Mock private AccountRepository accountRepository;
  @Mock private TutorialDefinition globalDefinition;
  @Mock private TutorialStep stepMock;

  private TutorialService service;
  private Account account;

  @BeforeEach
  void setUp() {
    account = new Account("player", "password", "player@example.com");
    // Use reflection-friendly approach: set id via a helper
    when(globalDefinition.getMode()).thenReturn(Universe.UniverseType.GLOBAL);
    when(globalDefinition.getSteps()).thenReturn(List.of(stepMock, stepMock, stepMock));
    service =
        new TutorialService(
            completionRepository, gameSettingService, accountRepository, List.of(globalDefinition));
  }

  // ── shouldShowTutorial ────────────────────────────────────────────────────

  @Test
  void shouldShowTutorial_whenDisabled_returnsFalse() {
    when(gameSettingService.isTutorialEnabled(Universe.UniverseType.GLOBAL)).thenReturn(false);

    assertThat(service.shouldShowTutorial(account, Universe.UniverseType.GLOBAL)).isFalse();
    verify(completionRepository, never()).findByAccountIdAndUniverseType(any(), any());
  }

  @Test
  void shouldShowTutorial_noRecord_returnsTrue() {
    when(gameSettingService.isTutorialEnabled(Universe.UniverseType.GLOBAL)).thenReturn(true);
    when(completionRepository.findByAccountIdAndUniverseType(any(), any()))
        .thenReturn(Optional.empty());

    assertThat(service.shouldShowTutorial(account, Universe.UniverseType.GLOBAL)).isTrue();
  }

  @Test
  void shouldShowTutorial_recordExistsNotComplete_returnsTrue() {
    when(gameSettingService.isTutorialEnabled(Universe.UniverseType.GLOBAL)).thenReturn(true);
    AccountTutorialCompletion incomplete = new AccountTutorialCompletion();
    incomplete.setCurrentStep(1);
    // completedAt is null → not done
    when(completionRepository.findByAccountIdAndUniverseType(any(), any()))
        .thenReturn(Optional.of(incomplete));

    assertThat(service.shouldShowTutorial(account, Universe.UniverseType.GLOBAL)).isTrue();
  }

  @Test
  void shouldShowTutorial_recordFullyComplete_returnsFalse() {
    when(gameSettingService.isTutorialEnabled(Universe.UniverseType.GLOBAL)).thenReturn(true);
    AccountTutorialCompletion done = new AccountTutorialCompletion();
    done.setCurrentStep(3);
    done.setCompletedAt(LocalDateTime.now());
    when(completionRepository.findByAccountIdAndUniverseType(any(), any()))
        .thenReturn(Optional.of(done));

    assertThat(service.shouldShowTutorial(account, Universe.UniverseType.GLOBAL)).isFalse();
  }

  // ── getCurrentStep ────────────────────────────────────────────────────────

  @Test
  void getCurrentStep_noRecord_returnsZero() {
    when(completionRepository.findByAccountIdAndUniverseType(any(), any()))
        .thenReturn(Optional.empty());

    assertThat(service.getCurrentStep(1L, Universe.UniverseType.GLOBAL)).isEqualTo(0);
  }

  @Test
  void getCurrentStep_withRecord_returnsStoredStep() {
    AccountTutorialCompletion record = new AccountTutorialCompletion();
    record.setCurrentStep(2);
    when(completionRepository.findByAccountIdAndUniverseType(any(), any()))
        .thenReturn(Optional.of(record));

    assertThat(service.getCurrentStep(1L, Universe.UniverseType.GLOBAL)).isEqualTo(2);
  }

  // ── advanceStep ───────────────────────────────────────────────────────────

  @Test
  void advanceStep_toIntermediateStep_doesNotSetCompletedAt() {
    when(completionRepository.findByAccountIdAndUniverseType(any(), any()))
        .thenReturn(Optional.empty());
    when(accountRepository.getReferenceById(any())).thenReturn(account);
    when(completionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.advanceStep(1L, Universe.UniverseType.GLOBAL, 1, 3);

    ArgumentCaptor<AccountTutorialCompletion> captor =
        ArgumentCaptor.forClass(AccountTutorialCompletion.class);
    verify(completionRepository).save(captor.capture());
    assertThat(captor.getValue().getCurrentStep()).isEqualTo(1);
    assertThat(captor.getValue().getCompletedAt()).isNull();
  }

  @Test
  void advanceStep_toFinalStep_setsCompletedAt() {
    when(completionRepository.findByAccountIdAndUniverseType(any(), any()))
        .thenReturn(Optional.empty());
    when(accountRepository.getReferenceById(any())).thenReturn(account);
    when(completionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.advanceStep(1L, Universe.UniverseType.GLOBAL, 3, 3);

    ArgumentCaptor<AccountTutorialCompletion> captor =
        ArgumentCaptor.forClass(AccountTutorialCompletion.class);
    verify(completionRepository).save(captor.capture());
    assertThat(captor.getValue().getCompletedAt()).isNotNull();
  }

  // ── markSkipped ───────────────────────────────────────────────────────────

  @Test
  void markSkipped_setsCompletedAt() {
    when(completionRepository.findByAccountIdAndUniverseType(any(), any()))
        .thenReturn(Optional.empty());
    when(accountRepository.getReferenceById(any())).thenReturn(account);
    when(completionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.markSkipped(1L, Universe.UniverseType.GLOBAL, 3);

    ArgumentCaptor<AccountTutorialCompletion> captor =
        ArgumentCaptor.forClass(AccountTutorialCompletion.class);
    verify(completionRepository).save(captor.capture());
    assertThat(captor.getValue().getCompletedAt()).isNotNull();
    assertThat(captor.getValue().getCurrentStep()).isEqualTo(3);
  }

  // ── markIncomplete ────────────────────────────────────────────────────────

  @Test
  void markIncomplete_deletesRecord() {
    service.markIncomplete(1L, Universe.UniverseType.GLOBAL);
    verify(completionRepository).deleteByAccountIdAndUniverseType(1L, Universe.UniverseType.GLOBAL);
  }

  // ── getDefinition ─────────────────────────────────────────────────────────

  @Test
  void getDefinition_returnsMatchingDefinition() {
    assertThat(service.getDefinition(Universe.UniverseType.GLOBAL)).isSameAs(globalDefinition);
  }
}

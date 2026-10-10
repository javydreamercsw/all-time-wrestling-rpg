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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.GameSetting;
import com.github.javydreamercsw.management.service.GameSettingService;
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
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.annotation.Order;

/** Unit tests for the one-time data migration runner contract. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataMigrationRunnerTest {

  @Mock private DataMigrationHistoryRepository historyRepository;
  @Mock private GameSettingService gameSettingService;
  @Mock private BuildProperties buildProperties;
  @Mock private DataMigration firstMigration;
  @Mock private DataMigration secondMigration;
  @Mock private DataMigration thirdMigration;

  @BeforeEach
  void setUp() {
    lenient()
        .when(historyRepository.save(any(DataMigrationHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(historyRepository.findByMigrationId(any())).thenReturn(Optional.empty());
    lenient()
        .when(gameSettingService.findByKeyForUniverse(any(), isNull()))
        .thenReturn(Optional.empty());
    lenient().when(buildProperties.getVersion()).thenReturn("2.11.0");
    lenient().when(firstMigration.id()).thenReturn("first-migration");
    lenient().when(secondMigration.id()).thenReturn("second-migration");
    lenient().when(thirdMigration.id()).thenReturn("third-migration");
  }

  private DataMigrationRunner runnerFor(DataMigration... migrations) {
    return new DataMigrationRunner(
        List.of(migrations), historyRepository, gameSettingService, Optional.of(buildProperties));
  }

  private DataMigrationHistory capturedHistory() {
    ArgumentCaptor<DataMigrationHistory> captor =
        ArgumentCaptor.forClass(DataMigrationHistory.class);
    verify(historyRepository).save(captor.capture());
    return captor.getValue();
  }

  private List<DataMigrationHistory> capturedHistories(int expected) {
    ArgumentCaptor<DataMigrationHistory> captor =
        ArgumentCaptor.forClass(DataMigrationHistory.class);
    verify(historyRepository, times(expected)).save(captor.capture());
    return captor.getAllValues();
  }

  @Test
  void firstRun_executesMigrationsInDeclaredOrder_andRecordsSuccessRows() {
    DataMigrationRunner runner = runnerFor(firstMigration, secondMigration);

    runner.runMigrations();

    verify(firstMigration).migrate();
    verify(secondMigration).migrate();

    List<DataMigrationHistory> rows = capturedHistories(2);
    assertEquals("first-migration", rows.get(0).getMigrationId());
    assertEquals(DataMigrationHistory.STATUS_SUCCESS, rows.get(0).getStatus());
    assertEquals("2.11.0", rows.get(0).getAppVersion());
    assertEquals("second-migration", rows.get(1).getMigrationId());
    assertEquals(DataMigrationHistory.STATUS_SUCCESS, rows.get(1).getStatus());
  }

  @Test
  void firstRun_honorsSpringOrderAnnotation_overListOrder() {
    // Bean list declares the @Order(20) migration first; execution must still sort by @Order.
    DataMigrationRunner runner =
        new DataMigrationRunner(
            List.of(new OrderedSecond(), new OrderedFirst()),
            historyRepository,
            gameSettingService,
            Optional.of(buildProperties));

    runner.runMigrations();

    List<DataMigrationHistory> rows = capturedHistories(2);
    assertEquals("first-ordered", rows.get(0).getMigrationId());
    assertEquals("second-ordered", rows.get(1).getMigrationId());
  }

  @Test
  void alreadyRecordedMigration_isSkipped() {
    when(historyRepository.findByMigrationId("first-migration"))
        .thenReturn(Optional.of(new DataMigrationHistory()));
    DataMigrationRunner runner = runnerFor(firstMigration, secondMigration);

    runner.runMigrations();

    verify(firstMigration, never()).migrate();
    verify(secondMigration).migrate();
    List<DataMigrationHistory> rows = capturedHistories(1);
    assertEquals("second-migration", rows.get(0).getMigrationId());
  }

  @Test
  void legacyGameSettingPresent_recordsLegacySeeded_andNeverMigrates() {
    when(firstMigration.legacyGameSettingKey()).thenReturn(Optional.of("legacy_done_key"));
    when(gameSettingService.findByKeyForUniverse("legacy_done_key", null))
        .thenReturn(Optional.of(new GameSetting()));
    DataMigrationRunner runner = runnerFor(firstMigration);

    runner.runMigrations();

    verify(firstMigration, never()).migrate();
    DataMigrationHistory row = capturedHistory();
    assertEquals("first-migration", row.getMigrationId());
    assertEquals(DataMigrationHistory.STATUS_LEGACY_SEEDED, row.getStatus());
    assertEquals(0L, row.getDurationMs());
  }

  @Test
  void legacyGameSettingAbsent_runsMigrationNormally() {
    when(firstMigration.legacyGameSettingKey()).thenReturn(Optional.of("not_set_yet_key"));
    when(gameSettingService.findByKeyForUniverse("not_set_yet_key", null))
        .thenReturn(Optional.empty());
    DataMigrationRunner runner = runnerFor(firstMigration);

    runner.runMigrations();

    verify(firstMigration).migrate();
    assertEquals(DataMigrationHistory.STATUS_SUCCESS, capturedHistory().getStatus());
  }

  @Test
  void failingMigration_recordsFailedRow_andRemainingMigrationsStillRun() {
    doThrow(new RuntimeException("boom")).when(firstMigration).migrate();
    DataMigrationRunner runner = runnerFor(firstMigration, secondMigration, thirdMigration);

    assertDoesNotThrow(runner::runMigrations);

    verify(firstMigration).migrate();
    verify(secondMigration).migrate();
    verify(thirdMigration).migrate();

    List<DataMigrationHistory> rows = capturedHistories(3);
    assertEquals(DataMigrationHistory.STATUS_FAILED, rows.get(0).getStatus());
    assertEquals("first-migration", rows.get(0).getMigrationId());
    assertEquals(DataMigrationHistory.STATUS_SUCCESS, rows.get(1).getStatus());
    assertEquals(DataMigrationHistory.STATUS_SUCCESS, rows.get(2).getStatus());
  }

  @Test
  void failingMigration_catchesThrowable_notJustExceptions() {
    doThrow(new OutOfMemoryError("jvm is dying")).when(firstMigration).migrate();
    DataMigrationRunner runner = runnerFor(firstMigration, secondMigration);

    assertDoesNotThrow(runner::runMigrations);

    verify(secondMigration).migrate();
    List<DataMigrationHistory> rows = capturedHistories(2);
    assertEquals(DataMigrationHistory.STATUS_FAILED, rows.get(0).getStatus());
    assertEquals(DataMigrationHistory.STATUS_SUCCESS, rows.get(1).getStatus());
  }

  @Test
  void duplicateIds_failFastAtStartup() {
    when(firstMigration.id()).thenReturn("same-id");
    when(secondMigration.id()).thenReturn("same-id");

    assertThrows(
        IllegalStateException.class,
        () ->
            new DataMigrationRunner(
                List.of(firstMigration, secondMigration),
                historyRepository,
                gameSettingService,
                Optional.of(buildProperties)));
  }

  @Test
  void appVersionRecordedFromBuildProperties() {
    DataMigrationRunner runner = runnerFor(firstMigration);

    runner.runMigrations();

    assertEquals("2.11.0", capturedHistory().getAppVersion());
  }

  @Test
  void appVersionUnknown_whenBuildPropertiesEmpty() {
    DataMigrationRunner runner =
        new DataMigrationRunner(
            List.of(firstMigration), historyRepository, gameSettingService, Optional.empty());

    runner.runMigrations();

    assertEquals("unknown", capturedHistory().getAppVersion());
  }

  /** Fixed-@Order stubs: two classes because @Order values must be compile-time constants. */
  @Order(10)
  private static final class OrderedFirst implements DataMigration {
    @Override
    public String id() {
      return "first-ordered";
    }

    @Override
    public void migrate() {
      // no-op
    }
  }

  @Order(20)
  private static final class OrderedSecond implements DataMigration {
    @Override
    public String id() {
      return "second-ordered";
    }

    @Override
    public void migrate() {
      // no-op
    }
  }
}

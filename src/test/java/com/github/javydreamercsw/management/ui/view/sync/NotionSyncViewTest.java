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
package com.github.javydreamercsw.management.ui.view.sync;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker.SyncProgress;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NotionSyncViewTest extends AbstractViewTest {

  @MockitoBean private NotionSyncScheduler notionSyncScheduler;
  @MockitoBean private NotionSyncProperties notionSyncProperties;
  @MockitoBean private SyncProgressTracker progressTracker;
  @MockitoBean private EntityDependencyAnalyzer dependencyAnalyzer;

  private NotionSyncView view;

  @BeforeEach
  public void setup() {
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of(SyncEntityType.WRESTLERS));

    NotionSyncProperties.Scheduler scheduler = new NotionSyncProperties.Scheduler();
    scheduler.setInterval(3600000L);
    when(notionSyncProperties.getScheduler()).thenReturn(scheduler);

    NotionSyncProperties.Backup backup = new NotionSyncProperties.Backup();
    backup.setDirectory("backup");
    when(notionSyncProperties.getBackup()).thenReturn(backup);

    view =
        new NotionSyncView(
            notionSyncScheduler, notionSyncProperties, progressTracker, dependencyAnalyzer);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should update progress and logs in the UI when sync is running")
  void shouldUpdateUIProgressAndLogs() {
    Button syncAllButton = _get(Button.class, spec -> spec.withId("sync-all-button"));

    // Capture the progress listener that the view registers
    ArgumentCaptor<SyncProgressTracker.SyncProgressListener> listenerCaptor =
        ArgumentCaptor.forClass(SyncProgressTracker.SyncProgressListener.class);

    syncAllButton.click();

    verify(progressTracker, timeout(5000).atLeastOnce())
        .addProgressListener(listenerCaptor.capture());
    SyncProgressTracker.SyncProgressListener listener = listenerCaptor.getValue();

    // Extract the operationId from the view's internal state or by capturing a call to
    // triggerManualSync
    ArgumentCaptor<String> opIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(notionSyncScheduler, timeout(5000)).triggerManualSync(opIdCaptor.capture());
    String operationId = opIdCaptor.getValue();

    SyncProgress progress = new SyncProgress(operationId, "Syncing...", 10);
    progress.setCurrentStep(5);
    progress.setCurrentStepDescription("Processing item 5");

    // Simulate callback from background thread
    listener.onProgressUpdated(progress);

    // Verify progress bar and text update
    ProgressBar progressBar = _get(ProgressBar.class);
    assertFalse(progressBar.isIndeterminate());
    assertEquals(0.5, progressBar.getValue(), 0.01);

    // Verify log entry added
    // addLogEntry is called for progress updates
    assertTrue(
        _get(view, Span.class, spec -> spec.withText("Progress: Processing item 5")).isVisible());

    // Verify progress text update
    assertTrue(
        _get(view, Span.class, spec -> spec.withText("Processing item 5 (5/10)")).isVisible());

    // Simulate a log message
    listener.onLogMessage(operationId, "Custom log message", "SUCCESS");
    assertTrue(_get(view, Span.class, spec -> spec.withText("Custom log message")).isVisible());

    // Simulate completion
    progress.setCompleted(true);
    progress.setSuccess(true);
    progress.setResultMessage("Sync finished");
    listener.onOperationCompleted(progress);

    // Verify progress section hidden
    com.github.mvysny.kaributesting.v10.LocatorJ._assertNone(ProgressBar.class);

    Span statusLabel = _get(view, Span.class, spec -> spec.withText("Completed"));
    assertTrue(statusLabel.isVisible());
  }
}

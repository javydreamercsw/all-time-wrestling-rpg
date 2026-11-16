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
package com.github.javydreamercsw.management.ui.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.event.ShowAdjudicationProgressEvent;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Karibu tests for the floating adjudication progress overlay (ATW-v0vh): determinate progress
 * labels per phase, the completed-with-failures Retry affordance, retry re-enable semantics, and
 * dismiss/hide behavior.
 */
class AdjudicationProgressOverlayTest {

  private AdjudicationProgressOverlay overlay;

  @BeforeEach
  void setUp() {
    MockVaadin.setup();
    overlay = new AdjudicationProgressOverlay();
    UI.getCurrent().add(overlay);
  }

  @AfterEach
  void tearDown() {
    MockVaadin.tearDown();
  }

  private static ShowAdjudicationProgressEvent event(
      ShowAdjudicationProgressEvent.Phase phase, int completed, int total) {
    return new ShowAdjudicationProgressEvent(
        new Object(), 1L, phase, completed, total, "Segment 4: A vs B");
  }

  private Button findButton(String text) {
    return flatten(overlay)
        .filter(c -> c instanceof Button b && text.equals(b.getText()))
        .map(c -> (Button) c)
        .findFirst()
        .orElse(null);
  }

  private static Stream<Component> flatten(Component c) {
    return Stream.concat(
        Stream.of(c), c.getChildren().flatMap(AdjudicationProgressOverlayTest::flatten));
  }

  @Test
  @DisplayName("Progress events drive the determinate bar and phase title")
  void progress_updatesBarAndTitle() {
    overlay.onProgress(event(ShowAdjudicationProgressEvent.Phase.NARRATION, 3, 7));
    assertThat(overlay.isVisible()).isTrue();
    StringBuilder text = new StringBuilder();
    overlay.getChildren().forEach(c -> collect(c, text));
    assertThat(text.toString()).contains("Narrating…");
    assertThat(text.toString()).contains("3 of 7");
  }

  @Test
  @DisplayName("Completed with failures shows Retry; clicking it disables and runs the action")
  void completedWithFailures_retryButtonWires() {
    AtomicInteger retries = new AtomicInteger();
    ShowService.AdjudicationResult result =
        new ShowService.AdjudicationResult(7, 5, List.of(11L, 12L), List.of(), 7);
    overlay.onCompleted(result, retries::incrementAndGet, () -> {});

    Button retry = findButton("Retry failed");
    assertThat(retry).isNotNull();
    assertThat(overlay.isVisible()).isTrue();
    retry.click();
    assertThat(retries.get()).isEqualTo(1);
    // Double-click guard: the first click disabled the button.
    assertThat(retry.isEnabled()).isFalse();
    retry.click();
    assertThat(retries.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("Completed without failures hides the Retry button")
  void completedWithoutFailures_noRetry() {
    ShowService.AdjudicationResult result =
        new ShowService.AdjudicationResult(7, 7, List.of(), List.of(), 7);
    overlay.onCompleted(result, () -> {}, () -> {});
    // The Retry button exists in the tree but is hidden (setVisible(false)).
    Button retry = findButton("Retry failed");
    assertThat(retry == null || !retry.isVisible()).isTrue();
    assertThat(overlay.isVisible()).isTrue();
  }

  @Test
  @DisplayName("Dismiss hides the overlay")
  void dismiss_hides() {
    ShowService.AdjudicationResult result =
        new ShowService.AdjudicationResult(7, 5, List.of(11L), List.of(), 7);
    overlay.onCompleted(result, () -> {}, overlay::hide);
    Button dismiss = findButton("Dismiss");
    assertThat(dismiss).isNotNull();
    dismiss.click();
    assertThat(overlay.isVisible()).isFalse();
  }

  @Test
  @DisplayName("Run failure shows the error state with Dismiss only")
  void runFailed_showsErrorDismissOnly() {
    AtomicInteger dismissed = new AtomicInteger();
    overlay.onRunFailed("Provider exploded", dismissed::incrementAndGet);
    assertThat(overlay.isVisible()).isTrue();
    // Retry is hidden on a whole-run failure (nothing succeeded to retry).
    Button retry = findButton("Retry failed");
    assertThat(retry == null || !retry.isVisible()).isTrue();
    assertThat(findButton("Dismiss")).isNotNull();
  }

  @Test
  @DisplayName("Retry-phase progress re-enters the running state")
  void retryProgress_runningStateAgain() {
    overlay.onProgress(event(ShowAdjudicationProgressEvent.Phase.RETRY, 1, 2));
    StringBuilder text = new StringBuilder();
    overlay.getChildren().forEach(c -> collect(c, text));
    assertThat(text.toString()).contains("Retrying narration…");
    assertThat(text.toString()).contains("1 of 2");
  }

  private static void collect(Component component, StringBuilder sb) {
    String own = component.getElement().getText();
    if (own != null && !own.isBlank()) {
      sb.append(own).append(' ');
    }
    component.getChildren().forEach(c -> collect(c, sb));
  }
}

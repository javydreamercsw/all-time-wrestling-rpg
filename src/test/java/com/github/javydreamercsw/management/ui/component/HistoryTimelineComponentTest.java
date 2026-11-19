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

import static com.github.mvysny.kaributesting.v10.LocatorJ._assert;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryTimelineComponentTest extends AbstractViewTest {

  @Test
  void testTimelineRendering() {
    TitleReignDTO reign1 =
        TitleReignDTO.builder()
            .id(1L)
            .championNames(List.of("Undertaker"))
            .startDate(Instant.now().minusSeconds(200 * 24 * 60 * 60))
            .endDate(Instant.now().minusSeconds(100 * 24 * 60 * 60))
            .isCurrent(false)
            .build();

    TitleReignDTO reign2 =
        TitleReignDTO.builder()
            .id(2L)
            .championNames(List.of("Kane"))
            .startDate(Instant.now().minusSeconds(100 * 24 * 60 * 60))
            .isCurrent(true)
            .build();

    HistoryTimelineComponent component = new HistoryTimelineComponent(List.of(reign2, reign1));
    assertNotNull(component);

    // Verify champions are shown
    _assert(component, Span.class, 1, spec -> spec.withText("Undertaker"));
    _assert(component, Span.class, 1, spec -> spec.withText("Kane"));
    _assert(component, Span.class, 1, spec -> spec.withText("CURRENT"));
  }

  /** A short history fits in the initial window — no "show older" control may appear. */
  @Test
  void shortHistory_showsNoExpansionControl() {
    HistoryTimelineComponent component = new HistoryTimelineComponent(reigns(3, "A"));
    for (int id = 1; id < 3 + 1; id++) {
      final int reignId = id;
      _assert(component, Span.class, 1, spec -> spec.withText("A" + reignId));
    }
    assertTrue(_findAllButtons(component).isEmpty(), "no expansion button for short history");
  }

  /**
   * ATW-3a0t: the history graph must not scroll forever as reigns accumulate. A long history
   * renders only the newest window of reigns plus an on-demand "show older" control; the older
   * reigns stay unrendered until the control is used.
   */
  @Test
  void longHistory_rendersOnlyNewestWindowWithExpansionControl() {
    // 30 reigns, newest first: W1 (index 0, current) … W30 (oldest, tail).
    List<TitleReignDTO> history = reigns(30, "W");
    HistoryTimelineComponent component = new HistoryTimelineComponent(history);

    // The newest INITIAL_VISIBLE_REIGNS reigns (W1..W8) are rendered.
    for (int id = 1; id < HistoryTimelineComponent.INITIAL_VISIBLE_REIGNS + 1; id++) {
      final int reignId = id;
      _assert(component, Span.class, 1, spec -> spec.withText("W" + reignId));
    }
    // The oldest reigns (W9 onward) are not rendered yet.
    assertTrue(_findAllButtons(component).size() == 1, "exactly one expansion control");
    Button expand = _get(component, Button.class);
    assertTrue(
        expand
            .getText()
            .contains(String.valueOf(30 - HistoryTimelineComponent.INITIAL_VISIBLE_REIGNS)),
        "control must announce the hidden count, got: " + expand.getText());
  }

  /** Clicking the control reveals the next batch; exhausting the history removes the control. */
  @Test
  void expansionControl_revealsOlderReignsInBatchesUntilExhausted() {
    List<TitleReignDTO> history = reigns(30, "W");
    HistoryTimelineComponent component = new HistoryTimelineComponent(history);

    Button expand = _get(component, Button.class);
    expand.click();

    // First click: the next EXPAND_BATCH older reigns (W9..W20) are now rendered too.
    for (int id = HistoryTimelineComponent.INITIAL_VISIBLE_REIGNS + 1;
        id
            < HistoryTimelineComponent.INITIAL_VISIBLE_REIGNS
                + HistoryTimelineComponent.EXPAND_BATCH
                + 1;
        id++) {
      final int reignId = id;
      _assert(component, Span.class, 1, spec -> spec.withText("W" + reignId));
    }
    expand = _get(component, Button.class);
    expand.click();

    // Second click: everything is visible (30 = 8 + 12 + 10), the control disappears.
    _assert(component, Span.class, 1, spec -> spec.withText("W1"));
    _assert(component, Span.class, 1, spec -> spec.withText("W30"));
    assertTrue(_findAllButtons(component).isEmpty(), "control must disappear once exhausted");
  }

  private static List<TitleReignDTO> reigns(int count, String namePrefix) {
    List<TitleReignDTO> history = new ArrayList<>();
    // id N started N days ago → id 1 is the newest (current). The service returns newest-first
    // (descending startDate), so index 0 must hold the smallest id: append ascending.
    for (int id = 1; id < count + 1; id++) {
      history.add(
          TitleReignDTO.builder()
              .id((long) id)
              .championNames(List.of(namePrefix + id))
              .startDate(Instant.now().minusSeconds(id * 24 * 60 * 60L))
              .isCurrent(id == 1)
              .build());
    }
    // Index 0 = W1 (newest) … index count-1 = W<count> (oldest).
    return history;
  }

  private static List<Button> _findAllButtons(HistoryTimelineComponent component) {
    List<Button> found = new ArrayList<>();
    collectButtons(component.getContent(), found);
    return found;
  }

  private static void collectButtons(Component root, List<Button> out) {
    if (root instanceof Button button) {
      out.add(button);
    }
    root.getChildren().forEach(child -> collectButtons(child, out));
  }
}

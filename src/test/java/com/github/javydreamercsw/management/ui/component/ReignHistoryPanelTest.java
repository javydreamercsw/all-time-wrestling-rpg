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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReignHistoryPanelTest extends AbstractViewTest {

  /** Short history: every card renders, no expansion control. */
  @Test
  void shortHistory_showsAllCardsWithoutControl() {
    ReignHistoryPanel panel = new ReignHistoryPanel(reigns(3));
    assertEquals(3, countCards(panel));
    assertTrue(_findAllButtons(panel).isEmpty(), "no expansion button for short history");
  }

  /** Long history: only the newest window renders; the control announces the hidden count. */
  @Test
  void longHistory_rendersOnlyNewestWindow() {
    ReignHistoryPanel panel = new ReignHistoryPanel(reigns(30));
    assertEquals(ReignHistoryPanel.INITIAL_VISIBLE_REIGNS, countCards(panel));
    Button expand = _get(panel, Button.class);
    assertTrue(
        expand.getText().contains(String.valueOf(30 - ReignHistoryPanel.INITIAL_VISIBLE_REIGNS)),
        "control must announce the hidden count, got: " + expand.getText());
  }

  /** Each click reveals the next batch; the control disappears once the history is exhausted. */
  @Test
  void expansionControl_revealsBatchesUntilExhausted() {
    ReignHistoryPanel panel = new ReignHistoryPanel(reigns(30));

    Button expand = _get(panel, Button.class);
    expand.click();
    assertEquals(
        ReignHistoryPanel.INITIAL_VISIBLE_REIGNS + ReignHistoryPanel.EXPAND_BATCH,
        countCards(panel));

    expand = _get(panel, Button.class);
    expand.click();
    assertEquals(30, countCards(panel), "all cards render once the history is exhausted");
    assertTrue(_findAllButtons(panel).isEmpty(), "control must disappear once exhausted");
  }

  private static List<TitleReignDTO> reigns(int count) {
    List<TitleReignDTO> history = new ArrayList<>();
    // Index 0 = newest (current reign), matching the service's descending-startDate sort.
    for (int id = 1; id < count + 1; id++) {
      history.add(
          TitleReignDTO.builder()
              .id((long) id)
              .championshipName("Title " + id)
              .championNames(List.of("Champion " + id))
              .startDate(Instant.now().minusSeconds(id * 24 * 60 * 60L))
              .isCurrent(id == 1)
              .build());
    }
    return history;
  }

  private static int countCards(ReignHistoryPanel panel) {
    return countDivsWithBorderClass(panel.getContent());
  }

  private static int countDivsWithBorderClass(Component root) {
    int count = 0;
    if (root instanceof Div div
        && div.getClassNames().toString().contains("border")
        && div.getClassNames().stream().anyMatch(c -> c.startsWith("rounded"))) {
      count = 1;
    }
    for (Component child : root.getChildren().toList()) {
      count += countDivsWithBorderClass(child);
    }
    return count;
  }

  private static List<Button> _findAllButtons(ReignHistoryPanel panel) {
    List<Button> found = new ArrayList<>();
    collectButtons(panel.getContent(), found);
    return found;
  }

  private static void collectButtons(Component root, List<Button> out) {
    if (root instanceof Button button) {
      out.add(button);
    }
    root.getChildren().forEach(child -> collectButtons(child, out));
  }
}

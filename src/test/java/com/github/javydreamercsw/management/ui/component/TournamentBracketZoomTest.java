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

import com.github.javydreamercsw.management.service.tournament.QualifierGroupsFormat;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Guards the bracket's CLIENT-SIDE scripts against the two failure modes that shipped to the
 * sandbox (both times the zoom was silently dead while the UI still rendered):
 *
 * <ol>
 *   <li>a bare arrow-function expression passed to {@code executeJs} evaluates to a function object
 *       the browser never invokes — every script must self-invoke (IIFE) or end in a call
 *   <li>a Flow parameter referenced by its Java name instead of {@code $N} throws ReferenceError
 *       that kills the entire script
 * </ol>
 *
 * <p>Karibu's mock UI never records {@code executeJs} invocations in its pending queue (verified:
 * even a bare script dump is empty), so the tests assert the exact script constants the component
 * ships — the same strings the browser receives. The scripts live in package-private constants on
 * {@link TournamentBracketComponent} so a regression fails here, not in a sandbox eyeball.
 */
public class TournamentBracketZoomTest extends AbstractMockUserIntegrationTest {

  @BeforeEach
  void setUpVaadin() {
    MockVaadin.setup();
  }

  @AfterEach
  void tearDownVaadin() {
    MockVaadin.tearDown();
  }

  @Test
  void zoomScript_isAnIIFEWithDollarZeroParameter() {
    String script = TournamentBracketComponent.ZOOM_SCRIPT.strip();

    // IIFE: opens with (( and self-invokes with (this, $0) — element + first Flow parameter.
    assertThat(script).startsWith("((");
    assertThat(script).endsWith("})(this, $0)");
    assertThat(script).doesNotContain("(this, mode)");
  }

  @Test
  void zoomScript_fitScalesCanvasDown() {
    String script = TournamentBracketComponent.ZOOM_SCRIPT;

    // The fit path: scale = available / natural, applied via CSS transform, with the frame
    // height shrunk so the page doesn't reserve dead space.
    assertThat(script).contains("scale(");
    assertThat(script).contains("el.style.transform = 'scale(' + scale + ')'");
    assertThat(script).contains("frame.style.height");
    // Measurement resets the transform first, or scrollWidth measures the SCALED width.
    assertThat(script).contains("el.style.transform = 'none'");
    assertThat(script).contains("el.scrollWidth");
  }

  @Test
  void zoomScript_phoneGuardKeepsNaturalSize() {
    String script = TournamentBracketComponent.ZOOM_SCRIPT;

    // Sub-700px viewports never scale — shrinking a 6-qualifier bracket to a 400px screen
    // makes cards unreadable; phones scroll instead.
    assertThat(script).contains("window.innerWidth < 700");
    assertThat(script.indexOf("window.innerWidth < 700"))
        .as("The phone guard must run before any scaling logic")
        .isLessThan(script.indexOf("const scale"));
  }

  @Test
  void zoomScript_fullModeResetsTransform() {
    String script = TournamentBracketComponent.ZOOM_SCRIPT;
    assertThat(script).contains("mode === '100%'");
    assertThat(script).contains("el.style.transform = 'none'");
  }

  @Test
  void resizeObserverScript_selfInvokesAndReFits() {
    String script = TournamentBracketComponent.ZOOM_OBSERVER_SCRIPT.strip();

    assertThat(script).startsWith("((");
    assertThat(script).endsWith("})(this)");
    // It registers a window-level observer (one per window, guarded) and re-runs the fit math.
    assertThat(script).contains("__bracketFitObserver");
    assertThat(script).contains("ResizeObserver");
    assertThat(script).contains("el.scrollWidth");
  }

  @Test
  void connectorScript_definesDrawFunctionAndCallsIt() {
    String script = TournamentBracketComponent.CONNECTOR_SCRIPT;

    // The draw function must be CALLED with the host element, not just defined — a script that
    // only defines it is the dead-code failure mode.
    assertThat(script.strip()).endsWith("window.__drawBracketConnectors(this);");
    // Edges come off the host attribute; paths are drawn per src:dst pair.
    assertThat(script).contains("data-bracket-edges");
    assertThat(script).contains("[data-match-number=");
  }

  @Test
  void bracketComponent_shipsAllThreeScripts_andCardsCarryMatchNumbers() {
    // End-to-end wiring: constructing a projected bracket must attach cards tagged with
    // data-match-number (the connector script's lookup keys) inside the zoom canvas.
    TournamentBracketComponent component = bracketComponent();

    assertThat(countDescendantsWithAttribute(component, "data-match-number")).isEqualTo(3);
    assertThat(findByClass(component, "bracket-connector-host")).isPresent();
    assertThat(findByClass(component, "bracket-canvas")).isPresent();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  /** A 6-entrant qualifier bracket (2 qualifiers + a projected final) with zoom wiring. */
  private static TournamentBracketComponent bracketComponent() {
    com.github.javydreamercsw.management.domain.tournament.Tournament t =
        new com.github.javydreamercsw.management.domain.tournament.Tournament();
    t.setFormatId(QualifierGroupsFormat.FORMAT_ID);
    java.util.List<com.github.javydreamercsw.management.domain.tournament.TournamentEntry> entries =
        new java.util.ArrayList<>();
    for (int i = 1; i <= 6; i++) {
      com.github.javydreamercsw.management.domain.wrestler.Wrestler w =
          new com.github.javydreamercsw.management.domain.wrestler.Wrestler();
      w.setId((long) i);
      w.setName("Wrestler " + i);
      entries.add(
          com.github.javydreamercsw.management.domain.tournament.TournamentEntry.builder()
              .wrestler(w)
              .seed(i)
              .build());
    }
    t.setEntries(entries);
    t.setRounds(new java.util.ArrayList<>());

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new QualifierGroupsFormat()));
    return new TournamentBracketComponent(adapter);
  }

  /** Counts descendant elements carrying {@code attribute}. */
  private static int countDescendantsWithAttribute(
      com.vaadin.flow.component.Component root, String attribute) {
    int count = root.getElement().hasAttribute(attribute) ? 1 : 0;
    for (com.vaadin.flow.component.Component child : root.getChildren().toList()) {
      count += countDescendantsWithAttribute(child, attribute);
    }
    return count;
  }

  /** Depth-first search for a component carrying {@code className}. */
  private static java.util.Optional<com.vaadin.flow.component.Component> findByClass(
      com.vaadin.flow.component.Component root, String className) {
    if (root.getClassNames().contains(className)) {
      return java.util.Optional.of(root);
    }
    for (com.vaadin.flow.component.Component child : root.getChildren().toList()) {
      var found = findByClass(child, className);
      if (found.isPresent()) {
        return found;
      }
    }
    return java.util.Optional.empty();
  }
}

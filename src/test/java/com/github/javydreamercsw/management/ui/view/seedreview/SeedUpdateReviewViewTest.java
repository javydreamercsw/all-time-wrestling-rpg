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
package com.github.javydreamercsw.management.ui.view.seedreview;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.service.seedreview.SeedUpdateDecisionService;
import com.github.javydreamercsw.management.sync.review.SeedFieldDecision;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.provider.ListDataProvider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/**
 * Structure + behavior tests for the admin diff-approval tree (seed-update review feature, PR 3,
 * Task 10): Domain → Entity → {@link Details} tree, per-row radios, sweep buttons, the
 * approve-everything shortcut (removals excluded), and the single Apply button that submits one
 * all-or-nothing batch and navigates home on success.
 */
class SeedUpdateReviewViewTest extends AbstractViewTest {

  private static final String RADIO_1 = "decision-wrestler-wrestler-bret-hart-catchphrase";
  private static final String RADIO_2 = "decision-wrestler-wrestler-bret-hart-theme";
  private static final String RADIO_3 = "decision-wrestler-wrestler-shawn-michaels-entrance";
  private static final String RADIO_4 = "decision-championship-championship-world-title-name";
  private static final String RADIO_5 = "decision-championship-championship-world-title-prestige";
  private static final String RADIO_6 = "decision-championship-championship-tag-title-color";

  private static final List<String> ALL_RADIOS =
      List.of(RADIO_1, RADIO_2, RADIO_3, RADIO_4, RADIO_5, RADIO_6);

  @Mock private SeedUpdateDecisionService decisionService;

  @Mock private NotificationService notificationService;

  private SeedUpdateReviewView view;

  @BeforeEach
  void resetView() {
    view = null;
  }

  // ── Fixtures ────────────────────────────────────────────────────────────────

  /** Two domains × two entities of field conflicts: the base review tree. */
  private List<SeedUpdateDecision> baseFixture() {
    return List.of(
        conflict(1L, "wrestler", "wrestler:bret-hart", "Bret Hart", "catchphrase"),
        conflict(2L, "wrestler", "wrestler:bret-hart", "Bret Hart", "theme"),
        conflict(3L, "wrestler", "wrestler:shawn-michaels", "Shawn Michaels", "entrance"),
        conflict(4L, "championship", "championship:world-title", "World Title", "name"),
        conflict(5L, "championship", "championship:world-title", "World Title", "prestige"),
        conflict(6L, "championship", "championship:tag-title", "Tag Title", "color"));
  }

  private SeedUpdateDecision conflict(
      long id, String domainKey, String entityKey, String displayName, String fieldName) {
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setId(id);
    decision.setDomainKey(domainKey);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(displayName);
    decision.setFieldName(fieldName);
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT);
    decision.setCurrentValue("current " + fieldName);
    decision.setNewValue("new " + fieldName);
    decision.setNewFingerprint("fp-" + id);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion("2.13.0");
    return decision;
  }

  private SeedUpdateDecision removal(long id, String domainKey, String entityKey, String display) {
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setId(id);
    decision.setDomainKey(domainKey);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(display);
    decision.setFieldName(null); // NULL for removal decisions
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_REMOVAL);
    decision.setCurrentValue(null);
    decision.setNewValue(null);
    decision.setNewFingerprint("fp-" + id);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion("2.13.0");
    return decision;
  }

  private SeedUpdateReviewView viewWith(List<SeedUpdateDecision> rows) {
    when(decisionService.pendingGrouped()).thenReturn(rows);
    SeedUpdateReviewView reviewView =
        new SeedUpdateReviewView(decisionService, notificationService);
    UI.getCurrent().add(reviewView);
    return reviewView;
  }

  // ── Lookups ─────────────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  private RadioButtonGroup<SeedFieldDecision> radio(SeedUpdateReviewView reviewView, String id) {
    return _get(reviewView, RadioButtonGroup.class, spec -> spec.withId(id));
  }

  private Button button(SeedUpdateReviewView reviewView, String id) {
    return _get(reviewView, Button.class, spec -> spec.withId(id));
  }

  @SuppressWarnings("unchecked")
  private Set<SeedFieldDecision> optionsOf(RadioButtonGroup<SeedFieldDecision> radio) {
    return Set.copyOf(((ListDataProvider<SeedFieldDecision>) radio.getDataProvider()).getItems());
  }

  /** Chooses every base-fixture field radio as {@code decision}. */
  private void decideAll(SeedUpdateReviewView reviewView, SeedFieldDecision decision) {
    ALL_RADIOS.forEach(id -> radio(reviewView, id).setValue(decision));
  }

  /** Fires the confirm event on a Karibu-rendered ConfirmDialog (buttons live in shadow DOM). */
  private void confirmDialog() {
    ConfirmDialog dialog = _get(UI.getCurrent(), ConfirmDialog.class);
    assertTrue(dialog.isOpened(), "Approve-everything must confirm first");
    try {
      var event = new ConfirmDialog.ConfirmEvent(dialog, true);
      Method fireEvent =
          Component.class.getDeclaredMethod(
              "fireEvent", com.vaadin.flow.component.ComponentEvent.class);
      fireEvent.setAccessible(true);
      fireEvent.invoke(dialog, event);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to fire confirm", e);
    }
  }

  /** Swaps the live Karibu UI for a mock so {@code navigate(...)} calls can be verified. */
  private UI swapInMockUi() {
    UI karibuUi = UI.getCurrent();
    UI mockUi = mock(UI.class);
    UI.setCurrent(mockUi);
    return karibuUi;
  }

  // ── Tree structure ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("Tree groups pending rows by domain then entity (2 domains x 2 entities)")
  void treeGroupsByDomainThenEntity() {
    view = viewWith(baseFixture());

    // 2 domain Details + 4 entity Details.
    assertEquals(6, _find(view, Details.class).size());

    Details wrestlerDomain =
        _get(view, Details.class, spec -> spec.withId("domain-section-wrestler"));
    assertTrue(wrestlerDomain.isVisible());
    Details championshipDomain =
        _get(view, Details.class, spec -> spec.withId("domain-section-championship"));
    assertTrue(championshipDomain.isVisible());

    assertNotNull(
        _get(
            wrestlerDomain,
            Details.class,
            spec -> spec.withId("entity-section-wrestler-bret-hart")));
    assertNotNull(
        _get(
            wrestlerDomain,
            Details.class,
            spec -> spec.withId("entity-section-wrestler-shawn-michaels")));
    assertNotNull(
        _get(
            championshipDomain,
            Details.class,
            spec -> spec.withId("entity-section-championship-world-title")));

    // Top bar: heading + "N entities · M fields awaiting decision" summary.
    Span summary = _get(view, Span.class, spec -> spec.withId("seed-review-summary"));
    assertEquals("4 entities · 6 fields awaiting decision", summary.getText());
  }

  // ── Apply gating ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Apply stays disabled until every pending row has a choice")
  void applyDisabledUntilAllDecided() {
    view = viewWith(baseFixture());
    Button apply = button(view, "apply-button");
    assertFalse(apply.isEnabled(), "Nothing decided yet");

    radio(view, RADIO_1).setValue(SeedFieldDecision.USE_NEW);
    assertFalse(apply.isEnabled(), "One decided row is not enough");

    decideAll(view, SeedFieldDecision.KEEP_MINE);
    assertTrue(apply.isEnabled(), "Every pending row decided — apply unlocks");
  }

  // ── Removal rows ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Removal rows offer Keep-as-custom and Remove, with a dependents warning")
  void removalRowsOfferKeepAsCustomAndRemove() {
    List<SeedUpdateDecision> rows = new ArrayList<>(baseFixture());
    rows.add(removal(7L, "championship", "championship:legacy-title", "Legacy Title"));
    view = viewWith(rows);

    RadioButtonGroup<SeedFieldDecision> removalRadio =
        radio(view, "decision-championship-championship-legacy-title-removal");

    assertEquals(
        Set.of(SeedFieldDecision.KEEP_AS_CUSTOM, SeedFieldDecision.REMOVE),
        optionsOf(removalRadio),
        "Removal rows must offer exactly keep-as-custom and remove");

    assertTrue(
        removalRadio.getHelperText().contains("dependent"),
        "Helper text must explain the apply-time dependents refusal");

    Span notice =
        _get(view, Span.class, spec -> spec.withId("removal-notice-championship-legacy-title"));
    assertTrue(notice.getText().contains("Removed from source data"));

    // Both options are selectable and land in the submitted choices.
    removalRadio.setValue(SeedFieldDecision.KEEP_AS_CUSTOM);
    assertEquals(SeedFieldDecision.KEEP_AS_CUSTOM, view.getChoices().get(7L));
    removalRadio.setValue(SeedFieldDecision.REMOVE);
    assertEquals(SeedFieldDecision.REMOVE, view.getChoices().get(7L));
  }

  // ── Approve everything ──────────────────────────────────────────────────────

  @Test
  @DisplayName("Approve-everything sets field conflicts to use-new and leaves removals untouched")
  void approveEverythingSkipsRemovals() {
    List<SeedUpdateDecision> rows = new ArrayList<>(baseFixture());
    rows.add(removal(7L, "championship", "championship:legacy-title", "Legacy Title"));
    view = viewWith(rows);

    // Service contract: only pending field conflicts, mapped to USE_NEW — removals excluded.
    Map<Long, SeedFieldDecision> approved =
        baseFixture().stream()
            .collect(
                Collectors.toMap(SeedUpdateDecision::getId, decision -> SeedFieldDecision.USE_NEW));
    when(decisionService.approveEverythingExceptRemovals()).thenReturn(approved);

    button(view, "approve-everything-button").click();
    confirmDialog();

    for (String id : ALL_RADIOS) {
      assertEquals(SeedFieldDecision.USE_NEW, radio(view, id).getValue(), "Radio " + id);
    }
    assertEquals(
        null,
        radio(view, "decision-championship-championship-legacy-title-removal").getValue(),
        "Removal radio must stay untouched by approve-everything");
    assertFalse(view.getChoices().containsKey(7L), "Removal must stay undecided");
    assertFalse(
        button(view, "apply-button").isEnabled(),
        "Apply must stay disabled while the removal is undecided");
    verify(decisionService, never()).apply(any());
  }

  // ── Sweep buttons ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("Domain buttons sweep every descendant field row (and leave removals alone)")
  void domainButtonsSweepDescendants() {
    List<SeedUpdateDecision> rows = new ArrayList<>(baseFixture());
    rows.add(removal(7L, "wrestler", "wrestler:dusty-rhodes", "Dusty Rhodes"));
    view = viewWith(rows);

    button(view, "domain-wrestler-keep-all-mine").click();
    assertEquals(SeedFieldDecision.KEEP_MINE, radio(view, RADIO_1).getValue());
    assertEquals(SeedFieldDecision.KEEP_MINE, radio(view, RADIO_2).getValue());
    assertEquals(SeedFieldDecision.KEEP_MINE, radio(view, RADIO_3).getValue());
    assertNull(
        radio(view, "decision-wrestler-wrestler-dusty-rhodes-removal").getValue(),
        "Removal rows need an explicit per-entity choice — sweeps must not touch them");
    assertEquals(SeedFieldDecision.KEEP_MINE, view.getChoices().get(1L));
    assertEquals(SeedFieldDecision.KEEP_MINE, view.getChoices().get(3L));

    button(view, "domain-championship-approve-all-new").click();
    assertEquals(SeedFieldDecision.USE_NEW, radio(view, RADIO_4).getValue());
    assertEquals(SeedFieldDecision.USE_NEW, radio(view, RADIO_5).getValue());
    assertEquals(SeedFieldDecision.USE_NEW, radio(view, RADIO_6).getValue());
    assertEquals(SeedFieldDecision.USE_NEW, view.getChoices().get(6L));
  }

  @Test
  @DisplayName("Entity buttons sweep that entity's field rows only")
  void entityButtonsSweepFields() {
    view = viewWith(baseFixture());

    button(view, "entity-wrestler-bret-hart-keep-mine").click();
    assertEquals(SeedFieldDecision.KEEP_MINE, radio(view, RADIO_1).getValue());
    assertEquals(SeedFieldDecision.KEEP_MINE, radio(view, RADIO_2).getValue());
    assertNull(radio(view, RADIO_3).getValue(), "Other entities must stay untouched");

    button(view, "entity-wrestler-shawn-michaels-use-new").click();
    assertEquals(SeedFieldDecision.USE_NEW, radio(view, RADIO_3).getValue());
    assertEquals(SeedFieldDecision.USE_NEW, view.getChoices().get(3L));
    assertEquals(SeedFieldDecision.KEEP_MINE, view.getChoices().get(1L));
  }

  // ── Apply ───────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("Apply submits every pending row with its radio choice to the service")
  void applyCallsServiceWithAllDecisions() {
    view = viewWith(baseFixture());

    radio(view, RADIO_1).setValue(SeedFieldDecision.KEEP_MINE);
    radio(view, RADIO_2).setValue(SeedFieldDecision.USE_NEW);
    radio(view, RADIO_3).setValue(SeedFieldDecision.KEEP_MINE);
    radio(view, RADIO_4).setValue(SeedFieldDecision.USE_NEW);
    radio(view, RADIO_5).setValue(SeedFieldDecision.USE_NEW);
    radio(view, RADIO_6).setValue(SeedFieldDecision.KEEP_MINE);

    // Look the buttons up while the real Karibu UI is installed — a mock UI breaks component
    // lookup (ShowPlanningViewTest precedent: capture via mock, but click live components).
    Button apply = button(view, "apply-button");

    when(decisionService.pendingCount()).thenReturn(0L);
    UI karibuUi = swapInMockUi();
    try {
      apply.click();

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<Map<Long, SeedFieldDecision>> captor =
          ArgumentCaptor.forClass((Class<Map<Long, SeedFieldDecision>>) (Class) Map.class);
      verify(decisionService).apply(captor.capture());

      Map<Long, SeedFieldDecision> submitted = captor.getValue();
      assertEquals(
          baseFixture().stream().map(SeedUpdateDecision::getId).collect(Collectors.toSet()),
          submitted.keySet(),
          "Every pending row must be in the submitted batch");
      assertEquals(
          Map.of(
              1L, SeedFieldDecision.KEEP_MINE,
              2L, SeedFieldDecision.USE_NEW,
              3L, SeedFieldDecision.KEEP_MINE,
              4L, SeedFieldDecision.USE_NEW,
              5L, SeedFieldDecision.USE_NEW,
              6L, SeedFieldDecision.KEEP_MINE),
          submitted,
          "Each submitted choice must match its radio");
    } finally {
      UI.setCurrent(karibuUi);
    }
  }

  @Test
  @DisplayName("Successful apply notifies success and navigates home (gate down)")
  void applySuccessNavigatesHome() {
    view = viewWith(baseFixture());
    decideAll(view, SeedFieldDecision.USE_NEW);
    Button apply = button(view, "apply-button");
    when(decisionService.pendingCount()).thenReturn(0L);

    UI karibuUi = swapInMockUi();
    try {
      apply.click();

      verify(decisionService).apply(any());
      verify(notificationService).showSuccess(contains("unlocked"));
      verify(UI.getCurrent()).navigate("");
    } finally {
      UI.setCurrent(karibuUi);
    }
  }

  @Test
  @DisplayName("Failed apply shows the service's error and leaves the choices editable")
  void applyFailureShowsErrorNotification() {
    view = viewWith(baseFixture());
    decideAll(view, SeedFieldDecision.KEEP_MINE);
    Button apply = button(view, "apply-button");
    doThrow(
            new IllegalStateException(
                "Cannot remove 'championship:legacy-title': other data still references it."))
        .when(decisionService)
        .apply(any());

    UI karibuUi = swapInMockUi();
    try {
      apply.click();

      verify(notificationService).showError(contains("references it"));
      verify(UI.getCurrent(), never()).navigate(any(String.class));
      assertTrue(
          apply.isEnabled(),
          "A failed batch must leave the decisions editable (all rows still decided)");
    } finally {
      UI.setCurrent(karibuUi);
    }
  }
}

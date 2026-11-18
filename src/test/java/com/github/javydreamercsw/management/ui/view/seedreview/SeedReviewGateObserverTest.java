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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Behavior table for the Vaadin navigation freeze (seed-update review feature, Task 7). The
 * observer's decision is a pure function of (gate state, location path, navigation target), so a
 * mocked {@link BeforeEnterEvent} plus a mocked {@link SeedReviewGate} is the lightest honest
 * harness — no Vaadin session or Spring context needed.
 */
class SeedReviewGateObserverTest {

  private SeedReviewGate gate;

  private BeforeEnterEvent event;

  @BeforeEach
  void setUp() {
    gate = mock(SeedReviewGate.class);
    event = mock(BeforeEnterEvent.class);
  }

  private void navigateTo(String path) {
    when(event.getLocation()).thenReturn(new Location(path));
  }

  private void assertReroutedToGate() {
    SeedReviewGateObserver.checkNavigation(event, gate);
    verify(event).rerouteTo(SeedReviewGateView.ROUTE);
    clearInvocations(event);
  }

  private void assertNotRerouted() {
    SeedReviewGateObserver.checkNavigation(event, gate);
    verify(event, never()).rerouteTo(anyString());
    clearInvocations(event);
  }

  @Test
  @DisplayName("Gameplay views reroute to the gate while the gate is up")
  void gameplayNavigationRedirectsUnderGate() {
    when(gate.isUp()).thenReturn(true);
    for (String path :
        List.of("", "wrestler-list", "show-planning", "admin/observability", "match/42", "teams")) {
      navigateTo(path);
      assertReroutedToGate();
    }
  }

  @Test
  @DisplayName(
      "Review, inbox, profile, auth and access-denied routes stay reachable under the gate")
  void reviewAndInboxReachableUnderGate() {
    when(gate.isUp()).thenReturn(true);
    // Every exempt route, straight from the production set so a future addition is covered here
    // automatically (Review Focus #5: exempt routes must never reroute — redirect-loop killer).
    for (String exempt : SeedReviewGateObserver.EXEMPT_ROUTES) {
      navigateTo(exempt);
      assertNotRerouted();
    }
  }

  @Test
  @DisplayName("Navigating to the gate view itself never reroutes")
  void gateViewDoesNotRerouteItself() {
    when(gate.isUp()).thenReturn(true);
    navigateTo(SeedReviewGateView.ROUTE);
    doReturn(SeedReviewGateView.class).when(event).getNavigationTarget();
    assertNotRerouted();
  }

  @Test
  @DisplayName("Exempt matching covers sub-routes but not same-prefix neighbours")
  void exemptMatchRequiresSegmentBoundary() {
    when(gate.isUp()).thenReturn(true);
    // Hierarchical child of an exempt route stays exempt.
    navigateTo("inbox/detail");
    assertNotRerouted();
    navigateTo("seed-review/decisions");
    assertNotRerouted();
    // Same-prefix neighbours are NOT exempt and must freeze.
    navigateTo("inboxx");
    assertReroutedToGate();
    navigateTo("login-faq");
    assertReroutedToGate();
    navigateTo("seed-reviews");
    assertReroutedToGate();
  }

  @Test
  @DisplayName("No reroute for any navigation once the gate is down")
  void passesWhenGateDown() {
    when(gate.isUp()).thenReturn(false);
    for (String path : List.of("", "wrestler-list", "show-planning", "inbox")) {
      navigateTo(path);
      assertNotRerouted();
    }
  }

  @Test
  @DisplayName("Error navigations (unknown URLs, error views) are not hijacked by the gate")
  void errorNavigationsStayUntouched() {
    when(gate.isUp()).thenReturn(true);
    navigateTo("some-unknown-url");
    when(event.isErrorEvent()).thenReturn(true);
    assertNotRerouted();
  }

  @Test
  @DisplayName(
      "Navigation checks read the snapshot only — never refresh() (no DB hit per navigation)")
  void observerNeverRefreshesGateSnapshot() {
    when(gate.isUp()).thenReturn(true);
    navigateTo("wrestler-list");
    SeedReviewGateObserver.checkNavigation(event, gate);
    navigateTo("inbox");
    SeedReviewGateObserver.checkNavigation(event, gate);
    verify(gate, never()).refresh();
  }

  @Test
  @DisplayName("Exempt matching: exact segment or '/'-boundary prefix only")
  void isExemptBoundaryRules() {
    assertTrue(SeedReviewGateObserver.isExempt("inbox"));
    assertTrue(SeedReviewGateObserver.isExempt("inbox/detail"));
    assertFalse(SeedReviewGateObserver.isExempt("inboxx"));
    assertFalse(SeedReviewGateObserver.isExempt(""));
    assertFalse(SeedReviewGateObserver.isExempt(null));
  }

  @Test
  @DisplayName("Service-init wiring registers a before-enter listener on every new UI")
  void registersNavigationListenerOnUiInit() {
    VaadinService service = mock(VaadinService.class);
    UI ui = mock(UI.class);

    VaadinServiceInitListener initListener =
        new SeedReviewGateObserver().seedReviewGateInitListener(gate);
    initListener.serviceInit(new ServiceInitEvent(service));

    ArgumentCaptor<UIInitListener> uiInitCaptor = ArgumentCaptor.forClass(UIInitListener.class);
    verify(service).addUIInitListener(uiInitCaptor.capture());

    uiInitCaptor.getValue().uiInit(new UIInitEvent(ui, service));
    ArgumentCaptor<BeforeEnterListener> enterCaptor =
        ArgumentCaptor.forClass(BeforeEnterListener.class);
    verify(ui).addBeforeEnterListener(enterCaptor.capture());

    // The registered listener applies the gate decision.
    when(gate.isUp()).thenReturn(true);
    BeforeEnterEvent navigation = mock(BeforeEnterEvent.class);
    when(navigation.getLocation()).thenReturn(new Location("wrestler-list"));
    enterCaptor.getValue().beforeEnter(navigation);
    verify(navigation).rerouteTo(SeedReviewGateView.ROUTE);
  }
}

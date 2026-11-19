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

import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Vaadin half of the seed-update review gate (seed-update review feature, Task 7): while {@link
 * SeedReviewGate#isUp()}, every navigation to a non-exempt route is rerouted to {@link
 * SeedReviewGateView}.
 *
 * <p>Registered as a {@link VaadinServiceInitListener} bean so the before-enter listener attaches
 * to every new UI without touching each view (mirrors {@code TutorialRedirectInitializer}).
 *
 * <p>Performance contract: the check reads only the gate's boot+park maintained snapshot ({@link
 * SeedReviewGate#isUp()}) — {@code refresh()} would mean a DB query per navigation.
 *
 * <p>Exempt routes: exact-segment match or a {@code '/'}-boundary prefix match (exempting {@code
 * inbox} also covers a hierarchical {@code inbox/...} child, but never a same-prefix neighbour like
 * {@code inboxx}). Error navigations are also exempt — an error screen must not be hijacked into a
 * redirect loop.
 */
@Configuration
public class SeedReviewGateObserver {

  /**
   * Routes that stay reachable while the gate is up: auth, the gate itself, the admin review screen
   * (PR 3), inbox, profile, and the access-denied screen. Route strings exact — the review view
   * registers at {@code seed-review}.
   */
  public static final Set<String> EXEMPT_ROUTES =
      Set.of(
          "login",
          "logout",
          SeedReviewGateView.ROUTE,
          SeedReviewGateView.REVIEW_ROUTE,
          "inbox",
          "profile",
          "access-denied");

  private static final String SEGMENT_SEPARATOR = "/";

  /**
   * The navigation decision, factored out of the listener lambda so the behavior table (exempt ×
   * gate state × error event) is unit-testable without a UI.
   *
   * @param event the in-flight before-enter navigation
   * @param gate the freeze gate (snapshot read only)
   */
  static void checkNavigation(BeforeEnterEvent event, SeedReviewGate gate) {
    if (!gate.isUp()) {
      return;
    }
    // Error navigation: the URL never resolved to a route, so an exempt match is impossible;
    // rerouting it would mask the error and risks a loop if the error target itself fails.
    if (event.isErrorEvent()) {
      return;
    }
    // Navigating to the gate view itself: exempt by definition (this check also holds when a
    // future route prefix makes the location path diverge from the target class).
    if (SeedReviewGateView.class.equals(event.getNavigationTarget())) {
      return;
    }
    if (isExempt(event.getLocation().getPath())) {
      return;
    }
    event.rerouteTo(SeedReviewGateView.ROUTE);
  }

  /**
   * True when {@code path} is an exempt route: equal to it, or a child of it (a {@code '/'}
   * -boundary prefix). Same-prefix neighbours ({@code inboxx} vs {@code inbox}) do not match.
   */
  static boolean isExempt(String path) {
    if (path == null || path.isEmpty()) {
      return false;
    }
    return EXEMPT_ROUTES.stream()
        .anyMatch(
            exempt ->
                path.equals(exempt)
                    || (path.startsWith(exempt)
                        && path.length() > exempt.length()
                        && path.charAt(exempt.length()) == SEGMENT_SEPARATOR.charAt(0)));
  }

  /**
   * Service-init wiring: on every new UI, attach one before-enter listener that applies the gate
   * decision to every navigation that UI performs.
   */
  @Bean
  public VaadinServiceInitListener seedReviewGateInitListener(SeedReviewGate gate) {
    return event ->
        event
            .getSource()
            .addUIInitListener(
                uiEvent ->
                    uiEvent
                        .getUI()
                        .addBeforeEnterListener(enterEvent -> checkNavigation(enterEvent, gate)));
  }
}

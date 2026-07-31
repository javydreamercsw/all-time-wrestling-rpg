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
package com.github.javydreamercsw.management.service.home;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.vaadin.flow.component.Component;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Plug-in interface for sections on the landing page "Since Your Last Visit" panel.
 *
 * <p>Implement this and annotate with {@code @org.springframework.stereotype.Service} to contribute
 * a card. {@link com.github.javydreamercsw.management.ui.view.home.HomeView} collects all
 * providers, filters by role, sorts by {@link #getOrder()}, and renders non-null cards.
 */
public interface LandingPageSummaryProvider {

  /** Display title shown above the card. */
  String getTitle();

  /** Sort order — lower numbers render first. */
  int getOrder();

  /**
   * Roles this provider applies to. An empty set means the card is shown to all authenticated
   * users.
   */
  Set<RoleName> applicableRoles();

  /**
   * Build the summary card component.
   *
   * @param since the previous login timestamp; {@code null} on first-ever login
   * @param account the current authenticated account
   * @return the card component, or {@code null} if there is nothing to report
   */
  Component buildSummaryCard(LocalDateTime since, Account account);
}

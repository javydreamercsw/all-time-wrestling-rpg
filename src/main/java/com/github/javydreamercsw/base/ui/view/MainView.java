/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.base.ui.view;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

/** This view shows up when a user navigates to the root ('/') of the application. */
@Route
@PermitAll // When security is enabled, allow all authenticated users
public final class MainView extends Main {

  public MainView() {
    addClassName(LumoUtility.Padding.MEDIUM);
    add(new ViewToolbar("Main"));
    add(new Div("Please select a view from the menu on the left."));
  }

  /** Navigates to the main view. */
  public static void showMainView() {
    UI.getCurrent().navigate(MainView.class);
  }
}

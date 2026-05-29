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
package com.github.javydreamercsw.management.ui.view.admin;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.tabs.Tabs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for AdminView using Karibu + Spring instantiator.
 *
 * <p>AdminView calls VaadinService.getCurrent().getInstantiator().getOrCreate() in its constructor
 * to resolve Spring-managed sub-views (AiSettingsView, GameSettingsView, etc.). MockSpringServlet
 * wires SpringInstantiator so those lookups resolve against the real application context.
 */
@TestPropertySource(properties = "data.initializer.enabled=false")
class AdminViewIT extends ManagementIntegrationTest {

  private static Routes routes;

  @Autowired private ApplicationContext applicationContext;

  private AdminView view;

  @BeforeAll
  static void discoverRoutes() {
    routes = new Routes().autoDiscoverViews("com.github.javydreamercsw");
  }

  @BeforeEach
  void setupKaribu() {
    MockSpringServlet servlet = new MockSpringServlet(routes, applicationContext, UI::new);
    MockVaadin.setup(UI::new, servlet);
    // Navigate to the admin route — MockSpringServlet uses SpringInstantiator so
    // VaadinService.getInstantiator().getOrCreate() resolves Spring-managed sub-views.
    UI.getCurrent().navigate("admin");
    view = (AdminView) UI.getCurrent().getCurrentView();
  }

  @AfterEach
  @Override
  public void tearDown() {
    MockVaadin.tearDown();
    super.tearDown();
  }

  @Test
  @DisplayName("Should render the Admin Tools toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }

  @Test
  @DisplayName("Should render all admin tabs")
  void shouldRenderTabs() {
    Tabs tabs = _get(view, Tabs.class);
    assertTrue(tabs.isVisible());
    assertFalse(tabs.getChildren().findAny().isEmpty());
  }
}

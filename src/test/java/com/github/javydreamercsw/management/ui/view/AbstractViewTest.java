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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

public abstract class AbstractViewTest extends AbstractMockUserIntegrationTest {
  private static Routes routes;

  @BeforeAll
  public static void discoverRoutes() {
    // Auto-discover your application's routes
    routes = new Routes().autoDiscoverViews("com.github.javydreamercsw");
  }

  @BeforeEach
  public void setupKaribu() {
    MockitoAnnotations.openMocks(this);
    MockVaadin.setup(routes); // Set up Karibu with your discovered routes
  }

  @AfterEach
  public void tearDown() {
    MockVaadin.tearDown();
  }
}

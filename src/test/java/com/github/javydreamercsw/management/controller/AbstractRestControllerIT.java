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
package com.github.javydreamercsw.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for REST controller integration tests that need real database interaction. Inherits
 * Vaadin infrastructure mocks from AbstractIntegrationTest.
 */
public abstract class AbstractRestControllerIT extends AbstractIntegrationTest {

  protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected WebApplicationContext context;

  @BeforeEach
  public void configureMockMvc() {
    // Default MockMvc using webAppContextSetup.
    // Subclasses can override if they need standaloneSetup.
    if (mockMvc == null) {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }
  }
}

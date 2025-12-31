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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.config.TestSecurityConfig;
import com.github.javydreamercsw.base.security.CustomUserDetailsService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN", "ROLE_BOOKER"})
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
public abstract class AbstractControllerTest extends AbstractIntegrationTest {

  @Autowired private WebApplicationContext context;
  protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;

  // Infrastructure mocks likely needed by multiple controllers or security
  @MockitoBean protected CustomUserDetailsService customUserDetailsService;
  @MockitoBean protected VaadinDefaultRequestCache vaadinDefaultRequestCache;
  @MockitoBean protected RequestUtil requestUtil;
  @MockitoBean protected SegmentNarrationServiceFactory serviceFactory;
  @MockitoBean protected TierRecalculationService tierRecalculationService;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }
}

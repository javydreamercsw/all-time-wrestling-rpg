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

import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationScheduler;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN", "ROLE_BOOKER"})
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
public abstract class AbstractControllerTest extends AbstractIntegrationTest {

  protected MockMvc mockMvc;
  @org.springframework.beans.factory.annotation.Autowired private WebApplicationContext context;

  @BeforeEach
  public void configureMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  // Infrastructure mocks still needed (e.g. for external services or scheduled tasks)
  @MockitoBean protected NotionApiExecutor notionApiExecutor;
  @MockitoBean protected TierRecalculationScheduler tierRecalculationScheduler;
  @MockitoBean protected RankingService rankingService;
  @MockitoBean protected TierRecalculationService tierRecalculationService;
  @MockitoBean protected RivalryRepository rivalryRepository;

  @MockitoBean(name = "testUserInitializer")
  protected CommandLineRunner testUserInitializer;

  @MockitoBean(name = "recalculateRanking")
  protected CommandLineRunner recalculateRanking;

  @Override
  @BeforeEach
  public void baseSetUp() throws Exception {
    this.dataInitializerEnabled = false;
    super.baseSetUp();
  }
}

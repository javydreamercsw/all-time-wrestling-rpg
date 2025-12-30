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
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.config.TestSecurityConfig;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.CustomUserDetailsService;
import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
public abstract class AbstractControllerTest {

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  // Infrastructure mocks likely needed by multiple controllers or security
  @MockitoBean protected CustomUserDetailsService customUserDetailsService;
  @MockitoBean protected VaadinDefaultRequestCache vaadinDefaultRequestCache;
  @MockitoBean protected RequestUtil requestUtil;
  @MockitoBean protected SegmentNarrationServiceFactory serviceFactory;
  @MockitoBean protected RankingService rankingService;
  @MockitoBean protected WrestlerRepository wrestlerRepository;
  @MockitoBean private AccountRepository accountRepository;
  @MockitoBean private RoleRepository roleRepository;
}

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
package com.github.javydreamercsw.management;

import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.TestSecurityContextHolder;

@Slf4j
public abstract class ManagementIntegrationTest extends AbstractMockUserIntegrationTest {

  static {
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
  }

  @Autowired protected FactionRivalryRepository factionRivalryRepository;
  @Autowired protected RivalryRepository rivalryRepository;
  @Autowired protected SegmentRepository segmentRepository;
  @Autowired protected SeasonRepository seasonRepository;
  @Autowired protected SegmentRuleService segmentRuleService;
  @Autowired protected ShowTypeService showTypeService;
  @Autowired protected ShowTypeRepository showTypeRepository;
  @Autowired protected ShowTemplateRepository showTemplateRepository;
  @Autowired protected ShowRepository showRepository;
  @Autowired protected SegmentTypeService segmentTypeService;
  @Autowired protected ShowTemplateService showTemplateService;
  @Autowired protected WrestlerService wrestlerService;
  @Autowired protected NotionSyncService notionSyncService;
  @Autowired protected FactionRepository factionRepository;
  @Autowired protected TitleReignRepository titleReignRepository;
  @Autowired protected TitleRepository titleRepository;
  @Autowired protected TeamService teamService;
  @Autowired protected TeamRepository teamRepository;
  @Autowired protected ShowService showService;
  @Autowired protected DeckRepository deckRepository;
  @Autowired protected SegmentTypeRepository segmentTypeRepository;
  @Autowired protected DatabaseCleanup databaseCleaner;

  @Autowired
  @Qualifier("testCustomUserDetailsService")
  protected UserDetailsService userDetailsService;

  protected static Routes routes;
  private AutoCloseable mocks;

  @BeforeAll
  public static void setupRoutes() {
    routes = new Routes().autoDiscoverViews("com.github.javydreamercsw");
  }

  @BeforeEach
  public void prepareTestEnvironment() {
    // Clean up database and re-initialize default accounts
    databaseCleaner.clearRepositories();
    
    // Refresh security context to ensure the principal has persistent entities
    refreshSecurityContext();

    // If no authentication was established (e.g. first run with empty DB or not using @WithCustomMockUser), default to admin
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      log.info("No authentication found after cleanup. logging in as 'admin'.");
      loginAs("admin");
    }

    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      log.info(
          "Security context established for: {}",
          SecurityContextHolder.getContext().getAuthentication().getName());
    } else {
      log.warn("Failed to establish security context for test: {}", this.getClass().getSimpleName());
    }
  }

  @BeforeEach
  public void setupKaribu() {
    mocks = MockitoAnnotations.openMocks(this);

    // Only setup MockVaadin for UI view tests to avoid interfering with service-layer security
    String packageName = this.getClass().getPackageName();
    if (packageName.contains(".ui.view") && !packageName.contains(".security")) {
      MockVaadin.setup(routes);
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    String packageName = this.getClass().getPackageName();
    if (packageName.contains(".ui.view") && !packageName.contains(".security")) {
      MockVaadin.tearDown();
    }
    if (mocks != null) {
      mocks.close();
    }
    clearSecurityContext();
  }

  protected void clearAllRepositories() {
    databaseCleaner.clearRepositories();
  }

  protected void refreshSecurityContext() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      auth = TestSecurityContextHolder.getContext().getAuthentication();
    }

    if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
      log.debug("Refreshing security context for user: {}", details.getUsername());
      var accountOpt = accountRepository.findByUsername(details.getUsername());
      if (accountOpt.isPresent()) {
        log.debug("Found persistent account for {}, logging in...", details.getUsername());
        this.login(accountOpt.get());
      } else {
        log.warn("Persistent account for {} not found after cleanup. Clearing context.", details.getUsername());
        clearSecurityContext();
      }
    } else if (auth != null
        && auth.getPrincipal()
            instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
      log.debug(
          "Refreshing security context for standard UserDetails: {}", userDetails.getUsername());
      var accountOpt = accountRepository.findByUsername(userDetails.getUsername());
      if (accountOpt.isPresent()) {
        this.login(accountOpt.get());
      } else {
        log.warn("Persistent account for {} not found after cleanup. Clearing context.", userDetails.getUsername());
        clearSecurityContext();
      }
    }
  }

  protected void loginAs(String username) {
    clearSecurityContext();
    var accountOpt = accountRepository.findByUsername(username);
    if (accountOpt.isPresent()) {
      this.login(accountOpt.get());
    } else {
      throw new RuntimeException("loginAs: Account not found: " + username);
    }
  }

  protected void clearSecurityContext() {
    SecurityContextHolder.clearContext();
    TestSecurityContextHolder.clearContext();
  }
}

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
package com.github.javydreamercsw.management.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.Application;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.DatabaseCleanup;
import com.github.javydreamercsw.management.config.TestAIConfiguration;
import com.github.javydreamercsw.management.config.TestNotionConfiguration;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTargetRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.league.DraftPickRepository;
import com.github.javydreamercsw.management.domain.league.DraftRepository;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
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
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import java.util.Collections;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = Application.class)
@Slf4j
@WithCustomMockUser(roles = {"ADMIN"})
@ActiveProfiles("test")
@Import({TestAIConfiguration.class, TestNotionConfiguration.class})
public abstract class AbstractIntegrationTest {

  @MockitoBean protected VaadinDefaultRequestCache vaadinDefaultRequestCache;
  @MockitoBean protected RequestUtil requestUtil;

  @Autowired protected ApplicationContext applicationContext;
  @Autowired protected InboxRepository inboxRepository;
  @Autowired protected InboxItemTargetRepository inboxItemTargetRepository;
  @Autowired protected WrestlerRepository wrestlerRepository;
  @Autowired protected MultiWrestlerFeudService multiWrestlerFeudService;
  @Autowired protected SeasonRepository seasonRepository;
  @Autowired protected SegmentService segmentService;
  @Autowired protected SeasonService seasonService;
  @Autowired protected RivalryService rivalryService;
  @Autowired protected TitleService titleService;
  @Autowired protected ShowService showService;
  @Autowired protected SegmentTypeService segmentTypeService;
  @Autowired protected SegmentRuleService segmentRuleService;
  @Autowired protected SegmentRepository segmentRepository;
  @Autowired protected MultiWrestlerFeudRepository multiWrestlerFeudRepository;
  @Autowired protected ShowRepository showRepository;
  @Autowired protected ShowTypeRepository showTypeRepository;
  @Autowired protected SegmentTypeRepository segmentTypeRepository;
  @Autowired protected FactionService factionService;
  @Autowired protected WrestlerService wrestlerService;
  @Autowired protected ShowTemplateRepository showTemplateRepository;
  @Autowired protected ShowTemplateService showTemplateService;
  @Autowired protected AccountRepository accountRepository;
  @Autowired protected RoleRepository roleRepository;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected LeagueRepository leagueRepository;
  @Autowired protected LeagueRosterRepository leagueRosterRepository;
  @Autowired protected DeckRepository deckRepository;
  @Autowired protected RivalryRepository rivalryRepository;
  @Autowired protected WrestlerStateRepository wrestlerStateRepository;
  @Autowired protected ShowTypeService showTypeService;
  @Autowired protected TeamService teamService;
  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected TransactionTemplate transactionTemplate;
  @Autowired protected FactionRivalryRepository factionRivalryRepository;
  @Autowired protected FactionRepository factionRepository;
  @Autowired protected TeamRepository teamRepository;
  @Autowired protected NpcRepository npcRepository;
  @Autowired protected TitleRepository titleRepository;
  @Autowired protected TitleReignRepository titleReignRepository;
  @Autowired protected UniverseRepository universeRepository;
  @Autowired protected UniverseContextService universeContextService;
  @Autowired protected LeagueMembershipRepository leagueMembershipRepository;
  @Autowired protected DraftRepository draftRepository;
  @Autowired protected DraftPickRepository draftPickRepository;
  @Autowired protected MatchFulfillmentRepository matchFulfillmentRepository;
  @Autowired protected CampaignRepository campaignRepository;
  @Autowired protected CampaignStateRepository campaignStateRepository;
  @Autowired protected BackstageActionHistoryRepository backstageActionHistoryRepository;
  @Autowired protected CampaignEncounterRepository campaignEncounterRepository;
  @Autowired protected WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Autowired protected DatabaseCleanup databaseCleanup;
  @Autowired protected DataInitializer dataInitializer;

  protected boolean skipDataInit = false;

  @Autowired(required = false)
  protected CacheManager cacheManager;

  @BeforeEach
  public void setUp() throws Exception {
    clearAllRepositories();
  }

  public Wrestler createTestWrestler(@NonNull String name) {
    return createTestWrestler(name, 0L);
  }

  public Wrestler createTestWrestler(@NonNull String name, @NonNull Long fans) {
    Universe universe =
        universeRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No universe found after reset"));

    Wrestler wrestler = TestUtils.createWrestler(name, fans, universe);
    return wrestlerRepository.saveAndFlush(wrestler);
  }

  protected Account createTestAccount(@NonNull String username, @NonNull RoleName roleName) {
    return createTestAccount(username, "password", roleName);
  }

  protected Account createTestAccount(
      @NonNull String username, @NonNull String password, @NonNull RoleName roleName) {
    Optional<Account> existing = accountRepository.findByUsername(username);
    if (existing.isPresent()) {
      return existing.get();
    }

    Role role =
        roleRepository
            .findByName(roleName)
            .orElseGet(() -> roleRepository.save(new Role(roleName, roleName.name())));

    Account account =
        new Account(username, passwordEncoder.encode(password), username + "@example.com");
    account.setRoles(Collections.singleton(role));
    return accountRepository.save(account);
  }

  protected SegmentNarrationService.SegmentNarrationContext createCustomSegmentContext() {
    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();

    SegmentNarrationService.SegmentTypeContext matchType =
        new SegmentNarrationService.SegmentTypeContext();
    matchType.setSegmentType("Hell in a Cell");
    matchType.setStipulation("King of the Ring 1998");
    matchType.setRules(java.util.Arrays.asList("No Disqualification", "Falls Count Anywhere"));
    context.setSegmentType(matchType);

    SegmentNarrationService.VenueContext venue = new SegmentNarrationService.VenueContext();
    venue.setName("Civic Arena");
    venue.setLocation("Pittsburgh, Pennsylvania");
    venue.setType("Indoor Arena");
    venue.setCapacity(17_000);
    venue.setDescription("Historic venue for legendary matches");
    venue.setAtmosphere("Intense and foreboding");
    venue.setSignificance("Site of the most famous Hell in a Cell segment");
    context.setVenue(venue);

    SegmentNarrationService.WrestlerContext undertaker =
        new SegmentNarrationService.WrestlerContext();
    undertaker.setName("The Undertaker");
    undertaker.setDescription("The Deadman - Phenom of WWE");

    com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext mankind =
        new com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext();
    mankind.setName("Mankind");
    mankind.setDescription("Hardcore legend Mick Foley");

    context.setWrestlers(java.util.Arrays.asList(undertaker, mankind));

    context.setAudience("Shocked and horrified crowd of 17,000");
    context.setDeterminedOutcome(
        "The Undertaker wins after Mankind is thrown off the Hell in a Cell");

    return context;
  }

  protected void clearRepositoriesOnly() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          transactionTemplate.execute(
              status -> {
                log.info("Cleaning up database using DatabaseCleanup (No init)...");
                databaseCleanup.clearRepositories();

                if (cacheManager != null) {
                  log.info("Clearing all caches...");
                  cacheManager
                      .getCacheNames()
                      .forEach(
                          cacheName -> {
                            var cache = cacheManager.getCache(cacheName);
                            if (cache != null) {
                              cache.clear();
                            }
                          });
                }
                return null;
              });
        });
  }

  protected void clearAllRepositories() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          transactionTemplate.execute(
              status -> {
                log.info("Cleaning up database using DatabaseCleanup...");
                databaseCleanup.clearRepositories();

                // Explicitly clear universe table to prevent ID 1 conflicts
                universeRepository.deleteAll();
                universeRepository.flush();
                if (cacheManager != null) {
                  log.info("Clearing all caches...");
                  cacheManager
                      .getCacheNames()
                      .forEach(
                          cacheName -> {
                            var cache = cacheManager.getCache(cacheName);
                            if (cache != null) {
                              cache.clear();
                            }
                          });
                }

                if (!skipDataInit) {
                  log.info("Re-initializing data using DataInitializer...");
                  dataInitializer.init();

                  // Set default universe for tests
                  universeRepository.findAll().stream()
                      .findFirst()
                      .ifPresent(
                          u -> {
                            com.github.javydreamercsw.TestUtils.setDefaultUniverse(u);
                            universeContextService.setCurrentUniverse(u);
                          });
                }

                log.info("Database reset complete.");
                return null;
              });
        });
  }

  protected void cleanupLeagues() {
    clearAllRepositories();
  }
}

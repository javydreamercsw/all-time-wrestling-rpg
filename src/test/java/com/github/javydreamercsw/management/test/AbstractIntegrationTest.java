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
import com.github.javydreamercsw.base.config.TestSecurityContextConfig;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.DatabaseCleanup;
import com.github.javydreamercsw.management.config.TestAIConfiguration;
import com.github.javydreamercsw.management.config.TestNotionConfiguration;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.commentator.CommentatorRepository;
import com.github.javydreamercsw.management.domain.deck.DeckCardRepository;
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
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
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
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.campaign.CampaignChapterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.StorylineDirectorService;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
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
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = Application.class)
@Slf4j
@ActiveProfiles("test")
@Import({TestAIConfiguration.class, TestNotionConfiguration.class, TestSecurityContextConfig.class})
@WithMockUser(
    username = "admin",
    roles = {"ADMIN", "SYSTEM", "BOOKER", "PLAYER", "VIEWER"})
public abstract class AbstractIntegrationTest {

  static {
    // Mark as test environment to stabilize security context
    System.setProperty("is.test", "true");
  }

  @MockitoBean protected VaadinDefaultRequestCache vaadinDefaultRequestCache;
  @MockitoBean protected RequestUtil requestUtil;

  @Autowired protected AuthenticationContext authenticationContext;

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
  @Autowired protected Environment environment;
  @Autowired protected RoleRepository roleRepository;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected LeagueRepository leagueRepository;
  @Autowired protected LeagueRosterRepository leagueRosterRepository;
  @Autowired protected DraftRepository draftRepository;
  @Autowired protected DraftPickRepository draftPickRepository;
  @Autowired protected LeagueMembershipRepository leagueMembershipRepository;
  @Autowired protected MatchFulfillmentRepository matchFulfillmentRepository;
  @Autowired protected FactionRivalryRepository factionRivalryRepository;
  @Autowired protected FactionRepository factionRepository;
  @Autowired protected NpcRepository npcRepository;
  @Autowired protected CardRepository cardRepository;
  @Autowired protected CardSetRepository cardSetRepository;
  @Autowired protected DeckCardRepository deckCardRepository;
  @Autowired protected GameSettingService gameSettingService;
  @Autowired protected FactionRivalryService factionRivalryService;
  @Autowired protected RivalryRepository rivalryRepository;
  @Autowired protected DeckRepository deckRepository;
  @Autowired protected WrestlerStateRepository wrestlerStateRepository;
  @Autowired protected ShowTypeService showTypeService;
  @Autowired protected SegmentRuleRepository segmentRuleRepository;
  @Autowired protected TitleRepository titleRepository;
  @Autowired protected TitleReignRepository titleReignRepository;
  @Autowired protected CommentaryTeamRepository commentaryTeamRepository;
  @Autowired protected CommentatorRepository commentatorRepository;
  @Autowired protected TeamRepository teamRepository;
  @Autowired protected TeamService teamService;
  @Autowired protected UniverseRepository universeRepository;
  @Autowired protected UniverseContextService universeContextService;
  @Autowired protected CampaignRepository campaignRepository;
  @Autowired protected CampaignService campaignService;
  @Autowired protected CampaignStateRepository campaignStateRepository;
  @Autowired protected CampaignChapterService campaignChapterService;
  @Autowired protected StorylineDirectorService storylineDirectorService;
  @Autowired protected DatabaseCleanup databaseCleanup;
  @Autowired protected TransactionTemplate transactionTemplate;
  @PersistenceContext protected EntityManager entityManager;
  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected com.github.javydreamercsw.management.DataInitializer dataInitializer;
  @Autowired protected SegmentNarrationService segmentNarrationService;
  @Autowired protected BackstageActionHistoryRepository backstageActionHistoryRepository;
  @Autowired protected CampaignEncounterRepository campaignEncounterRepository;
  @Autowired protected WrestlerAlignmentRepository wrestlerAlignmentRepository;

  protected Universe defaultUniverse;

  @Value("${data.initializer.enabled:true}")
  protected boolean dataInitializerEnabled;

  @Autowired(required = false)
  protected CacheManager cacheManager;

  @AfterEach
  public void tearDown() throws Exception {
    log.debug("AbstractIntegrationTest.tearDown() called");
    clearSecurityContext();
    clearCache();
  }

  @BeforeEach
  public void baseSetUp() throws Exception {
    log.debug("AbstractIntegrationTest.baseSetUp() called for {}", this.getClass().getSimpleName());

    // 1. Capture original authentication
    Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();

    try {
      // 2. Pre-initialize defaultUniverse from DB if it exists
      universeRepository
          .findByName("Default Universe")
          .ifPresent(
              u -> {
                this.defaultUniverse = u;
                TestUtils.setDefaultUniverse(u);
                universeContextService.setCurrentUniverse(u);
              });

      // 3. Cleanup and Init
      clearAllRepositories();

      // 4. Final verification of universe
      if (this.defaultUniverse == null) {
        ensureDefaultUniverseExists();
      }
    } finally {
      // 5. Ensure original context is restored/maintained
      if (originalAuth != null) {
        SecurityContextHolder.getContext().setAuthentication(originalAuth);
      } else {
        log.info("Establishing default admin context for test execution...");
        loginAs("admin");
      }
    }
  }

  protected void clearAllRepositories() {
    log.debug("AbstractIntegrationTest.clearAllRepositories() called");

    // 1. Reset sequences (H2 specific) for tables that ARE cleared
    try {
      transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      transactionTemplate.execute(
          status -> {
            entityManager
                .createNativeQuery("ALTER TABLE wrestler_state ALTER COLUMN id RESTART WITH 1")
                .executeUpdate();
            entityManager
                .createNativeQuery("ALTER TABLE account ALTER COLUMN id RESTART WITH 1")
                .executeUpdate();
            return null;
          });
    } catch (Exception e) {
      log.trace("Could not reset sequences (might not be H2): {}", e.getMessage());
    }

    // 2. Perform cleanup and init in a new transaction to avoid conflicts
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(
        status -> {
          log.info("Cleaning up database using DatabaseCleanup...");
          databaseCleanup.clearRepositories();

          clearCache();

          // Always ensure at least one universe exists for tests
          ensureDefaultUniverseExists();

          // 3. Re-initialize data
          if (dataInitializerEnabled) {
            dataInitializer.init();
          }
          return null;
        });

    // Clear the entity manager
    if (entityManager != null) {
      entityManager.clear();
    }
  }

  protected void clearCache() {
    if (cacheManager != null) {
      cacheManager
          .getCacheNames()
          .forEach(
              name -> {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                  cache.clear();
                }
              });
    }
  }

  protected void ensureDefaultUniverseExists() {
    synchronized (AbstractIntegrationTest.class) {
      try {
        Universe newUniverse =
            Universe.builder().name("Default Universe").type(Universe.UniverseType.GLOBAL).build();
        newUniverse = universeRepository.saveAndFlush(newUniverse);
        this.defaultUniverse = newUniverse;
        TestUtils.setDefaultUniverse(newUniverse);
        universeContextService.setCurrentUniverse(newUniverse);
      } catch (DataIntegrityViolationException e) {
        universeRepository
            .findByName("Default Universe")
            .ifPresent(
                u -> {
                  this.defaultUniverse = u;
                  TestUtils.setDefaultUniverse(u);
                  universeContextService.setCurrentUniverse(u);
                });
      }
    }
  }

  protected Wrestler createTestWrestler(@NonNull String name) {
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
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseGet(() -> roleRepository.save(new Role(roleName, roleName.name())));

    return accountRepository
        .findByUsername(username)
        .orElseGet(
            () -> {
              Account account =
                  new Account(username, passwordEncoder.encode(password), username + "@test.com");
              account.setRoles(Collections.singleton(role));
              return accountRepository.saveAndFlush(account);
            });
  }

  protected void runAsAdmin(@NonNull Runnable task) {
    GeneralSecurityUtils.runAsAdmin(task);
  }

  protected void login(Object principal) {
    login(principal, Collections.emptyList());
  }

  protected void login(Object principal, Collection<? extends GrantedAuthority> authorities) {
    final Account account;
    final String principalName;

    if (principal instanceof Account a) {
      account = a;
      principalName = a.getUsername();
    } else if (principal instanceof CustomUserDetails ud) {
      account = ud.getAccount();
      principalName = ud.getUsername();
    } else if (principal instanceof UserDetails ud) {
      principalName = ud.getUsername();
      account = accountRepository.findByUsername(principalName).orElse(null);
    } else if (principal instanceof String s) {
      principalName = s;
      if ("anonymousUser".equals(s)) {
        account = null;
      } else {
        account = accountRepository.findByUsername(s).orElse(null);
      }
    } else {
      throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass());
    }

    // Build user details and authorities
    final Object finalPrincipal;
    final Set<SimpleGrantedAuthority> finalAuthorities = new HashSet<>();

    if (account != null) {
      java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
      Wrestler wrestler = wrestlers.isEmpty() ? null : wrestlers.get(0);
      finalPrincipal = new CustomUserDetails(account, wrestler);

      for (Role role : account.getRoles()) {
        finalAuthorities.add(new SimpleGrantedAuthority(role.getName().name()));
        finalAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));
      }
    } else {
      if ("anonymousUser".equals(principalName)) {
        finalPrincipal = principalName;
        finalAuthorities.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
      } else {
        // Build a mock account for missing users (standard @WithMockUser users)
        Account mockAccount = new Account(principalName, "password", principalName + "@test.com");
        mockAccount.setId(-1L);
        finalPrincipal = new CustomUserDetails(mockAccount, null);
      }
    }

    // Add explicit authorities if provided
    if (authorities != null) {
      for (GrantedAuthority authority : authorities) {
        finalAuthorities.add(new SimpleGrantedAuthority(authority.getAuthority()));
        if (!authority.getAuthority().startsWith("ROLE_")) {
          finalAuthorities.add(new SimpleGrantedAuthority("ROLE_" + authority.getAuthority()));
        }
      }
    }

    // Ensure we have at least one authority for a valid authentication object
    if (finalAuthorities.isEmpty()) {
      finalAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(finalPrincipal, "password", finalAuthorities);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);

    SecurityContextHolder.setContext(context);
    TestSecurityContextHolder.setContext(context);
  }

  protected void loginAs(String username) {
    login(username);
  }

  /** Refreshes the current security context by re-loading the authenticated user from the DB. */
  protected void refreshSecurityContext() {
    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
    if (currentAuth == null) {
      currentAuth = TestSecurityContextHolder.getContext().getAuthentication();
    }

    if (currentAuth != null) {
      Object principal = currentAuth.getPrincipal();
      String principalName = null;
      if (principal instanceof CustomUserDetails ud) {
        principalName = ud.getUsername();
      } else if (principal instanceof UserDetails ud) {
        principalName = ud.getUsername();
      } else if (principal instanceof String s) {
        principalName = s;
      }

      if (principalName != null) {
        // Re-create the account if missing (e.g. after wipe)
        final String finalName = principalName;
        accountRepository
            .findByUsername(finalName)
            .orElseGet(
                () -> {
                  log.info("Re-creating missing account '{}' after database wipe...", finalName);
                  Account a = new Account(finalName, "password", finalName + "@test.com");
                  // Give it some default roles based on authorities if possible, or just ROLE_USER
                  return accountRepository.saveAndFlush(a);
                });
      }

      login(currentAuth.getPrincipal(), currentAuth.getAuthorities());
    }
  }

  protected void clearSecurityContext() {
    SecurityContextHolder.clearContext();
    TestSecurityContextHolder.clearContext();
  }

  protected void cleanupLeagues() {
    clearAllRepositories();
  }

  protected void clearRepositoriesOnly() {
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(
        status -> {
          log.debug("Cleaning up database using DatabaseCleanup...");
          databaseCleanup.clearRepositories();

          if (cacheManager != null) {
            log.debug("Clearing all caches...");
            cacheManager
                .getCacheNames()
                .forEach(
                    name -> {
                      var cache = cacheManager.getCache(name);
                      if (cache != null) {
                        cache.clear();
                      }
                    });
          }

          // Always ensure at least one universe exists for tests
          ensureDefaultUniverseExists();

          if (dataInitializerEnabled) {
            log.debug("Re-initializing data using DataInitializer...");
            dataInitializer.init();
          }
          log.debug("Database reset complete.");
          return null;
        });
  }
}

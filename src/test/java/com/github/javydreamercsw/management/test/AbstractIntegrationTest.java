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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = Application.class)
@Slf4j
@WithCustomMockUser(
    username = "admin",
    roles = {"ADMIN"})
@ActiveProfiles("test")
@Import({TestAIConfiguration.class, TestNotionConfiguration.class})
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true)
public abstract class AbstractIntegrationTest {

  static {
    // Enable InheritableThreadLocal to ensure background threads
    // inherit the security context from the parent UI thread.
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
  }

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
  @Autowired protected com.github.javydreamercsw.base.AccountInitializer accountInitializer;

  protected boolean skipDataInit = false;

  @Autowired(required = false)
  protected CacheManager cacheManager;

  @BeforeEach
  public void baseSetUp() throws Exception {
    log.info("AbstractIntegrationTest.baseSetUp() called for {}", this.getClass().getSimpleName());
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

  /**
   * Helper to run a task with ADMIN privileges in tests, synchronizing both standard and test
   * security contexts.
   *
   * @param task The task to run
   */
  protected void runAsAdmin(@NonNull Runnable task) {
    Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
    Authentication originalTestAuth = TestSecurityContextHolder.getContext().getAuthentication();

    // Create a temporary system-like authentication with ADMIN role
    Set<SimpleGrantedAuthority> authorities = new HashSet<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    authorities.add(new SimpleGrantedAuthority("ADMIN"));

    org.springframework.security.core.userdetails.UserDetails systemUser =
        org.springframework.security.core.userdetails.User.withUsername("system")
            .password("password")
            .authorities(authorities)
            .build();

    UsernamePasswordAuthenticationToken adminAuth =
        new UsernamePasswordAuthenticationToken(systemUser, "password", authorities);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(adminAuth);
    SecurityContextHolder.setContext(context);
    TestSecurityContextHolder.setContext(context);

    try {
      task.run();
    } finally {
      // Restore original contexts
      restoreSecurityContext(originalAuth, originalTestAuth);
    }
  }

  protected void login(Account account) {
    java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
    Wrestler wrestler = wrestlers.isEmpty() ? null : wrestlers.get(0);
    com.github.javydreamercsw.base.security.CustomUserDetails principal =
        new com.github.javydreamercsw.base.security.CustomUserDetails(account, wrestler);

    Set<SimpleGrantedAuthority> authorities = new HashSet<>();
    for (Role role : account.getRoles()) {
      authorities.add(new SimpleGrantedAuthority(role.getName().name()));
      authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));
    }

    // Use a non-null credential to ensure fully authenticated status in some Spring versions
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, "password", authorities);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    TestSecurityContextHolder.setContext(context);
  }

  protected void loginAs(String username) {
    accountRepository
        .findByUsername(username)
        .ifPresentOrElse(
            this::login,
            () -> {
              throw new RuntimeException("loginAs: Account not found: " + username);
            });
  }

  /** Refreshes the current security context by re-loading the authenticated user from the DB. */
  protected void refreshSecurityContext() {
    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
    if (currentAuth != null) {
      final String username;
      if (currentAuth.getPrincipal() instanceof Account accountPrincipal) {
        username = accountPrincipal.getUsername();
      } else if (currentAuth.getPrincipal()
          instanceof com.github.javydreamercsw.base.security.CustomUserDetails userDetails) {
        username = userDetails.getUsername();
      } else if (currentAuth.getPrincipal()
          instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
        username = userDetails.getUsername();
      } else {
        username = null;
      }

      if (username != null) {
        accountRepository
            .findByUsername(username)
            .ifPresentOrElse(
                refreshedAccount -> {
                  log.debug(
                      "Refreshing security context for user: {}", refreshedAccount.getUsername());
                  login(refreshedAccount);
                },
                () -> {
                  log.warn("Account not found during refresh: {}, clearing context", username);
                  clearSecurityContext();
                });
      }
    }
  }

  protected void clearSecurityContext() {
    SecurityContextHolder.clearContext();
    TestSecurityContextHolder.clearContext();
  }

  protected void clearCache() {
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
  }

  protected void restoreSecurityContext(
      Authentication originalAuth, Authentication originalTestAuth) {
    if (originalAuth != null) {
      SecurityContext originalContext = SecurityContextHolder.createEmptyContext();
      originalContext.setAuthentication(originalAuth);
      SecurityContextHolder.setContext(originalContext);
    } else {
      SecurityContextHolder.clearContext();
    }

    if (originalTestAuth != null) {
      TestSecurityContextHolder.setAuthentication(originalTestAuth);
    } else {
      TestSecurityContextHolder.clearContext();
    }
  }

  protected void clearRepositoriesOnly() {
    transactionTemplate.execute(
        status -> {
          runAsAdmin(
              () -> {
                log.info("Cleaning up database using DatabaseCleanup (No init)...");
                databaseCleanup.clearRepositories();
                clearCache();
              });
          return null;
        });
  }

  protected void ensureAuthenticatedUserExists() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      String username = null;
      com.github.javydreamercsw.base.security.CustomUserDetails customUserDetails = null;

      if (auth.getPrincipal()
          instanceof com.github.javydreamercsw.base.security.CustomUserDetails userDetails) {
        username = userDetails.getUsername();
        customUserDetails = userDetails;
      } else if (auth.getPrincipal()
          instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
        username = userDetails.getUsername();
      }

      if (username != null) {
        log.info("ensureAuthenticatedUserExists found current user: {}", username);
        Account account = accountRepository.findByUsername(username).orElse(null);
        if (account == null) {
          log.info("Re-creating missing authenticated user in DB: {}", username);
          account =
              new Account(username, passwordEncoder.encode("password"), username + "@example.com");

          Set<Role> roles = new HashSet<>();
          for (org.springframework.security.core.GrantedAuthority authority :
              auth.getAuthorities()) {
            String roleNameStr = authority.getAuthority();
            if (roleNameStr.startsWith("ROLE_")) {
              roleNameStr = roleNameStr.substring(5);
            }
            try {
              RoleName roleName = RoleName.valueOf(roleNameStr);
              roles.add(
                  roleRepository
                      .findByName(roleName)
                      .orElseGet(() -> roleRepository.save(new Role(roleName, roleName.name()))));
            } catch (IllegalArgumentException e) {
              // Not a standard role, skip
            }
          }
          account.setRoles(roles);
          account = accountRepository.saveAndFlush(account);
        }

        // Re-create wrestler if it was in the original context but is now missing from DB
        if (customUserDetails != null && customUserDetails.getWrestler() != null) {
          String wrestlerName = customUserDetails.getWrestler().getName();
          if (wrestlerRepository.findByName(wrestlerName).isEmpty()) {
            log.info("Re-creating missing authenticated wrestler in DB: {}", wrestlerName);
            createTestWrestler(wrestlerName);
            // Link wrestler to account if needed (Account normally has a list or ref)
            final Account finalAccount = account;
            wrestlerRepository
                .findByName(wrestlerName)
                .ifPresent(
                    w -> {
                      w.setAccount(finalAccount);
                      wrestlerRepository.saveAndFlush(w);
                    });
          }
        }
      }
    }
  }

  protected void clearAllRepositories() {
    runAsAdmin(
        () -> {
          transactionTemplate.setPropagationBehavior(
              org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
          transactionTemplate.execute(
              status -> {
                log.info("Cleaning up database using DatabaseCleanup...");
                databaseCleanup.clearRepositories();

                clearCache();
                return null;
              });

          if (!skipDataInit) {
            log.info("Initializing accounts using AccountInitializer...");
            accountInitializer.init();

            log.info("Re-initializing data using DataInitializer...");
            dataInitializer.init();
          }

          // Always ensure at least one universe exists for tests
          transactionTemplate.execute(
              status -> {
                if (universeRepository.count() == 0) {
                  log.info("Creating default universe for test...");
                  universeRepository.saveAndFlush(
                      Universe.builder()
                          .name("Default Universe")
                          .type(Universe.UniverseType.GLOBAL)
                          .build());
                }
                universeRepository.findAll().stream()
                    .findFirst()
                    .ifPresent(
                        u -> {
                          com.github.javydreamercsw.TestUtils.setDefaultUniverse(u);
                          universeContextService.setCurrentUniverse(u);
                        });
                return null;
              });

          log.info("Database reset complete.");
        });
    ensureAuthenticatedUserExists();
  }

  protected void cleanupLeagues() {
    clearAllRepositories();
  }
}

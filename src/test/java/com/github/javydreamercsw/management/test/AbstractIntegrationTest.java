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
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
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
@ActiveProfiles("test")
@Import({TestAIConfiguration.class, TestNotionConfiguration.class})
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
  @Autowired protected Environment environment;
  @Autowired protected RoleRepository roleRepository;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected LeagueRepository leagueRepository;
  @Autowired protected LeagueRosterRepository leagueRosterRepository;
  @Autowired protected DeckRepository deckRepository;
  @Autowired protected RivalryRepository rivalryRepository;
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

  protected Universe defaultUniverse;

  @Value("${data.initializer.enabled:true}")
  protected boolean dataInitializerEnabled;

  @Autowired(required = false)
  protected CacheManager cacheManager;

  @org.junit.jupiter.api.AfterEach
  public void tearDown() throws Exception {
    log.info("AbstractIntegrationTest.tearDown() called");
    clearSecurityContext();
    clearCache();
  }

  @BeforeEach
  public void baseSetUp() throws Exception {
    log.info("AbstractIntegrationTest.baseSetUp() called for {}", this.getClass().getSimpleName());

    // 1. Capture original authentication if set (e.g. by @WithCustomMockUser)
    Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();

    // 2. Absolute clean slate
    clearSecurityContext();

    // 3. Pre-initialize defaultUniverse from DB if it exists
    universeRepository
        .findByName("Default Universe")
        .ifPresent(
            u -> {
              this.defaultUniverse = u;
              TestUtils.setDefaultUniverse(u);
              universeContextService.setCurrentUniverse(u);
            });

    // 4. Cleanup and Init
    clearAllRepositories();

    // 5. Final verification of universe
    if (this.defaultUniverse == null) {
      universeRepository
          .findByName("Default Universe")
          .ifPresent(
              u -> {
                this.defaultUniverse = u;
                TestUtils.setDefaultUniverse(u);
                universeContextService.setCurrentUniverse(u);
              });
    }

    // 6. Restore original context if it was present, otherwise default to admin
    if (originalAuth != null) {
      log.info("Restoring original security context for user: {}", originalAuth.getName());
      SecurityContextHolder.getContext().setAuthentication(originalAuth);
      TestSecurityContextHolder.setAuthentication(originalAuth);
    } else {
      log.info("Establishing default admin context for test...");
      loginAs("admin");
    }
  }

  private static boolean initialDataLoaded = false;

  protected void clearAllRepositories() {
    log.info("AbstractIntegrationTest.clearAllRepositories() called");

    com.github.javydreamercsw.base.security.GeneralSecurityUtils.runAsAdmin(
        () -> {
          // 1. Reset sequence (H2 specific) - Try directly first
          try {
            transactionTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.execute(
                status -> {
                  entityManager
                      .createNativeQuery(
                          "ALTER TABLE wrestler_state ALTER COLUMN id RESTART WITH 1")
                      .executeUpdate();
                  return null;
                });
          } catch (Exception e) {
            log.trace("Could not reset sequence (might not be H2): {}", e.getMessage());
          }

          // 2. Perform cleanup and init in a new transaction to avoid conflicts
          transactionTemplate.setPropagationBehavior(
              org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
          return null;
        });
  }

  protected void clearCache() {
    if (cacheManager != null) {
      cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
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
      } catch (org.springframework.dao.DataIntegrityViolationException e) {
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

    Account account =
        new Account(username, passwordEncoder.encode(password), username + "@test.com");
    account.setRoles(Collections.singleton(role));
    return accountRepository.saveAndFlush(account);
  }

  protected void runAsAdmin(@NonNull Runnable task) {
    com.github.javydreamercsw.base.security.GeneralSecurityUtils.runAsAdmin(task);
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
    if (currentAuth == null) {
      currentAuth = TestSecurityContextHolder.getContext().getAuthentication();
    }

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
      } else if (currentAuth.getPrincipal() instanceof String s) {
        username = s;
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

  void restoreSecurityContext(Authentication originalAuth, Authentication originalTestAuth) {
    if (originalAuth != null) {
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(originalAuth);
      SecurityContextHolder.setContext(context);
    } else {
      SecurityContextHolder.clearContext();
    }

    if (originalTestAuth != null) {
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(originalTestAuth);
      TestSecurityContextHolder.setContext(context);
    } else {
      TestSecurityContextHolder.clearContext();
    }
  }

  protected void ensureAuthenticatedUserExists() {
    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
    if (currentAuth != null && currentAuth.isAuthenticated()) {
      final String username;
      Object principal = currentAuth.getPrincipal();
      if (principal instanceof com.github.javydreamercsw.base.security.CustomUserDetails ud) {
        username = ud.getUsername();
      } else if (principal
          instanceof org.springframework.security.core.userdetails.UserDetails ud) {
        username = ud.getUsername();
      } else if (principal instanceof com.github.javydreamercsw.base.domain.account.Account a) {
        username = a.getUsername();
      } else if (principal instanceof String s) {
        username = s;
      } else {
        username = null;
      }

      if (username != null && !username.equals("system") && !username.equals("anonymousUser")) {
        Account account = accountRepository.findByUsername(username).orElse(null);
        if (account == null) {
          log.info("Re-creating missing authenticated user in DB: {}", username);
          account =
              createTestAccount(
                  username,
                  RoleName.valueOf(
                      currentAuth.getAuthorities().stream()
                          .map(a -> a.getAuthority().replace("ROLE_", ""))
                          .filter(
                              a -> {
                                try {
                                  RoleName.valueOf(a);
                                  return true;
                                } catch (Exception e) {
                                  return false;
                                }
                              })
                          .findFirst()
                          .orElse("PLAYER")));
        }

        // Check if wrestler exists if this is a PLAYER role
        if (currentAuth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_PLAYER"))) {
          final String wrestlerName = username; // Default to username for test consistency
          if (wrestlerRepository.findByName(wrestlerName).isEmpty()) {
            log.info("Re-creating missing authenticated wrestler in DB: {}", wrestlerName);
            Wrestler w = createTestWrestler(wrestlerName);
            w.setAccount(account);
            wrestlerRepository.saveAndFlush(w);
          }
        }
      }
    }
  }

  private void forceLoginAsAdmin() {
    accountRepository
        .findByUsername("admin")
        .ifPresentOrElse(
            account -> {
              java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
              Wrestler wrestler = wrestlers.isEmpty() ? null : wrestlers.get(0);
              com.github.javydreamercsw.base.security.CustomUserDetails principal =
                  new com.github.javydreamercsw.base.security.CustomUserDetails(account, wrestler);

              SecurityContext context = SecurityContextHolder.createEmptyContext();
              List<SimpleGrantedAuthority> authorities =
                  account.getRoles().stream()
                      .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                      .toList();
              UsernamePasswordAuthenticationToken auth =
                  new UsernamePasswordAuthenticationToken(
                      principal, account.getPassword(), authorities);
              context.setAuthentication(auth);

              SecurityContextHolder.setContext(context);
              TestSecurityContextHolder.setContext(context);

              log.debug("Force logged in as admin. Authorities: {}", authorities);
            },
            () -> {
              log.warn("Admin account not found during force login, using system admin context");
              Set<SimpleGrantedAuthority> authorities = new HashSet<>();
              authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
              authorities.add(new SimpleGrantedAuthority("ROLE_SYSTEM"));

              Account systemAccount = new Account("system", "password", "system@example.com");
              systemAccount.setRoles(
                  Set.of(new Role(RoleName.ADMIN, "ADMIN"), new Role(RoleName.SYSTEM, "SYSTEM")));
              com.github.javydreamercsw.base.security.CustomUserDetails principal =
                  new com.github.javydreamercsw.base.security.CustomUserDetails(
                      systemAccount, null);

              UsernamePasswordAuthenticationToken adminAuth =
                  new UsernamePasswordAuthenticationToken(principal, "password", authorities);

              SecurityContext context = SecurityContextHolder.createEmptyContext();
              context.setAuthentication(adminAuth);
              SecurityContextHolder.setContext(context);
              TestSecurityContextHolder.setContext(context);
            });
  }

  protected void cleanupLeagues() {
    clearAllRepositories();
  }

  protected void clearRepositoriesOnly() {
    transactionTemplate.setPropagationBehavior(
        org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(
        status -> {
          com.github.javydreamercsw.base.security.GeneralSecurityUtils.runAsAdmin(
              () -> {
                log.debug("Cleaning up database using DatabaseCleanup...");
                databaseCleanup.clearRepositories();

                if (cacheManager != null) {
                  log.debug("Clearing all caches...");
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

                // Always ensure at least one universe exists for tests
                ensureDefaultUniverseExists();

                if (dataInitializerEnabled) {
                  log.debug("Re-initializing data using DataInitializer...");
                  dataInitializer.init();
                }
                log.debug("Database reset complete.");
                return null;
              });
          return null;
        });
  }
}

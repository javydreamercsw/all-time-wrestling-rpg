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

import com.github.javydreamercsw.Application;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTargetRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.league.DraftPickRepository;
import com.github.javydreamercsw.management.domain.league.DraftRepository;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Collections;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@Slf4j
public abstract class AbstractIntegrationTest {

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
  @Autowired protected AccountRepository accountRepository;
  @Autowired protected RoleRepository roleRepository;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected LeagueRepository leagueRepository;
  @Autowired protected LeagueRosterRepository leagueRosterRepository;
  @Autowired protected LeagueMembershipRepository leagueMembershipRepository;
  @Autowired protected DraftRepository draftRepository;
  @Autowired protected DraftPickRepository draftPickRepository;
  @Autowired protected MatchFulfillmentRepository matchFulfillmentRepository;

  @Autowired
  protected com.github.javydreamercsw.management.domain.campaign.CampaignRepository
      campaignRepository;

  @Autowired
  protected com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository
      campaignStateRepository;

  @Autowired
  protected com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository
      backstageActionHistoryRepository;

  @Autowired
  protected com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository
      campaignEncounterRepository;

  @Autowired
  protected com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository
      wrestlerAlignmentRepository;

  @Autowired protected com.github.javydreamercsw.management.domain.npc.NpcRepository npcRepository;

  @Autowired
  protected com.github.javydreamercsw.management.domain.faction.FactionRepository factionRepository;

  @Autowired
  protected com.github.javydreamercsw.management.domain.team.TeamRepository teamRepository;

  @Autowired protected com.github.javydreamercsw.management.DatabaseCleanup databaseCleanup;
  @Autowired protected com.github.javydreamercsw.management.DataInitializer dataInitializer;

  @Autowired
  protected com.github.javydreamercsw.management.domain.title.TitleReignRepository
      titleReignRepository;

  @Autowired(required = false)
  protected org.springframework.cache.CacheManager cacheManager;

  protected Wrestler createTestWrestler(@NonNull String name) {
    return TestUtils.createWrestler(name);
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

    // Match Type
    SegmentNarrationService.SegmentTypeContext matchType =
        new SegmentNarrationService.SegmentTypeContext();
    matchType.setSegmentType("Hell in a Cell");
    matchType.setStipulation("King of the Ring 1998");
    matchType.setRules(java.util.Arrays.asList("No Disqualification", "Falls Count Anywhere"));
    context.setSegmentType(matchType);

    // Venue
    SegmentNarrationService.VenueContext venue = new SegmentNarrationService.VenueContext();
    venue.setName("Civic Arena");
    venue.setLocation("Pittsburgh, Pennsylvania");
    venue.setType("Indoor Arena");
    venue.setCapacity(17_000);
    venue.setDescription("Historic venue for legendary matches");
    venue.setAtmosphere("Intense and foreboding");
    venue.setSignificance("Site of the most famous Hell in a Cell segment");
    context.setVenue(venue);

    // Wrestlers
    SegmentNarrationService.WrestlerContext undertaker =
        new SegmentNarrationService.WrestlerContext();
    undertaker.setName("The Undertaker");
    undertaker.setDescription("The Deadman - Phenom of WWE");

    com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext mankind =
        new com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext();
    mankind.setName("Mankind");
    mankind.setDescription("Hardcore legend Mick Foley");

    context.setWrestlers(java.util.Arrays.asList(undertaker, mankind));

    // Context
    context.setAudience("Shocked and horrified crowd of 17,000");
    context.setDeterminedOutcome(
        "The Undertaker wins after Mankind is thrown off the Hell in a Cell");

    return context;
  }

  protected void cleanupLeagues() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          log.info("Cleaning up database using DatabaseCleanup...");
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

          log.info("Re-initializing data using DataInitializer...");
          dataInitializer.init();
          log.info("Database reset complete.");
        });
  }
}

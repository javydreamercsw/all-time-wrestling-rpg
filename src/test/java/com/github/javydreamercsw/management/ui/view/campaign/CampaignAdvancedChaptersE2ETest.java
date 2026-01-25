/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.campaign;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.service.campaign.TournamentService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CampaignAdvancedChaptersE2ETest extends AbstractViewTest {

  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private CampaignAbilityCardRepository cardRepository;
  @Autowired private CampaignUpgradeService upgradeService;
  @Autowired private SecurityUtils securityUtils;
  @Autowired private TournamentService tournamentService;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private ObjectMapper objectMapper;

  private Campaign campaign;
  private Wrestler player;

  @BeforeEach
  public void setUp() {
    // Populate Reference Data
    if (showTypeRepository.count() == 0) {
      ShowType st = new ShowType();
      st.setName("Weekly");
      st.setDescription("Weekly Show");
      st.setExpectedMatches(3);
      st.setExpectedPromos(2);
      showTypeRepository.save(st);
    }
    if (segmentTypeRepository.findByName("One on One").isEmpty()) {
      SegmentType st = new SegmentType();
      st.setName("One on One");
      st.setDescription("Standard Match");
      segmentTypeRepository.save(st);
    }

    if (titleRepository.findByName("ATW World").isEmpty()) {
      Title world = new Title();
      world.setName("ATW World");
      world.setDescription("World Championship");
      world.setTier(WrestlerTier.MAIN_EVENTER);
      world.setChampionshipType(ChampionshipType.SINGLE);
      titleRepository.save(world);
    }

    Role bookerRole =
        roleRepository
            .findByName(RoleName.BOOKER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.BOOKER, "Booker")));

    // 1. Create Account & Wrestler
    Account account = new Account();
    account.setUsername("chapteruser");
    account.setPassword("password");
    account.setEmail("chapter@test.com");
    account.setRoles(Collections.singleton(bookerRole));
    account = accountRepository.save(account);

    player =
        Wrestler.builder().name("Advanced Player").startingHealth(100).startingStamina(100).build();
    player.setAccount(account);
    player = wrestlerRepository.save(player);

    // Login as user
    login(account);

    // 2. Start Campaign
    campaign = campaignService.startCampaign(player);
  }

  private void navigateToDashboard() {
    UI.getCurrent().removeAll();
    CampaignDashboardView dashboard =
        new CampaignDashboardView(
            campaignRepository,
            campaignService,
            wrestlerRepository,
            cardRepository,
            upgradeService,
            securityUtils,
            tournamentService,
            objectMapper);
    UI.getCurrent().add(dashboard);
  }

  @Test
  public void testFightingChampionChapterFlow() {
    // 1. Setup criteria for "The Fighting Champion"
    // Entry: isChampion = true
    Title world = titleRepository.findByName("ATW World").get();
    TitleReign reign = new TitleReign();
    reign.setTitle(world);
    reign.getChampions().add(player);
    reign.setStartDate(Instant.now());
    titleReignRepository.save(reign);

    // Force transition
    campaign.getState().setCurrentChapterId("fighting_champion");
    campaignStateRepository.save(campaign.getState());
    assertThat(campaign.getState().getCurrentChapterId()).isEqualTo("fighting_champion");

    // 2. Verify Dashboard
    navigateToDashboard();
    _get(
        Span.class,
        spec -> spec.withId("campaign-chapter-title").withText("Chapter: The Fighting Champion"));

    // 3. Play matches to reach exit criteria (minMatchesPlayed: 5, minVictoryPoints: 20)
    campaign.getState().setVictoryPoints(25);
    campaign.getState().setMatchesPlayed(5);
    campaignStateRepository.save(campaign.getState());

    // 4. Verify Advance button appears
    navigateToDashboard();
    _get(Button.class, spec -> spec.withText("Complete Chapter & Advance"));
  }

  @Test
  public void testGangWarfareChapterFlow() {
    // 1. Setup criteria for "Gang Warfare"
    // Entry: hasFaction = true
    Faction faction = Faction.builder().name("Test Faction").build();
    faction = factionRepository.save(faction);
    player.setFaction(faction);
    wrestlerRepository.save(player);

    // Force transition
    campaign.getState().setCurrentChapterId("gang_warfare");
    campaignStateRepository.save(campaign.getState());
    assertThat(campaign.getState().getCurrentChapterId()).isEqualTo("gang_warfare");

    // 2. Verify Dashboard
    navigateToDashboard();
    _get(
        Span.class,
        spec -> spec.withId("campaign-chapter-title").withText("Chapter: Gang Warfare"));

    // 3. Set Finale state
    campaignService.setFeatureValue(campaign.getState(), "wonFinale", true);
    campaign.getState().setMatchesPlayed(4);
    campaignStateRepository.save(campaign.getState());

    // 4. Verify Advance button appears
    navigateToDashboard();
    _get(Button.class, spec -> spec.withText("Complete Chapter & Advance"));
  }

  @Test
  public void testCorporatePowerTripChapterFlow() {
    // 1. Setup criteria for "Corporate Power Trip"
    // Entry: minVictoryPoints = 15
    campaign.getState().setVictoryPoints(15);
    campaignStateRepository.save(campaign.getState());

    // Force transition
    campaign.getState().setCurrentChapterId("corporate_power_trip");
    campaignStateRepository.save(campaign.getState());
    assertThat(campaign.getState().getCurrentChapterId()).isEqualTo("corporate_power_trip");

    // 2. Verify Dashboard
    navigateToDashboard();
    _get(
        Span.class,
        spec -> spec.withId("campaign-chapter-title").withText("Chapter: Corporate Power Trip"));

    // 3. Set Finale state
    campaignService.setFeatureValue(campaign.getState(), "wonFinale", true);
    campaign.getState().setMatchesPlayed(5);
    campaignStateRepository.save(campaign.getState());

    // 4. Verify Advance button appears
    navigateToDashboard();
    _get(Button.class, spec -> spec.withText("Complete Chapter & Advance"));
  }
}

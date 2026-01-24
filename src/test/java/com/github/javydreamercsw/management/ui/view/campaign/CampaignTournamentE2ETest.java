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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.service.campaign.TournamentService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H4;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CampaignTournamentE2ETest extends AbstractViewTest {

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
  private Campaign campaign;

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
    if (segmentTypeRepository.count() == 0) {
      SegmentType st = new SegmentType();
      st.setName("One on One");
      st.setDescription("Standard Match");
      segmentTypeRepository.save(st);
    }

    Role bookerRole =
        roleRepository
            .findByName(RoleName.BOOKER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.BOOKER, "Booker")));

    // 1. Create Account & Wrestler
    Account account = new Account();
    account.setUsername("tournamentuser");
    account.setPassword("password");
    account.setEmail("tournament@test.com");
    account.setRoles(Collections.singleton(bookerRole));
    account = accountRepository.save(account);

    Wrestler player =
        Wrestler.builder()
            .name("Tournament Player")
            .startingHealth(100)
            .startingStamina(100)
            .build();
    player.setAccount(account);
    player = wrestlerRepository.save(player);

    // Create opponents (5 opponents -> 6 total -> Bracket size 8)
    for (int i = 1; i <= 5; i++) {
      wrestlerRepository.save(
          Wrestler.builder()
              .name("Opponent " + i)
              .startingHealth(100)
              .startingStamina(100)
              .build());
    }

    // Login as user
    login(account);

    // 2. Start Campaign
    campaign = campaignService.startCampaign(player);

    // 3. Force Chapter 2 (Tournament)
    CampaignState state = campaign.getState();
    state.setCurrentChapterId("ch2_tournament");
    campaignStateRepository.save(state);

    // Force re-fetch to ensure tournament init logic runs when service is called
    campaign = campaignRepository.findById(campaign.getId()).get();
  }

  @Test
  public void testTournamentFullFlow() {
    // 1. Manually instantiate Dashboard
    CampaignDashboardView dashboard =
        new CampaignDashboardView(
            campaignRepository,
            campaignService,
            wrestlerRepository,
            cardRepository,
            upgradeService,
            securityUtils,
            tournamentService);

    UI.getCurrent().add(dashboard);

    // Initial State: Round 1
    // 5 Opponents + 1 Player = 6 Total. Bracket Size 8.
    // 3 Rounds (R1: 4 matches, R2: 2 matches, R3: 1 match).
    int expectedRounds = 3;

    for (int round = 1; round <= expectedRounds; round++) {
      // Determine expected title
      String expectedTitle = "Round " + round;

      // Verify Bracket Header
      _get(H4.class, spec -> spec.withText("Tournament Bracket (" + expectedTitle + ")"));

      // Play Match
      Button playButton =
          _get(
              Button.class,
              spec -> spec.withPredicate(b -> b.getText().startsWith("Play Tournament Match")));
      playButton.click();

      // Verify Continue Button exists (proving match created)
      _get(
          Button.class,
          spec -> spec.withPredicate(b -> b.getText().startsWith("Continue Tournament Match")));

      // Simulate Match Win (Bypass MatchView)
      campaignService.processMatchResult(campaign, true);

      // Reload Dashboard (simulating return from MatchView)
      UI.getCurrent().removeAll();
      dashboard =
          new CampaignDashboardView(
              campaignRepository,
              campaignService,
              wrestlerRepository,
              cardRepository,
              upgradeService,
              securityUtils,
              tournamentService);
      UI.getCurrent().add(dashboard);

      if (round < expectedRounds) {
        // Verify "Advance to Next Day" button
        Button advanceButton =
            _get(Button.class, spec -> spec.withText("Match Complete - Advance to Next Day"));

        // Click Advance (Clears match, advances day, refreshes UI)
        advanceButton.click();
      } else {
        // Final Round (Champion)

        // Verify Champion Message
        _get(
            com.vaadin.flow.component.html.Span.class,
            spec -> spec.withText("🏆 You are the Tournament Champion!"));

        // Verify Chapter Completion Button
        _get(Button.class, spec -> spec.withText("Complete Chapter & Advance"));
      }
    }
  }
}

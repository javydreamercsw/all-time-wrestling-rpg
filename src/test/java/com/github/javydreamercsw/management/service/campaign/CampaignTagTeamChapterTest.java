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
package com.github.javydreamercsw.management.service.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CampaignTagTeamChapterTest extends AbstractViewTest {

  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleService titleService;

  private Campaign campaign;

  @BeforeEach
  public void setUp() {
    super.setupKaribu();

    Role bookerRole =
        roleRepository
            .findByName(RoleName.BOOKER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.BOOKER, "Booker")));

    // 1. Create Account & Wrestler
    Account account = new Account();
    account.setUsername("taguser");
    account.setPassword("password");
    account.setEmail("tag@test.com");
    account.setRoles(Collections.singleton(bookerRole));
    account = accountRepository.save(account);

    Wrestler player =
        Wrestler.builder().name("Tag Player").startingHealth(100).startingStamina(100).build();
    player.setAccount(account);
    player = wrestlerRepository.save(player);

    // Create Team
    Wrestler w1 = wrestlerRepository.save(Wrestler.builder().name("W1").build());
    Wrestler w2 = wrestlerRepository.save(Wrestler.builder().name("W2").build());
    Team team = new Team();
    team.setName("Test Team");
    team.setWrestler1(w1);
    team.setWrestler2(w2);
    team.setStatus(TeamStatus.ACTIVE);
    teamRepository.save(team);

    // Ensure Tag Title Exists
    if (titleRepository.findByName("ATW Tag Team").isEmpty()) {
      titleService.createTitle(
          "ATW Tag Team", "Tag", WrestlerTier.MIDCARDER, ChampionshipType.TEAM);
    }

    // Login as user
    login(account);

    // 2. Start Campaign
    campaign = campaignService.startCampaign(player);
  }

  @Test
  public void testFailChapter1AndEnterTagChapter() {
    CampaignState state = campaign.getState();
    // Simulate Chapter 1 Failure
    // Criteria: minMatchesPlayed: 5, maxVictoryPoints: 4
    state.setMatchesPlayed(5);
    state.setVictoryPoints(3); // Less than 4
    state.setWins(1);
    campaignStateRepository.save(state);

    // Advance Chapter
    // Should detect "Sent to Development" exit from Ch1
    // And "Sent to Development" entry for Ch2 Tag Team
    campaignService.advanceChapter(campaign);

    // Reload
    campaign = campaignRepository.findById(campaign.getId()).get();
    state = campaign.getState();

    assertThat(state.getCurrentChapterId()).isEqualTo("ch2_tag_team");

    // Verify Tag Team Chapter Initialization
    // 1. Tag Team Title should be awarded (was vacant)
    Title tagTitle = titleRepository.findByName("ATW Tag Team").get();
    assertThat(tagTitle.isVacant()).isFalse();
    assertThat(tagTitle.getCurrentChampions()).isNotEmpty();

    // 2. Partner ID should be null (recruiting)
    assertThat(state.getPartnerId()).isNull();
    assertThat(state.isRecruitingPartner()).isTrue();
  }
}

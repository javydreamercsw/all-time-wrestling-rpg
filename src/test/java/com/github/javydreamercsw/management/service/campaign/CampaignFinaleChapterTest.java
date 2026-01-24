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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CampaignFinaleChapterTest extends AbstractMockUserIntegrationTest {

  @Autowired private CampaignService campaignService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private SecurityUtils securityUtils;
  @Autowired private ObjectMapper objectMapper;

  private Campaign campaign;
  private Wrestler player;
  private Wrestler rival;

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
    if (segmentRuleRepository.findByName("Barbwire Exploding Deathmatch").isEmpty()) {
      SegmentRule rule = new SegmentRule();
      rule.setName("Barbwire Exploding Deathmatch");
      rule.setDescription("Explosions!");
      segmentRuleRepository.save(rule);
    }

    Role bookerRole =
        roleRepository
            .findByName(RoleName.BOOKER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.BOOKER, "Booker")));

    Account account = new Account();
    account.setUsername("finaleuser");
    account.setPassword("password");
    account.setEmail("finale@test.com");
    account.setRoles(Collections.singleton(bookerRole));
    account = accountRepository.save(account);

    player =
        Wrestler.builder().name("Finale Player").startingHealth(100).startingStamina(100).build();
    player.setAccount(account);
    player = wrestlerRepository.save(player);

    rival = wrestlerRepository.save(Wrestler.builder().name("Evil Rival").build());

    login(account);

    campaign = campaignService.startCampaign(player);

    CampaignState state = campaign.getState();
    state.setCurrentChapterId("betrayal");
    state.setVictoryPoints(0);

    campaignStateRepository.save(state);

    // Reload
    campaign = campaignRepository.findById(campaign.getId()).get();
  }

  @Test
  public void testFinaleTriggerAndWin() throws JsonProcessingException {
    CampaignState state = campaign.getState();

    // 1. Earn VP to trigger Finale (Need 12)
    state.setVictoryPoints(12);
    state.setMatchesPlayed(2); // Set some matches

    campaignStateRepository.save(state);

    // Let's play a match.
    // Create match

    campaignService.createMatchForEncounter(campaign, "Evil Rival", "Buildup Match", "One on One");

    // Win match (VP 12 -> 16).
    // processMatchResult: checks VP >= 12. Sets finalsPhase = true.

    campaignService.processMatchResult(campaign, true);

    // Reload
    campaign = campaignRepository.findById(campaign.getId()).get();

    state = campaign.getState();

    // Parse feature data
    Map<String, Object> featureData =
        objectMapper.readValue(state.getFeatureData(), new TypeReference<>() {});

    assertThat(featureData.get("finalsPhase")).isEqualTo(true);
    assertThat(featureData.get("wonFinale")).isNull(); // or false depending on initialization

    // Complete Post Match
    campaignService.completePostMatch(campaign);

    // Now create next match (Finale)
    // Should be Barbwire Exploding Deathmatch

    Segment finaleMatch =
        campaignService.createMatchForEncounter(campaign, "Evil Rival", "The Finale", "One on One");

    // Verify Segment Type/Rules forced
    // SegmentType might still be "One on One" if "Barbwire Exploding Deathmatch" is a RULE or TYPE.
    // JSON: finalMatchType: "One on One", finalMatchRules: ["Barbwire Exploding Deathmatch"].

    assertThat(finaleMatch.getSegmentType().getName()).isEqualTo("One on One");

    assertThat(finaleMatch.getSegmentRules())
        .extracting("name")
        .contains("Barbwire Exploding Deathmatch");

    // Win Finale
    campaignService.processMatchResult(campaign, true);

    // Verify Won Finale
    campaign = campaignRepository.findById(campaign.getId()).get();
    state = campaign.getState();

    featureData = objectMapper.readValue(state.getFeatureData(), new TypeReference<>() {});

    assertThat(featureData.get("wonFinale")).isEqualTo(true);
    assertThat(featureData.get("finalsPhase")).isEqualTo(false);

    // Verify Chapter Complete
    boolean complete = campaignService.isChapterComplete(campaign);
    assertThat(complete).isTrue();
  }
}

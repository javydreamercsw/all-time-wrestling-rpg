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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private CampaignStateRepository campaignStateRepository;
  @Mock private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Mock private CampaignChapterService chapterService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private AlignmentService alignmentService;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseRepository universeRepository;
  @Mock private FeatureDataService featureDataService;

  @InjectMocks private CampaignService campaignService;

  @org.junit.jupiter.api.BeforeEach
  void setUpFeatureDataMock() {
    // Return the defaultValue argument so Boolean auto-unboxing never receives null
    org.mockito.Mockito.lenient()
        .when(
            featureDataService.getFeatureValue(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
        .thenAnswer(inv -> inv.getArgument(3));
  }

  @Test
  void testStartCampaign() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    // Initialize lazy collections to avoid NPE if service checks them
    wrestler.setReigns(new LinkedHashSet<>());

    Universe universe = Universe.builder().name("Default").build();
    universe.setId(1L);

    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe));

    when(campaignRepository.save(any(Campaign.class)))
        .thenAnswer(
            i -> {
              Campaign c = i.getArgument(0);
              if (c.getState() != null) {
                c.getState().setFeatureData("{}"); // Mock empty JSON
              }
              return c;
            });
    lenient()
        .when(campaignRepository.findActiveByWrestler(any()))
        .thenReturn(java.util.Optional.empty());

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    lenient()
        .when(wrestlerAlignmentRepository.findByWrestler(any()))
        .thenReturn(Optional.of(alignment));
    lenient().when(wrestlerAlignmentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    // Fix: Mock findById for startCampaign re-fetch
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler));

    Campaign campaign = campaignService.startCampaign(wrestler);

    assertThat(campaign).isNotNull();
    assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    assertThat(campaign.getState()).isNotNull();
    assertThat(campaign.getState().getPendingL1Picks()).isZero(); // Neutral start
    // Regression: campaign must be stamped with the current universe so it
    // appears in the CampaignListView universe filter
    assertThat(campaign.getUniverse()).isNotNull();
    assertThat(campaign.getUniverse().getId()).isEqualTo(1L);

    verify(campaignRepository, atLeastOnce()).save(any(Campaign.class));
    verify(campaignStateRepository, atLeastOnce()).save(any(CampaignState.class));
  }

  @Test
  void testShiftAlignment_NeutralToFace() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    campaignService.shiftAlignment(campaign, 1);
    verify(alignmentService).shiftAlignment(campaign, 1);
  }

  @Test
  void testShiftAlignment_FaceToNeutral() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    campaignService.shiftAlignment(campaign, -1);
    verify(alignmentService).shiftAlignment(campaign, -1);
  }

  @Test
  void testShiftAlignment_NeutralToHeel() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);

    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    campaignService.shiftAlignment(campaign, -1);
    verify(alignmentService).shiftAlignment(campaign, -1);
  }
}

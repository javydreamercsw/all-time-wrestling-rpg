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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignProgressionServiceTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private CampaignStateRepository campaignStateRepository;
  @Mock private CampaignChapterService chapterService;
  @Mock private TournamentService tournamentService;
  @Mock private TitleRepository titleRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TitleService titleService;
  @Mock private StorylineDirectorService storylineDirectorService;
  @Mock private WrestlerStatusService wrestlerStatusService;
  @Mock private FeatureDataService featureDataService;
  @Mock private CampaignService campaignService;

  @InjectMocks private CampaignProgressionService service;

  @BeforeEach
  void setUpMocks() {
    // campaignService is @Lazy field-injected; @InjectMocks won't set it automatically
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "campaignService", campaignService);
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
  void testAdvanceChapter() {
    Wrestler wrestler = new Wrestler();
    wrestler.setReigns(new LinkedHashSet<>());
    Campaign campaign = new Campaign();
    campaign.setId(1L);
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setCurrentChapterId("ch1");
    state.setCampaign(campaign);
    campaign.setState(state);

    when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));

    CampaignChapterDTO ch2 = new CampaignChapterDTO();
    ch2.setId("ch2");
    when(chapterService.findAvailableChapters(state)).thenReturn(List.of(ch2));

    service.advanceChapter(campaign);

    assertThat(state.getCurrentChapterId()).isEqualTo("ch2");
    assertThat(state.getMatchesPlayed()).isZero();
    verify(campaignStateRepository).save(state);
  }
}

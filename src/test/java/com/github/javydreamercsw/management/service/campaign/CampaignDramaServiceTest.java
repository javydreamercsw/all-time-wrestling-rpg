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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
class CampaignDramaServiceTest {

  @Mock private DramaEventService dramaEventService;

  @Mock
  private com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository
      wrestlerRepository;

  @Mock private CampaignService campaignService;
  @Mock private UniverseContextService universeContextService;
  @Mock private FeatureDataService featureDataService;
  @Mock private Random random;

  @InjectMocks private CampaignDramaService campaignDramaService;

  private Campaign campaign;
  private Wrestler player;
  private Wrestler rival;

  @BeforeEach
  void setUp() {
    player = new Wrestler();
    player.setId(1L);
    player.setName("Player Wrestler");

    rival = new Wrestler();
    rival.setId(2L);
    rival.setName("Rival Wrestler");

    CampaignState state = new CampaignState();
    state.setCurrentChapterId("ch1_rookie");

    campaign = new Campaign();
    campaign.setId(10L);
    campaign.setWrestler(player);
    campaign.setState(state);

    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    // Return the defaultValue arg so Boolean unboxing never receives null
    org.mockito.Mockito.lenient()
        .when(featureDataService.getFeatureValue(any(), any(), any(), any()))
        .thenAnswer(inv -> inv.getArgument(3));
  }

  @Test
  void checkForStoryEvents_noCurrentChapter_returnsEmpty() {
    when(campaignService.getCurrentChapter(campaign)).thenReturn(Optional.empty());

    Optional<DramaEvent> result = campaignDramaService.checkForStoryEvents(campaign);

    assertThat(result).isEmpty();
    verify(dramaEventService, never())
        .createDramaEvent(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void checkForStoryEvents_tournamentChapterWithNoRivalry_triggersRivalEvent() {
    CampaignChapterDTO chapter = new CampaignChapterDTO();
    chapter.setId("ch_tournament");
    chapter.setTournament(true);
    when(campaignService.getCurrentChapter(campaign)).thenReturn(Optional.of(chapter));

    // Player has no active rivalries (rivalriesAsWrestler1 and rivalriesAsWrestler2 are empty by
    // default)
    when(wrestlerRepository.findRandomExcluding(eq(1L), any())).thenReturn(List.of(rival));

    DramaEvent event = new DramaEvent();
    when(dramaEventService.createDramaEvent(
            eq(1L),
            eq(2L),
            eq(DramaEventType.CAMPAIGN_RIVAL),
            eq(DramaEventSeverity.NEGATIVE),
            any(),
            any(),
            eq(1L)))
        .thenReturn(Optional.of(event));

    Optional<DramaEvent> result = campaignDramaService.checkForStoryEvents(campaign);

    assertThat(result).isPresent();
    verify(dramaEventService)
        .createDramaEvent(
            eq(1L),
            eq(2L),
            eq(DramaEventType.CAMPAIGN_RIVAL),
            eq(DramaEventSeverity.NEGATIVE),
            any(),
            any(),
            eq(1L));
  }

  @Test
  void checkForStoryEvents_chapter3WithHighRandom_triggersOutsiderEvent() {
    CampaignChapterDTO chapter = new CampaignChapterDTO();
    chapter.setId("ch3_outsider");
    when(campaignService.getCurrentChapter(campaign)).thenReturn(Optional.of(chapter));
    campaign.getState().setCurrentChapterId("ch3_outsider");

    // Random returns value < 0.2 → triggers outsider event
    when(random.nextDouble()).thenReturn(0.1);
    when(wrestlerRepository.findRandomExcluding(eq(1L), any())).thenReturn(List.of(rival));

    DramaEvent event = new DramaEvent();
    when(dramaEventService.createDramaEvent(
            eq(1L),
            eq(2L),
            eq(DramaEventType.CAMPAIGN_OUTSIDER),
            eq(DramaEventSeverity.MAJOR),
            any(),
            any(),
            eq(1L)))
        .thenReturn(Optional.of(event));

    Optional<DramaEvent> result = campaignDramaService.checkForStoryEvents(campaign);

    assertThat(result).isPresent();
  }

  @Test
  void checkForStoryEvents_chapter3WithLowRandom_returnsEmpty() {
    CampaignChapterDTO chapter = new CampaignChapterDTO();
    chapter.setId("ch3_outsider");
    when(campaignService.getCurrentChapter(campaign)).thenReturn(Optional.of(chapter));
    campaign.getState().setCurrentChapterId("ch3_outsider");

    // Random returns value >= 0.2 → no event
    when(random.nextDouble()).thenReturn(0.5);

    Optional<DramaEvent> result = campaignDramaService.checkForStoryEvents(campaign);

    assertThat(result).isEmpty();
    verify(dramaEventService, never())
        .createDramaEvent(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void triggerRivalEvent_noRivalsAvailable_returnsEmpty() {
    when(wrestlerRepository.findRandomExcluding(eq(1L), any())).thenReturn(List.of());

    Optional<DramaEvent> result = campaignDramaService.triggerRivalEvent(campaign);

    assertThat(result).isEmpty();
    verify(dramaEventService, never())
        .createDramaEvent(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void triggerRivalEvent_rivalFound_createsEvent() {
    when(wrestlerRepository.findRandomExcluding(eq(1L), any())).thenReturn(List.of(rival));

    DramaEvent event = new DramaEvent();
    when(dramaEventService.createDramaEvent(
            eq(1L),
            eq(2L),
            eq(DramaEventType.CAMPAIGN_RIVAL),
            eq(DramaEventSeverity.NEGATIVE),
            any(),
            any(),
            anyLong()))
        .thenReturn(Optional.of(event));

    Optional<DramaEvent> result = campaignDramaService.triggerRivalEvent(campaign);

    assertThat(result).isPresent();
  }

  @Test
  void triggerOutsiderEvent_noOpponentsAvailable_returnsEmpty() {
    when(wrestlerRepository.findRandomExcluding(eq(1L), any())).thenReturn(List.of());

    Optional<DramaEvent> result = campaignDramaService.triggerOutsiderEvent(campaign);

    assertThat(result).isEmpty();
  }

  @Test
  void triggerOutsiderEvent_outsiderFound_createsEvent() {
    when(wrestlerRepository.findRandomExcluding(eq(1L), any())).thenReturn(List.of(rival));

    DramaEvent event = new DramaEvent();
    when(dramaEventService.createDramaEvent(
            eq(1L),
            eq(2L),
            eq(DramaEventType.CAMPAIGN_OUTSIDER),
            eq(DramaEventSeverity.MAJOR),
            any(),
            any(),
            anyLong()))
        .thenReturn(Optional.of(event));

    Optional<DramaEvent> result = campaignDramaService.triggerOutsiderEvent(campaign);

    assertThat(result).isPresent();
  }

  @Test
  void triggerRivalEvent_onlyPlayerInRoster_returnsEmpty() {
    // DB query excludes the player, so returns empty
    when(wrestlerRepository.findRandomExcluding(eq(1L), any())).thenReturn(List.of());

    Optional<DramaEvent> result = campaignDramaService.triggerRivalEvent(campaign);

    assertThat(result).isEmpty();
  }
}

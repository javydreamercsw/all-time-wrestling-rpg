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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.grid.Grid;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class CampaignListViewTest extends AbstractViewTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseRepository universeRepository;

  private Universe universe1;
  private Universe universe2;
  private Campaign campaignInUniverse1;
  private Campaign campaignInUniverse2;

  @BeforeEach
  public void setUp() {
    universe1 = Universe.builder().name("Universe One").build();
    universe1.setId(1L);

    universe2 = Universe.builder().name("Universe Two").build();
    universe2.setId(2L);

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler Alpha");

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler Beta");

    campaignInUniverse1 =
        Campaign.builder()
            .wrestler(wrestler1)
            .universe(universe1)
            .status(CampaignStatus.ACTIVE)
            .build();
    campaignInUniverse1.setId(1L);

    campaignInUniverse2 =
        Campaign.builder()
            .wrestler(wrestler2)
            .universe(universe2)
            .status(CampaignStatus.ACTIVE)
            .build();
    campaignInUniverse2.setId(2L);

    // Default: universe 1 is active
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    when(universeRepository.findById(1L)).thenReturn(Optional.of(universe1));
    when(universeRepository.findById(2L)).thenReturn(Optional.of(universe2));
    when(campaignRepository.findByUniverse(universe1)).thenReturn(List.of(campaignInUniverse1));
    when(campaignRepository.findByUniverse(universe2)).thenReturn(List.of(campaignInUniverse2));
  }

  @Test
  void testGridShowsOnlyCampaignsForCurrentUniverse() {
    CampaignListView view =
        new CampaignListView(campaignRepository, universeContextService, universeRepository);

    Grid<Campaign> grid = view.grid;
    List<Campaign> items = grid.getGenericDataView().getItems().toList();

    assertThat(items).hasSize(1);
    assertThat(items.get(0).getWrestler().getName()).isEqualTo("Wrestler Alpha");
    assertThat(items.get(0).getUniverse().getId()).isEqualTo(1L);
  }

  @Test
  void testGridUpdatesWhenUniverseSwitches() {
    CampaignListView view =
        new CampaignListView(campaignRepository, universeContextService, universeRepository);

    // Initially shows universe 1 campaign
    List<Campaign> initialItems = view.grid.getGenericDataView().getItems().toList();
    assertThat(initialItems).hasSize(1);
    assertThat(initialItems.get(0).getWrestler().getName()).isEqualTo("Wrestler Alpha");

    // Simulate universe switch to universe 2
    when(universeContextService.getCurrentUniverseId()).thenReturn(2L);
    view.refreshGrid();

    List<Campaign> updatedItems = view.grid.getGenericDataView().getItems().toList();
    assertThat(updatedItems).hasSize(1);
    assertThat(updatedItems.get(0).getWrestler().getName()).isEqualTo("Wrestler Beta");
    assertThat(updatedItems.get(0).getUniverse().getId()).isEqualTo(2L);
  }

  @Test
  void testGridIsEmptyWhenUniverseHasNoCampaigns() {
    Universe emptyUniverse = Universe.builder().name("Empty Universe").build();
    emptyUniverse.setId(3L);

    when(universeContextService.getCurrentUniverseId()).thenReturn(3L);
    when(universeRepository.findById(3L)).thenReturn(Optional.of(emptyUniverse));
    when(campaignRepository.findByUniverse(emptyUniverse)).thenReturn(List.of());

    CampaignListView view =
        new CampaignListView(campaignRepository, universeContextService, universeRepository);

    List<Campaign> items = view.grid.getGenericDataView().getItems().toList();
    assertThat(items).isEmpty();
  }
}

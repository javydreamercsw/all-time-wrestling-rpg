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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgrade;
import com.github.javydreamercsw.management.domain.campaign.CampaignUpgradeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignUpgradeServiceTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private CampaignUpgradeRepository upgradeRepository;
  @Mock private CampaignStateRepository stateRepository;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;

  private CampaignUpgradeService service;

  @BeforeEach
  void setUp() {
    service =
        new CampaignUpgradeService(
            objectMapper,
            upgradeRepository,
            stateRepository,
            wrestlerRepository,
            wrestlerService,
            wrestlerStateRepository);
  }

  // ── loadUpgrades ────────────────────────────────────────────────────────────

  @Test
  void loadUpgrades_ioException_logsErrorAndDoesNotThrow() throws Exception {
    // Simulate an I/O failure while parsing the resource file.
    doThrow(new IOException("parse error"))
        .when(objectMapper)
        .readValue(
            any(InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class));

    assertThatCode(() -> service.loadUpgrades()).doesNotThrowAnyException();
  }

  @Test
  void loadUpgrades_ioException_doesNotInteractWithRepository() throws Exception {
    // When parsing fails, the repository must not be modified.
    doThrow(new IOException("parse error"))
        .when(objectMapper)
        .readValue(
            any(InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class));

    service.loadUpgrades();

    verify(upgradeRepository, never()).deleteAllInBatch();
    verify(upgradeRepository, never()).saveAll(any());
  }

  @Test
  void loadUpgrades_success_savesUpgrades() throws Exception {
    // When the file is found and parsed, upgrades are persisted.
    List<CampaignUpgrade> upgrades =
        List.of(CampaignUpgrade.builder().name("Iron Chin").type("DEFENSE").build());
    when(objectMapper.readValue(
            any(InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(upgrades);

    service.loadUpgrades();

    verify(upgradeRepository).deleteAllInBatch();
    verify(upgradeRepository).saveAll(upgrades);
  }

  // ── getAllUpgrades ───────────────────────────────────────────────────────────

  @Test
  void getAllUpgrades_delegatesToRepository() {
    List<CampaignUpgrade> expected = List.of(new CampaignUpgrade());
    when(upgradeRepository.findAll()).thenReturn(expected);

    List<CampaignUpgrade> result = service.getAllUpgrades();

    assertThat(result).isEqualTo(expected);
    verify(upgradeRepository).findAll();
  }

  // ── purchaseUpgrade ──────────────────────────────────────────────────────────

  @Test
  void purchaseUpgrade_insufficientTokens_throwsIllegalStateException() {
    CampaignState state =
        CampaignState.builder().skillTokens(5).upgrades(new ArrayList<>()).build();
    Campaign campaign = Campaign.builder().state(state).build();

    assertThatThrownBy(() -> service.purchaseUpgrade(campaign, 1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("skill tokens");
  }

  @Test
  void purchaseUpgrade_upgradeNotFound_throwsIllegalArgumentException() {
    CampaignState state =
        CampaignState.builder().skillTokens(10).upgrades(new ArrayList<>()).build();
    Campaign campaign = Campaign.builder().state(state).build();

    when(upgradeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.purchaseUpgrade(campaign, 99L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Upgrade not found");
  }

  @Test
  void purchaseUpgrade_alreadyHasSameType_throwsIllegalStateException() {
    CampaignUpgrade existing = CampaignUpgrade.builder().id(1L).type("STAMINA").build();
    List<CampaignUpgrade> ownedUpgrades = new ArrayList<>();
    ownedUpgrades.add(existing);

    CampaignState state = CampaignState.builder().skillTokens(10).upgrades(ownedUpgrades).build();

    CampaignUpgrade requested = CampaignUpgrade.builder().id(2L).type("STAMINA").build();
    Campaign campaign = Campaign.builder().state(state).build();

    when(upgradeRepository.findById(2L)).thenReturn(Optional.of(requested));

    assertThatThrownBy(() -> service.purchaseUpgrade(campaign, 2L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already have a permanent upgrade");
  }

  @Test
  void purchaseUpgrade_success_deductsTokensAndSavesState() {
    CampaignState state =
        CampaignState.builder().skillTokens(10).upgrades(new ArrayList<>()).build();
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    Campaign campaign = Campaign.builder().state(state).wrestler(wrestler).build();

    CampaignUpgrade upgrade =
        CampaignUpgrade.builder().id(1L).type("DEFENSE").name("Iron Chin").build();
    when(upgradeRepository.findById(1L)).thenReturn(Optional.of(upgrade));
    when(stateRepository.save(any())).thenReturn(state);

    service.purchaseUpgrade(campaign, 1L);

    assertThat(state.getSkillTokens()).isEqualTo(2);
    assertThat(state.getUpgrades()).contains(upgrade);
    verify(stateRepository).save(state);
  }

  @Test
  void purchaseUpgrade_healthUpgrade_resetsWrestlerHealth() {
    CampaignState state =
        CampaignState.builder().skillTokens(10).upgrades(new ArrayList<>()).build();
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Big Show");
    Campaign campaign = Campaign.builder().state(state).wrestler(wrestler).universe(null).build();

    CampaignUpgrade upgrade =
        CampaignUpgrade.builder().id(5L).type("HEALTH").name("Iron Body").build();
    when(upgradeRepository.findById(5L)).thenReturn(Optional.of(upgrade));

    WrestlerState wrestlerState = WrestlerState.builder().build();
    when(wrestlerService.getOrCreateState(nullable(Long.class), anyLong()))
        .thenReturn(wrestlerState);
    when(wrestlerStateRepository.save(any())).thenReturn(wrestlerState);
    when(stateRepository.save(any())).thenReturn(state);

    service.purchaseUpgrade(campaign, 5L);

    verify(wrestlerService).getOrCreateState(nullable(Long.class), anyLong());
    verify(wrestlerStateRepository).save(wrestlerState);
  }

  // ── init ────────────────────────────────────────────────────────────────────

  @Test
  void init_callsLoadUpgrades() throws Exception {
    // init() delegates to loadUpgrades(). Stub the mapper so the method completes cleanly.
    when(objectMapper.readValue(
            any(InputStream.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(List.of());

    assertThatCode(() -> service.init()).doesNotThrowAnyException();

    verify(upgradeRepository).deleteAllInBatch();
  }
}

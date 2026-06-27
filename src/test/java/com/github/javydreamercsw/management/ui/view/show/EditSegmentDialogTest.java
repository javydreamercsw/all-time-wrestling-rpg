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
package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.UI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditSegmentDialogTest {

  private ProposedSegment segment;
  private WrestlerRepository wrestlerRepository;
  private WrestlerService wrestlerService;
  private TitleService titleService;
  private SegmentTypeRepository segmentTypeRepository;
  private SegmentRuleRepository segmentRuleRepository;
  private NpcService npcService;
  private Runnable onSave;
  private SegmentType matchType;
  private UI ui;

  @BeforeEach
  public void setUp() {
    // Mock the UI context
    ui = mock(UI.class);
    lenient().when(ui.getUI()).thenReturn(Optional.of(ui));
    UI.setCurrent(ui);

    segment = new ProposedSegment();
    segment.setType("Match");
    segment.setSummary("Original Summary");
    segment.setNarration("Original Narration");
    segment.setIsTitleSegment(false);
    segment.setTeams(List.of(List.of("Wrestler 1"), List.of("Wrestler 2")));

    wrestlerRepository = mock(WrestlerRepository.class);
    wrestlerService = mock(WrestlerService.class);
    titleService = mock(TitleService.class);
    segmentTypeRepository = mock(SegmentTypeRepository.class);
    segmentRuleRepository = mock(SegmentRuleRepository.class);
    npcService = mock(NpcService.class);

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");
    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler 2");
    List<Wrestler> allWrestlers = List.of(wrestler1, wrestler2);

    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), any(), any()))
        .thenReturn(allWrestlers);
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), any(Set.class)))
        .thenReturn(allWrestlers);
    when(wrestlerService.findAllFiltered(any(), any(), anyLong())).thenReturn(allWrestlers);
    when(wrestlerService.findByName("Wrestler 1")).thenReturn(Optional.of(wrestler1));
    when(wrestlerService.findByName("Wrestler 2")).thenReturn(Optional.of(wrestler2));
    when(wrestlerRepository.findAll()).thenReturn(allWrestlers);
    when(wrestlerRepository.findByName("Wrestler 1")).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findByName("Wrestler 2")).thenReturn(Optional.of(wrestler2));

    matchType = new SegmentType();
    matchType.setName("Match");
    when(segmentTypeRepository.findAll()).thenReturn(List.of(matchType));
    when(segmentTypeRepository.findByName("Match")).thenReturn(Optional.of(matchType));

    // Mock NpcService for referees
    when(npcService.findAllByType("Referee")).thenReturn(new ArrayList<>());

    // Mock TitleService and available titles
    Title title1 = new Title();
    title1.setId(1L);
    title1.setName("Test Title 1");
    Title title2 = new Title();
    title2.setId(2L);
    title2.setName("Test Title 2");
    when(titleService.findAll()).thenReturn(List.of(title1, title2));

    onSave = mock(Runnable.class);
  }

  @Test
  void testSave() {
    UI.setCurrent(ui);
    EditSegmentDialog dialog =
        new EditSegmentDialog(
            segment,
            wrestlerService,
            titleService,
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            null,
            1L,
            onSave);
    dialog.open();

    // Select segment type
    dialog.getSegmentTypeCombo().setValue(matchType);

    // Simulate user input
    dialog.getNarrationArea().setValue("New Description");
    segment.setNarration("New Description");

    // Act
    dialog.save();

    // Assert
    assertEquals("New Description", segment.getNarration());
    verify(onSave).run();
  }

  @Test
  void testAddTeamButtonPopulatesDropdown() {
    UI.setCurrent(ui);
    EditSegmentDialog dialog =
        new EditSegmentDialog(
            segment,
            wrestlerService,
            titleService,
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            null,
            1L,
            onSave);
    dialog.open();

    int initialTeamCount = dialog.getTeamCombos().size();

    // Simulate clicking Add Team — this was the bug: the new combo had empty items
    dialog.getAddTeamButton().click();

    assertEquals(initialTeamCount + 1, dialog.getTeamCombos().size());
    var newCombo = dialog.getTeamCombos().get(dialog.getTeamCombos().size() - 1);
    assertTrue(
        newCombo.getListDataView().getItemCount() > 0,
        "New team combo must have wrestlers available (regression: empty dropdown bug)");
  }

  @Test
  void testTitleSelection() {
    UI.setCurrent(ui);
    // Force it to be a title segment so combo is initially visible
    segment.setIsTitleSegment(true);

    EditSegmentDialog dialog =
        new EditSegmentDialog(
            segment,
            wrestlerService,
            titleService,
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            null,
            1L,
            onSave);
    dialog.open();

    // Verify title MultiSelectComboBox is visible and populated
    assertTrue(dialog.getTitleMultiSelectComboBox().isVisible());
    assertNotNull(dialog.getTitleMultiSelectComboBox().getListDataView());
  }

  @Test
  void healthFieldsPopulatedForNonPromoWithPlayerWrestler() {
    UI.setCurrent(ui);

    Account playerAccount = new Account();
    playerAccount.setId(100L);
    playerAccount.setUsername("player1");

    Wrestler playerWrestler = new Wrestler();
    playerWrestler.setId(3L);
    playerWrestler.setName("Player Wrestler");
    playerWrestler.setAccount(playerAccount);

    List<Wrestler> allWrestlersWithPlayer =
        List.of(
            playerWrestler,
            new Wrestler() {
              {
                setId(1L);
                setName("Wrestler 1");
              }
            },
            new Wrestler() {
              {
                setId(2L);
                setName("Wrestler 2");
              }
            });
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), any(), any()))
        .thenReturn(allWrestlersWithPlayer);
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), any(Set.class)))
        .thenReturn(allWrestlersWithPlayer);
    when(wrestlerService.findAllFiltered(any(), any(), anyLong()))
        .thenReturn(allWrestlersWithPlayer);
    when(wrestlerService.getAllWrestlers()).thenReturn(allWrestlersWithPlayer);
    when(wrestlerService.findByName("Player Wrestler")).thenReturn(Optional.of(playerWrestler));

    segment.setTeams(List.of(List.of("Player Wrestler"), List.of("Wrestler 2")));
    segment.setType("Match");

    EditSegmentDialog dialog =
        new EditSegmentDialog(
            segment,
            wrestlerService,
            titleService,
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            null,
            1L,
            onSave);
    dialog.open();

    // Explicitly set non-promo type to ensure refreshHealthFields fires with correct state
    dialog.getSegmentTypeCombo().setValue(matchType);

    assertFalse(
        dialog.getHealthFields().isEmpty(),
        "Health fields must be created for player-controlled wrestlers in non-promo segments");
    assertTrue(
        dialog.getHealthFields().containsKey(3L),
        "Health field must exist for the player-controlled wrestler (id=3)");
  }

  @Test
  void healthFieldsNotPopulatedForPromoSegment() {
    UI.setCurrent(ui);

    Account playerAccount = new Account();
    playerAccount.setId(100L);
    playerAccount.setUsername("player1");

    Wrestler playerWrestler = new Wrestler();
    playerWrestler.setId(3L);
    playerWrestler.setName("Player Wrestler");
    playerWrestler.setAccount(playerAccount);

    List<Wrestler> allWrestlersWithPlayer =
        List.of(
            playerWrestler,
            new Wrestler() {
              {
                setId(1L);
                setName("Wrestler 1");
              }
            },
            new Wrestler() {
              {
                setId(2L);
                setName("Wrestler 2");
              }
            });
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), any(), any()))
        .thenReturn(allWrestlersWithPlayer);
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), any(Set.class)))
        .thenReturn(allWrestlersWithPlayer);
    when(wrestlerService.findAllFiltered(any(), any(), anyLong()))
        .thenReturn(allWrestlersWithPlayer);
    when(wrestlerService.getAllWrestlers()).thenReturn(allWrestlersWithPlayer);
    when(wrestlerService.findByName("Player Wrestler")).thenReturn(Optional.of(playerWrestler));

    segment.setTeams(List.of(List.of("Player Wrestler"), List.of("Wrestler 2")));

    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    when(segmentTypeRepository.findAll()).thenReturn(List.of(matchType, promoType));
    lenient().when(segmentTypeRepository.findByName("Promo")).thenReturn(Optional.of(promoType));

    EditSegmentDialog dialog =
        new EditSegmentDialog(
            segment,
            wrestlerService,
            titleService,
            segmentTypeRepository,
            segmentRuleRepository,
            npcService,
            null,
            1L,
            onSave);
    dialog.open();

    // Setting promo type must clear all health fields
    dialog.getSegmentTypeCombo().setValue(promoType);

    assertTrue(
        dialog.getHealthFields().isEmpty(), "Health fields must not be created for promo segments");
  }
}

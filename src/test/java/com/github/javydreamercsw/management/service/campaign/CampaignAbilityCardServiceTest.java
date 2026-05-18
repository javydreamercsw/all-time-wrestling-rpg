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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignAbilityCardServiceTest {

  @Mock private CampaignAbilityCardRepository campaignAbilityCardRepository;

  @InjectMocks private CampaignAbilityCardService campaignAbilityCardService;

  private CampaignAbilityCard card;

  @BeforeEach
  void setUp() {
    card =
        CampaignAbilityCard.builder()
            .name("Test Card")
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .description("A test card")
            .oneTimeUse(true)
            .timing(AbilityTiming.OFFENSE)
            .build();

    when(campaignAbilityCardRepository.save(any(CampaignAbilityCard.class)))
        .thenAnswer(i -> i.getArguments()[0]);
  }

  @Test
  void save_delegatesAndReturns() {
    CampaignAbilityCard result = campaignAbilityCardService.save(card);

    verify(campaignAbilityCardRepository).save(card);
    assertSame(card, result);
  }

  @Test
  void saveAll_delegatesAndReturns() {
    List<CampaignAbilityCard> cards = List.of(card);
    when(campaignAbilityCardRepository.saveAll(cards)).thenReturn(cards);

    List<CampaignAbilityCard> result = campaignAbilityCardService.saveAll(cards);

    verify(campaignAbilityCardRepository).saveAll(cards);
    assertEquals(cards, result);
  }

  @Test
  void delete_delegatesById() {
    campaignAbilityCardService.delete(42L);

    verify(campaignAbilityCardRepository).deleteById(42L);
  }

  @Test
  void list_returnsPaginatedResults() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<CampaignAbilityCard> page = new PageImpl<>(List.of(card));
    when(campaignAbilityCardRepository.findAll(pageable)).thenReturn(page);

    Page<CampaignAbilityCard> result = campaignAbilityCardService.list(pageable);

    verify(campaignAbilityCardRepository).findAll(pageable);
    assertEquals(1, result.getTotalElements());
  }

  @Test
  void findAll_returnsList() {
    List<CampaignAbilityCard> cards = List.of(card);
    when(campaignAbilityCardRepository.findAll()).thenReturn(cards);

    List<CampaignAbilityCard> result = campaignAbilityCardService.findAll();

    verify(campaignAbilityCardRepository).findAll();
    assertEquals(cards, result);
  }

  @Test
  void count_returnsValue() {
    when(campaignAbilityCardRepository.count()).thenReturn(7L);

    long result = campaignAbilityCardService.count();

    assertEquals(7L, result);
  }

  @Test
  void findByNameAndAlignmentAndLevel_found() {
    when(campaignAbilityCardRepository.findByNameAndAlignmentTypeAndLevel(
            "Test Card", AlignmentType.FACE, 1))
        .thenReturn(Optional.of(card));

    Optional<CampaignAbilityCard> result =
        campaignAbilityCardService.findByNameAndAlignmentAndLevel(
            "Test Card", AlignmentType.FACE, 1);

    assertTrue(result.isPresent());
    assertSame(card, result.get());
  }

  @Test
  void findByNameAndAlignmentAndLevel_notFound() {
    when(campaignAbilityCardRepository.findByNameAndAlignmentTypeAndLevel(
            "Missing", AlignmentType.HEEL, 2))
        .thenReturn(Optional.empty());

    Optional<CampaignAbilityCard> result =
        campaignAbilityCardService.findByNameAndAlignmentAndLevel("Missing", AlignmentType.HEEL, 2);

    assertTrue(result.isEmpty());
  }

  @Test
  void createOrUpdateCard_existingCard_updatesFields() {
    when(campaignAbilityCardRepository.findByNameAndAlignmentTypeAndLevel(
            "Test Card", AlignmentType.FACE, 1))
        .thenReturn(Optional.of(card));

    CampaignAbilityCard result =
        campaignAbilityCardService.createOrUpdateCard(
            "Test Card",
            "Updated description",
            AlignmentType.FACE,
            1,
            false,
            AbilityTiming.DEFENSE,
            "effect()",
            "secondary()",
            true,
            AbilityTiming.BACKSTAGE);

    assertEquals("Updated description", result.getDescription());
    assertEquals(AbilityTiming.DEFENSE, result.getTiming());
    assertEquals("effect()", result.getEffectScript());
    assertEquals("secondary()", result.getSecondaryEffectScript());
    verify(campaignAbilityCardRepository).save(card);
  }

  @Test
  void createOrUpdateCard_newCard_createsAndSaves() {
    when(campaignAbilityCardRepository.findByNameAndAlignmentTypeAndLevel(
            "Brand New Card", AlignmentType.HEEL, 3))
        .thenReturn(Optional.empty());

    CampaignAbilityCard result =
        campaignAbilityCardService.createOrUpdateCard(
            "Brand New Card",
            "New description",
            AlignmentType.HEEL,
            3,
            true,
            AbilityTiming.PINNED,
            "newEffect()",
            null,
            false,
            null);

    assertEquals("Brand New Card", result.getName());
    assertEquals(AlignmentType.HEEL, result.getAlignmentType());
    assertEquals(3, result.getLevel());
    assertEquals("New description", result.getDescription());
    verify(campaignAbilityCardRepository).save(any(CampaignAbilityCard.class));
  }
}

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
package com.github.javydreamercsw.management.service.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import jakarta.validation.ValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CardServiceTest extends ManagementIntegrationTest {
  @Autowired CardRepository cardRepository;
  @Autowired CardSetRepository cardSetRepository;

  @BeforeEach
  public void setUp() {
    clearAllRepositories();
    // Create a default CardSet as CardServiceTest expects it.
    CardSet defaultCardSet = new CardSet();
    defaultCardSet.setName("Default");
    defaultCardSet.setSetCode("DEF"); // Use a short, unique code
    defaultCardSet.setCreationDate(Instant.now());
    cardSetRepository.save(defaultCardSet);
  }

  @Test
  public void tasks_are_stored_in_the_database_with_the_current_timestamp() {
    cardService.createCard("Do this");
    Assertions.assertTrue(
        cardService.findAll().stream().anyMatch(card -> card.getName().equals("Do this")));
  }

  @Test
  public void tasks_are_validated_before_they_are_stored() {
    long before = cardRepository.count();
    assertThatThrownBy(() -> cardService.createCard("X".repeat(Card.DESCRIPTION_MAX_LENGTH + 1)))
        .isInstanceOf(ValidationException.class);
    assertThat(cardRepository.count()).isEqualTo(before);
  }
}

package com.github.javydreamercsw.management.service.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CardServiceIT extends AbstractIntegrationTest {
  @Autowired CardRepository cardRepository;
  @Autowired CardSetRepository cardSetRepository;

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

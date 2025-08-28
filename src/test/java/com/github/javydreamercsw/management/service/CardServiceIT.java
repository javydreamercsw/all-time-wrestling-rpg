package com.github.javydreamercsw.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.service.card.CardService;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CardServiceIT {

  @Autowired CardService cardService;

  @Autowired CardRepository cardRepository;

  @Autowired CardSetRepository cardSetRepository;

  @BeforeEach
  void setUp() {
    // Create a test CardSet for the tests
    CardSet testSet = new CardSet();
    testSet.setName("TST");
    cardSetRepository.save(testSet);
  }

  @AfterEach
  void cleanUp() {
    cardRepository.deleteAll();
    cardSetRepository.deleteAll();
  }

  @Test
  public void tasks_are_stored_in_the_database_with_the_current_timestamp() {
    cardService.createCard("Do this");
    assertThat(cardService.list(PageRequest.ofSize(1)))
        .singleElement()
        .matches(card -> card.getName().equals("Do this"));
  }

  @Test
  public void tasks_are_validated_before_they_are_stored() {
    assertThatThrownBy(() -> cardService.createCard("X".repeat(Card.DESCRIPTION_MAX_LENGTH + 1)))
        .isInstanceOf(ValidationException.class);
    assertThat(cardRepository.count()).isEqualTo(0);
  }
}

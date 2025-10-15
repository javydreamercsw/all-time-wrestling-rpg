package com.github.javydreamercsw.management.service.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CardSetServiceTest extends ManagementIntegrationTest {

  @Test
  void testCreateCardSetSet() {
    int initialSize = cardSetRepository.findAll().size();
    cardSetService.createCardSet("TST", "TST");
    assertThat(cardSetRepository.findAll()).hasSize(initialSize + 1);
    Assertions.assertTrue(
        cardSetRepository.findAll().stream().anyMatch(set -> set.getName().equals("TST")));
  }
}

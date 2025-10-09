package com.github.javydreamercsw.management.service.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.transaction.annotation.Transactional;

@EnabledIf("isNotionTokenAvailable")
@Transactional
class CardSetServiceIT extends AbstractIntegrationTest {

  @Test
  void testCreateCardSetSet() {
    int initialSize = cardSetRepository.findAll().size();
    cardSetService.createCardSet("TST", "TST");
    assertThat(cardSetRepository.findAll()).hasSize(initialSize + 1);
    Assertions.assertTrue(
        cardSetRepository.findAll().stream().anyMatch(set -> set.getName().equals("TST")));
  }
}

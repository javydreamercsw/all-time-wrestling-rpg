package com.github.javydreamercsw.management.service.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.management.config.TestConfig;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Import({TestcontainersConfiguration.class, TestConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class CardSetServiceIT {

  @Autowired private CardSetService cardSetService;
  @Autowired private CardSetRepository cardSetRepository;

  @Test
  void testCreateCardSetSet() {
    int initialSize = cardSetRepository.findAll().size();
    cardSetService.createCardSet("TST");
    assertThat(cardSetRepository.findAll()).hasSize(initialSize + 1);
    Assertions.assertTrue(
        cardSetRepository.findAll().stream().anyMatch(set -> set.getName().equals("TST")));
  }
}

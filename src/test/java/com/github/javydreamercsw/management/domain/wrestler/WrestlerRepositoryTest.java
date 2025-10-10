package com.github.javydreamercsw.management.domain.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@DisplayName("WrestlerRepository Integration Tests")
class WrestlerRepositoryTest {

  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testFindAllByPagination() {
    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Wrestler One");
    wrestler1.setDeckSize(15);
    wrestler1.setLowHealth(0);
    wrestler1.setLowStamina(0);
    wrestler1.setStartingHealth(15);
    wrestler1.setStartingStamina(0);
    wrestler1.setCreationDate(java.time.Instant.now());
    wrestler1.setFans(100L);
    wrestler1.setIsPlayer(true);
    wrestler1.setBumps(0);
    wrestler1.setCurrentHealth(15);
    wrestler1.setGender(Gender.MALE);
    wrestler1.setTier(WrestlerTier.ROOKIE);
    wrestlerRepository.save(wrestler1);
    Wrestler wrestler2 = new Wrestler();
    wrestler2.setName("Wrestler Two");
    wrestler2.setDeckSize(15);
    wrestler2.setLowHealth(0);
    wrestler2.setLowStamina(0);
    wrestler2.setStartingHealth(15);
    wrestler2.setStartingStamina(0);
    wrestler2.setCreationDate(java.time.Instant.now());
    wrestler2.setFans(100L);
    wrestler2.setIsPlayer(true);
    wrestler2.setBumps(0);
    wrestler2.setCurrentHealth(15);
    wrestler2.setGender(Gender.MALE);
    wrestler2.setTier(WrestlerTier.ROOKIE);
    wrestlerRepository.save(wrestler2);

    var page = wrestlerRepository.findAllBy(PageRequest.of(0, 1));
    assertThat(page.getContent()).hasSize(1);
    assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void testFindByName() {
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Decked Wrestler");
    wrestler.setDeckSize(15);
    wrestler.setLowHealth(0);
    wrestler.setLowStamina(0);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(0);
    wrestler.setCreationDate(java.time.Instant.now());
    wrestler.setFans(100L);
    wrestler.setIsPlayer(true);
    wrestler.setBumps(0);
    wrestler.setCurrentHealth(15);
    wrestler.setGender(Gender.MALE);
    wrestler.setTier(WrestlerTier.ROOKIE);
    wrestlerRepository.save(wrestler);

    Optional<Wrestler> found = wrestlerRepository.findByName("Decked Wrestler");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Decked Wrestler");
  }

  @Test
  void testFindByExternalId() {
    Wrestler wrestler = new Wrestler();
    wrestler.setName("External Wrestler");
    wrestler.setExternalId("ext-123");
    wrestler.setDeckSize(15);
    wrestler.setLowHealth(0);
    wrestler.setLowStamina(0);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(0);
    wrestler.setCreationDate(java.time.Instant.now());
    wrestler.setFans(100L);
    wrestler.setIsPlayer(true);
    wrestler.setBumps(0);
    wrestler.setCurrentHealth(15);
    wrestler.setGender(Gender.MALE);
    wrestler.setTier(WrestlerTier.ROOKIE);
    wrestlerRepository.save(wrestler);

    Optional<Wrestler> found = wrestlerRepository.findByExternalId("ext-123");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("External Wrestler");
  }
}

package com.github.javydreamercsw;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import lombok.NonNull;

public class TestUtils {

  public static Wrestler createWrestler(
      @NonNull WrestlerRepository wrestlerRepository, @NonNull String name) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName(name);
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setLowHealth(4);
    wrestler.setLowStamina(2);
    wrestler.setTier(WrestlerTier.ROOKIE);
    return wrestlerRepository.save(wrestler);
  }
}

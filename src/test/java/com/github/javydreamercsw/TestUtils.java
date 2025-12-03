package com.github.javydreamercsw;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import lombok.NonNull;

public class TestUtils {
  /**
   * Create a wrestler with default values. Stil needs to be persisted in the database.
   *
   * @param name Desired wrestler's name.
   * @return Created wrestler.
   */
  public static Wrestler createWrestler(@NonNull String name) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setName(name);
    wrestler.setDescription("Test Wrestler");
    wrestler.setDeckSize(15);
    wrestler.setStartingHealth(15);
    wrestler.setStartingStamina(15);
    wrestler.setLowHealth(4);
    wrestler.setLowStamina(2);
    wrestler.setTier(WrestlerTier.ROOKIE);
    wrestler.setCreationDate(Instant.now());
    return wrestler;
  }
}

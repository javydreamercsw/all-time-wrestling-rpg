package com.github.javydreamercsw.management.service.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

@EnabledIf("isNotionTokenAvailable")
public class WrestlerDTOIntegrationTest extends ManagementIntegrationTest {

  @Test
  void robVanDamMoveSetShouldBePopulatedCorrectly() {
    // Given
    String wrestlerName = "Rob Van Dam";

    // When
    Wrestler wrestler =
        wrestlerService
            .findByName(wrestlerName)
            .orElseThrow(() -> new AssertionError("Wrestler " + wrestlerName + " not found"));

    WrestlerDTO wrestlerDTO = new WrestlerDTO(wrestler);

    // Then
    assertThat(wrestlerDTO.getMoveSet()).isNotNull();
    assertThat(wrestlerDTO.getMoveSet().getFinishers())
        .extracting("name")
        .containsExactlyInAnyOrder("Five-Star Frogsplash", "Van Terminator");
    assertThat(wrestlerDTO.getMoveSet().getTrademarks())
        .extracting("name")
        .containsExactlyInAnyOrder(
            "Rolling Thunder",
            "Split-Legged Moonsault",
            "Springboard Thrust Kick",
            "Van Daminator",
            "Corkscrew Legdrop");

    assertThat(wrestlerDTO.getMoveSet().getCommonMoves())
        .extracting("name")
        .containsExactlyInAnyOrder(
            "Monkey FLip",
            "Forearm",
            "Thrust Kick",
            "Handspring Moonsault",
            "Somersault",
            "Flying Cross-Body",
            "Slam",
            "Clothesline",
            "Northern Lights Suplex",
            "Tornado DDT");
  }
}

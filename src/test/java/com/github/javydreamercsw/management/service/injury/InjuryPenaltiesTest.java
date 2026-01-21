/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.injury;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@WithMockUser(roles = "BOOKER")
class InjuryPenaltiesTest extends AbstractIntegrationTest {

  @Autowired private InjuryService injuryService;

  @Test
  @Transactional
  void testInjuryCreationIncludesPenalties() {

    Wrestler wrestler = createTestWrestler("Penalty Tester");
    wrestler = wrestlerRepository.save(wrestler);

    Optional<Injury> injury =
        injuryService.createInjury(
            wrestler.getId(), "Test Injury", "Description", InjurySeverity.CRITICAL, "Notes");

    assertThat(injury).isPresent();
    assertThat(injury.get().getHealthPenalty()).isPositive();
    assertThat(injury.get().getStaminaPenalty()).isGreaterThanOrEqualTo(0);
    assertThat(injury.get().getHandSizePenalty()).isGreaterThanOrEqualTo(0);

    // Critical injuries should have some penalties based on our enum definition
    assertThat(injury.get().getStaminaPenalty()).isBetween(2, 3);
    assertThat(injury.get().getHandSizePenalty()).isEqualTo(1);
  }
}

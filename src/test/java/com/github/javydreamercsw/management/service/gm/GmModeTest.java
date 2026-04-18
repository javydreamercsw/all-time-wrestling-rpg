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
package com.github.javydreamercsw.management.service.gm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerContractRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GmModeTest {

  @Mock private WrestlerContractRepository contractRepository;
  @Mock private SalaryCalculator salaryCalculator;
  @Mock private WrestlerService wrestlerService;

  @Test
  void testSalaryCalculation() {
    SalaryCalculator calc = new SalaryCalculator();
    Wrestler w = new Wrestler();
    w.setId(1L);

    WrestlerState state =
        WrestlerState.builder().wrestler(w).fans(10000L).tier(WrestlerTier.MIDCARDER).build();

    // Base 500 + (10000/1000 * 10) = 500 + 100 = 600
    // Midcarder multiplier = 1.2
    // Total = 720.00
    BigDecimal salary = calc.calculateWeeklySalary(w, state);
    assertEquals(new BigDecimal("720.00"), salary);
  }

  @Test
  void testWrestlerAddBumpWithLowStamina() {
    Wrestler w = new Wrestler();
    WrestlerState state =
        WrestlerState.builder()
            .wrestler(w)
            .managementStamina(30) // Low stamina
            .bumps(1)
            .build();

    // With low stamina, threshold is 2. 1 bump + 1 more = injury.
    boolean injury = state.addBump();
    assertTrue(injury);
    assertEquals(0, state.getBumps()); // Should reset after injury
  }
}

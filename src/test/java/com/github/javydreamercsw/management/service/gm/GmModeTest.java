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
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GmModeTest {

  @Mock private WrestlerContractRepository contractRepository;
  @Mock private SalaryCalculator salaryCalculator;

  @Test
  void testSalaryCalculation() {
    SalaryCalculator calc = new SalaryCalculator();
    Wrestler w = new Wrestler();
    w.setFans(10000L); // 10,000 fans
    w.setTier(WrestlerTier.MIDCARDER);

    // Base 500 + (10000/1000 * 10) = 500 + 100 = 600
    // Midcarder multiplier = 1.2
    // Total = 720.00
    BigDecimal salary = calc.calculateWeeklySalary(w);
    assertEquals(new BigDecimal("720.00"), salary);
  }

  @Test
  void testWrestlerAddBumpWithLowStamina() {
    Wrestler w = new Wrestler();
    w.setManagementStamina(30); // Low stamina
    w.setBumps(1);

    // With low stamina, threshold is 2. 1 bump + 1 more = injury.
    boolean injury = w.addBump();
    assertTrue(injury);
    assertEquals(0, w.getBumps()); // Should reset after injury
  }
}

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

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Calculates wrestler salaries based on their fan count and tier. */
@Component
public class SalaryCalculator {

  private static final BigDecimal BASE_SALARY = new BigDecimal("500");
  private static final BigDecimal MULTIPLIER_PER_1000_FANS = new BigDecimal("10");

  /**
   * Calculates the weekly salary for a wrestler. Formula: Base Salary + (Fans / 1000) * Multiplier
   */
  public BigDecimal calculateWeeklySalary(Wrestler wrestler) {
    if (wrestler == null) {
      return BigDecimal.ZERO;
    }

    BigDecimal fanBonus =
        BigDecimal.valueOf(wrestler.getFans())
            .divide(new BigDecimal("1000"), 0, RoundingMode.FLOOR)
            .multiply(MULTIPLIER_PER_1000_FANS);

    BigDecimal tierMultiplier =
        switch (wrestler.getTier()) {
          case ROOKIE -> new BigDecimal("0.5");
          case RISER -> new BigDecimal("0.8");
          case CONTENDER -> new BigDecimal("1.0");
          case MIDCARDER -> new BigDecimal("1.2");
          case MAIN_EVENTER -> new BigDecimal("2.5");
          case ICON -> new BigDecimal("5.0");
        };

    return BASE_SALARY.add(fanBonus).multiply(tierMultiplier).setScale(2, RoundingMode.HALF_UP);
  }
}

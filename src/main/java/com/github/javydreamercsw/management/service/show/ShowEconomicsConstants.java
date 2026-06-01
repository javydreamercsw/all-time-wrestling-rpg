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
package com.github.javydreamercsw.management.service.show;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Numeric constants for show attendance and gate-revenue calculations. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShowEconomicsConstants {

  /** Divisor applied to the total fan-weight sum to produce the base attendance estimate. */
  public static final int FAN_WEIGHT_ATTENDANCE_DIVISOR = 2;

  /** Attendance multiplier for premium live events. */
  public static final double PREMIUM_EVENT_MULTIPLIER = 1.5;

  /** Baseline multiplier when no arena trait bonus applies. */
  public static final double BASE_TRAIT_MULTIPLIER = 1.0;

  /** Attendance bonus per matching arena environmental trait. */
  public static final double TRAIT_BONUS_PER_MATCH = 0.05;

  /** Maximum total bonus from arena environmental traits. */
  public static final double MAX_TRAIT_BONUS = 0.20;

  /** Ticket price for premium live events. */
  public static final BigDecimal PREMIUM_TICKET_PRICE = new BigDecimal("75.00");

  /** Ticket price for regular shows. */
  public static final BigDecimal STANDARD_TICKET_PRICE = new BigDecimal("25.00");
}

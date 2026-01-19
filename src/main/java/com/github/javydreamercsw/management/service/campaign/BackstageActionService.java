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
package com.github.javydreamercsw.management.service.campaign;

import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class BackstageActionService {

  private final Random random;

  /**
   * Roll dice and return the number of successes (4+).
   *
   * @param numberOfDice Number of dice to roll (based on attribute).
   * @return Number of successes.
   */
  public int rollDice(int numberOfDice) {
    int successes = 0;
    for (int i = 0; i < numberOfDice; i++) {
      int roll = random.nextInt(6) + 1;
      if (roll >= 4) {
        successes++;
      }
    }
    return successes;
  }
}

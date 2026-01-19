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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackstageActionServiceTest {

  @Mock private Random random;
  @InjectMocks private BackstageActionService backstageActionService;

  @Test
  void testRollDice() {
    // 4, 5, 6 are successes. 1, 2, 3 are failures.
    // We expect nextInt(6) to return 0-5. So we add 1.
    // Success: 3 (4), 4 (5), 5 (6)
    // Failure: 0 (1), 1 (2), 2 (3)

    // Mocking 3 rolls: 4 (Success), 2 (Fail), 6 (Success)
    when(random.nextInt(6)).thenReturn(3, 1, 5);

    int successes = backstageActionService.rollDice(3);

    assertThat(successes).isEqualTo(2);
  }
}

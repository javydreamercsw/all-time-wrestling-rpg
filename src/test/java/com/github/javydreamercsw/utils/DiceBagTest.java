/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.utils;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class DiceBagTest {
  @RepeatedTest(10)
  public void testRoll() {
    Random r = new Random();
    DiceBag diceBag =
        new DiceBag(r, new int[] {r.nextInt(100) + 1, r.nextInt(100) + 1, r.nextInt(100) + 1});
    int result = diceBag.roll();
    int[] individualRolls = diceBag.getLastRoll();

    System.out.println(
        "Dice sides: "
            + Arrays.toString(diceBag.getDice())
            + ", Individual rolls: "
            + Arrays.toString(individualRolls)
            + ", Total: "
            + result);

    // Verify the result matches the sum of individual rolls
    int expectedSum = Arrays.stream(individualRolls).sum();
    Assertions.assertEquals(expectedSum, result, "Roll result should equal sum of individual dice");

    // Original assertion: result should be within possible range
    Assertions.assertTrue(
        result >= diceBag.getDice().length && result <= Arrays.stream(diceBag.getDice()).sum(),
        "Roll result should be within possible range");
  }
}

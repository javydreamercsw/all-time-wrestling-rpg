package com.github.javydreamercsw.utils;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class DiceBagTest {
  @RepeatedTest(10)
  public void testRoll() {
    Random r = new Random();
    DiceBag diceBag = new DiceBag(r.nextInt(100) + 1, r.nextInt(100) + 1, r.nextInt(100) + 1);
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

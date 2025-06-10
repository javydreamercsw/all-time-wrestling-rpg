package com.github.javydreamercsw.utils;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class DiceBagTest {
  @RepeatedTest(10)
  public void testRoll() {
    Random r = new Random();
    DiceBag diceBag = new DiceBag(r.nextInt(100), r.nextInt(100), r.nextInt(100));
    int result = diceBag.roll();
    System.out.println("Rolled: " + result + " for dice: " + Arrays.toString(diceBag.getDice()));
    Assertions.assertTrue(
        result >= diceBag.getDice().length && result <= Arrays.stream(diceBag.getDice()).sum());
  }
}

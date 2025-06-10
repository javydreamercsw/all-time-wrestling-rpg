package com.github.javydreamercsw.utils;

import java.util.Random;

public class DiceBag {
  private int[] dice;

  public DiceBag(int... dice) {
    this.dice = dice;
    validateDice(dice);
  }

  public int[] getDice() {
    return dice;
  }

  public int roll() {
    int total = 0;
    Random rand = new Random();
    for (int die : dice) {
      total += rand.nextInt(die) + 1;
    }
    return total;
  }

  private void validateDice(int[] dice) {
    if (dice == null || dice.length == 0) {
      throw new IllegalArgumentException("Dice array cannot be null or empty");
    }
    for (int die : dice) {
      if (die <= 0) {
        throw new IllegalArgumentException("All dice must be positive integers");
      }
    }
  }
}

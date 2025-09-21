package com.github.javydreamercsw.utils;

import java.util.Random;

public class DiceBag {
  private int[] dice;
  private Random random;
  private int[] lastRoll; // Store the results of the last roll

  public DiceBag(int... dice) {
    this.dice = dice;
    this.random = new Random();
    validateDice(dice);
  }

  public DiceBag(Random random, int[] dice) {
    this.dice = dice;
    this.random = random;
    validateDice(dice);
  }

  public int[] getDice() {
    return dice;
  }

  /** Get the individual results from the last roll */
  public int[] getLastRoll() {
    return lastRoll != null ? lastRoll.clone() : null;
  }

  public int roll() {
    int total = 0;
    lastRoll = new int[dice.length];

    for (int i = 0; i < dice.length; i++) {
      int rollResult = random.nextInt(dice[i]) + 1;
      lastRoll[i] = rollResult;
      total += rollResult;
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

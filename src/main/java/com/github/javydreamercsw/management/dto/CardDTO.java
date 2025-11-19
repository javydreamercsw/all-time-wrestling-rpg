package com.github.javydreamercsw.management.dto;

import lombok.Data;

@Data
public class CardDTO {
  private String name;
  private String type;
  private int damage;
  private boolean finisher = false;
  private boolean signature = false;
  private boolean pin = false;
  private boolean taunt = false;
  private boolean recover = false;
  private int stamina;
  private int momentum;
  private int target;
  private int number;
  private String set;
}

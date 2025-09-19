package com.github.javydreamercsw.management.domain.wrestler;

import lombok.Data;
import lombok.NonNull;

@Data
public class WrestlerDTO {
  private String name;
  private String description;
  private String gender;
  private String tier;

  public WrestlerDTO(@NonNull Wrestler wrestler) {
    this.name = wrestler.getName();
    this.description = wrestler.getDescription();
    this.gender = wrestler.getGender().name();
    this.tier = wrestler.getTier().name();
  }
}

package com.github.javydreamercsw.management.dto;

import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import lombok.Data;

@Data
public class TitleDTO {
  private String name;
  private String description;
  private WrestlerTier tier;
  private Gender gender;
  private String currentChampionName;
}

package com.github.javydreamercsw.management.dto.ranking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChampionDTO {
  private Long id;
  private String name;
  private Long fans;
  private long reignDays;
}

package com.github.javydreamercsw.management.dto;

import lombok.Data;

@Data
public class SegmentRuleDTO {
  private String name;
  private String description;
  private boolean requiresHighHeat;
}

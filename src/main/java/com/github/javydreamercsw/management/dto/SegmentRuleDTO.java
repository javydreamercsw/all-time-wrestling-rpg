package com.github.javydreamercsw.management.dto;

import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import lombok.Data;

@Data
public class SegmentRuleDTO {
  private String name;
  private String description;
  private boolean requiresHighHeat;
  private BumpAddition bumpAddition;
}

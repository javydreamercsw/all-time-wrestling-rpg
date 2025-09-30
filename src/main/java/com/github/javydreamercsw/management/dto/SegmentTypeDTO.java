package com.github.javydreamercsw.management.dto;

import lombok.Data;

@Data
public class SegmentTypeDTO {
  private String name;
  private String description;
  private int playerAmount;
  private boolean unlimited;
}

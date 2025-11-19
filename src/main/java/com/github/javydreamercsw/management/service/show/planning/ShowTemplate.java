package com.github.javydreamercsw.management.service.show.planning;

import lombok.Data;

@Data
public class ShowTemplate {
  private String showName;
  private String description;
  private int expectedMatches;
  private int expectedPromos;
}

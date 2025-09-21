package com.github.javydreamercsw.management.service.show.planning;

import java.util.List;
import lombok.Data;

@Data
public class ProposedSegment {
  private String type; // "segment" or "promo"
  private String description;
  private List<String> participants;
}

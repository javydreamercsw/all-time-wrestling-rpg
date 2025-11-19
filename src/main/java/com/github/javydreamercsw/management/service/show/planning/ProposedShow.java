package com.github.javydreamercsw.management.service.show.planning;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ProposedShow {
  private List<ProposedSegment> segments = new ArrayList<>();
}

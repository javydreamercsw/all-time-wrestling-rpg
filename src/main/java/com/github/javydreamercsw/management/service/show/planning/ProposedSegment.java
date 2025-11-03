package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.title.Title;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class ProposedSegment {
  private String type; // "segment" or "promo"
  private String description;
  private String summary;
  private List<String> participants;
  private List<String> winners;
  private Boolean isTitleSegment = false;
  private Set<Title> titles = new java.util.HashSet<>();

  public void setTitles(Set<Title> titles) {
    this.titles = titles;
    this.isTitleSegment = !titles.isEmpty();
  }

  public Set<Title> getTitles() {
    return titles;
  }
}

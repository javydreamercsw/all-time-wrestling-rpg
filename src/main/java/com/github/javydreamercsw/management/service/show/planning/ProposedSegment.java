/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.show.planning;

import com.github.javydreamercsw.management.domain.title.Title;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class ProposedSegment {
  private String type; // "segment" or "promo"
  private String narration;
  private String summary;
  private List<String> participants;
  private List<String> winners;
  private Boolean isTitleSegment = false;
  private Set<Title> titles = new java.util.HashSet<>();
  private List<String> rules;
  private String refereeName;

  public void setTitles(Set<Title> titles) {
    this.titles = titles;
    this.isTitleSegment = !titles.isEmpty();
  }
}

package com.github.javydreamercsw.management.event.league;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DraftUpdateEvent {
  private Long draftId;
}

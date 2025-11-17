package com.github.javydreamercsw.management.domain.inbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum InboxEventType {
  RIVALRY_HEAT_CHANGE("Rivalry Heat Change"),
  FAN_ADJUDICATION("Fan Adjudication"),
  WRESTLER_INJURY_HEALED("Wrestler Injury Healed"),
  WRESTLER_INJURY_OBTAINED("Wrestler Injury Obtained");

  private final String friendlyName;

  @Override
  public String toString() {
    return friendlyName;
  }
}

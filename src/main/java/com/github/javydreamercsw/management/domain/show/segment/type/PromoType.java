package com.github.javydreamercsw.management.domain.show.segment.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromoType {
  CONFRONTATION_PROMO("Confrontation Promo"),
  CONTRACT_SIGNING("Contract Signing"),
  CHALLENGE_ISSUED("Challenge Issued"),
  INTERVIEW_SEGMENT("Interview Segment"),
  BACKSTAGE_SEGMENT("Backstage Segment"),
  SOLO_PROMO("Solo Promo"),
  GROUP_PROMO("Group Promo"),
  CHAMPIONSHIP_PRESENTATION("Championship Presentation"),
  RETIREMENT_SPEECH("Retirement Speech"),
  ALLIANCE_ANNOUNCEMENT("Alliance Announcement");

  private final String displayName;
}

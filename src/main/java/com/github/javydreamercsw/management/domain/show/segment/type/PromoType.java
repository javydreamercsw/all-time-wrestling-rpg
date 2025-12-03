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

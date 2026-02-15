/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.base.domain.account;

public enum AchievementType {
  // Collection Milestones
  FIRST_WRESTLER("Rookie Manager", "Create your first wrestler", 50),
  ROSTER_BUILDER("Talent Scout", "Build a roster of 10 wrestlers", 200),
  FULL_HOUSE("Promoter", "Build a roster of 50 wrestlers", 1000),

  // Fan Milestones
  CROWD_PLEASER("Crowd Pleaser", "Accumulate 10,000 total fans", 100),
  MAIN_EVENT_DRAW("Main Event Draw", "Accumulate 100,000 total fans", 500),
  GLOBAL_ICON("Global Icon", "Accumulate 1,000,000 total fans", 5000),

  // Championship Milestones
  FIRST_CHAMPION("Gold Rush", "Have a wrestler win a championship", 250),
  GRAND_SLAM("Grand Slam", "Have wrestlers hold every active title type", 2000),

  // Booking Milestones
  BOOKER_OF_THE_YEAR("Booker of the Year", "Book 50 shows", 500),
  FIVE_STAR_CLASSIC("5-Star Classic", "Book a match with a perfect 5-star rating", 1000);

  private final String displayName;
  private final String description;
  private final int xpValue;

  AchievementType(String displayName, String description, int xpValue) {
    this.displayName = displayName;
    this.description = description;
    this.xpValue = xpValue;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public int getXpValue() {
    return xpValue;
  }
}

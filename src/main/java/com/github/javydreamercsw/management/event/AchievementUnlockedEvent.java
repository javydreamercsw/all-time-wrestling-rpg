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
package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.Achievement;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AchievementUnlockedEvent extends ApplicationEvent {

  private final Long accountId;
  private final String achievementName;
  private final int xpValue;

  public AchievementUnlockedEvent(Object source, Account account, Achievement achievement) {
    super(source);
    this.accountId = account.getId();
    this.achievementName = achievement.getName();
    this.xpValue = achievement.getXpValue();
  }
}

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

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a wrestler defeats a reigning champion in a NON-title match — the champion-slayer
 * rule made the winner available as a challenger for the champion's title.
 */
@Getter
public class ChampionDefeatedEvent extends ApplicationEvent {

  private final Title title;
  private final Wrestler champion;
  private final Wrestler winner;

  public ChampionDefeatedEvent(
      final Object source, final Title title, final Wrestler champion, final Wrestler winner) {
    super(source);
    this.title = title;
    this.champion = champion;
    this.winner = winner;
  }
}

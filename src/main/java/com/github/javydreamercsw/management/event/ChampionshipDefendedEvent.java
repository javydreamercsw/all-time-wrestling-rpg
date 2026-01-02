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
package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChampionshipDefendedEvent extends ApplicationEvent {

  private final Long titleId;
  private final String titleName;
  private final List<Wrestler> champions;

  public ChampionshipDefendedEvent(Object source, Title title, List<Wrestler> champions) {
    super(source);
    this.titleId = title.getId();
    this.titleName = title.getName();
    this.champions = champions;
  }
}

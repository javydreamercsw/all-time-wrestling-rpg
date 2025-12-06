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

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class HeatChangeEvent extends ApplicationEvent {

  private final Long rivalryId;
  private final int oldHeat;
  private final int newHeat;
  private final String reason;
  private final List<Wrestler> wrestlers;

  public HeatChangeEvent(
      Object source, Rivalry rivalry, int oldHeat, String reason, List<Wrestler> wrestlers) {
    super(source);
    this.rivalryId = rivalry.getId();
    this.oldHeat = oldHeat;
    this.newHeat = rivalry.getHeat();
    this.reason = reason;
    this.wrestlers = wrestlers;
  }
}

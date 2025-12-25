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
package com.github.javydreamercsw.management.event.dto;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;

@Getter
public class WrestlerBumpEvent extends ApplicationEvent {

  private final Wrestler wrestler;

  public WrestlerBumpEvent(@NonNull Object source, @NonNull Wrestler wrestler) {
    super(source);
    this.wrestler = wrestler;
  }
}

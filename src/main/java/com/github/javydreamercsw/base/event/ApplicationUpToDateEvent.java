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
package com.github.javydreamercsw.base.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published by {@code UpdateCheckService} when a successful update check finds no release newer
 * than the running build. Listeners use it to clear stale "update available" notifications left
 * over from before an upgrade.
 */
@Getter
public class ApplicationUpToDateEvent extends ApplicationEvent {

  public ApplicationUpToDateEvent(final Object source) {
    super(source);
  }
}

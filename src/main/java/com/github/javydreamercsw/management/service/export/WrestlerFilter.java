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
package com.github.javydreamercsw.management.service.export;

import java.util.Set;

public record WrestlerFilter(Scope scope, Set<Long> wrestlerIds) {

  public enum Scope {
    ALL,
    ACTIVE_ONLY,
    MANUAL
  }

  public static WrestlerFilter all() {
    return new WrestlerFilter(Scope.ALL, null);
  }

  public static WrestlerFilter activeOnly() {
    return new WrestlerFilter(Scope.ACTIVE_ONLY, null);
  }

  public static WrestlerFilter manual(Set<Long> ids) {
    return new WrestlerFilter(Scope.MANUAL, ids);
  }
}

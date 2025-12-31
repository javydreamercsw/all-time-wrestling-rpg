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
package com.github.javydreamercsw.management.domain.inbox;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

public final class InboxEventType {
  private final @NonNull String name;
  private final @NonNull String friendlyName;

  public InboxEventType(@NonNull String name, @NonNull String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  public @NonNull String getName() {
    return name;
  }

  public @NonNull String getFriendlyName() {
    return friendlyName;
  }

  @Override
  public @NonNull String toString() {
    return friendlyName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InboxEventType that = (InboxEventType) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}

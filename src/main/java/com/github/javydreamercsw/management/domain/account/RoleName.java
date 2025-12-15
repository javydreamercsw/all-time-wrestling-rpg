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
package com.github.javydreamercsw.management.domain.account;

/** Enumeration of available system roles. */
public enum RoleName {
  /** Full system access - can manage accounts and all content */
  ADMIN,

  /** Can manage shows, wrestlers, and content but not system administration */
  BOOKER,

  /** Can manage own content and view most data */
  PLAYER,

  /** Read-only access to content */
  VIEWER;

  // String constants for use in @RolesAllowed annotations
  public static final String ADMIN_ROLE = "ADMIN";
  public static final String BOOKER_ROLE = "BOOKER";
  public static final String PLAYER_ROLE = "PLAYER";
  public static final String VIEWER_ROLE = "VIEWER";
}

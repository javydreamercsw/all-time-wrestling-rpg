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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.base.domain.account.RoleName;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuItem {
  private String title;
  private VaadinIcon icon;
  private String path;
  private boolean external;
  private List<MenuItem> children = new ArrayList<>();
  private List<RoleName> requiredRoles = new ArrayList<>();

  public MenuItem(String title, VaadinIcon icon, String path) {
    this(title, icon, path, false);
  }

  public MenuItem(String title, VaadinIcon icon, String path, boolean external) {
    this.title = title;
    this.icon = icon;
    this.path = path;
    this.external = external;
  }

  public MenuItem(String title, VaadinIcon icon, String path, RoleName... roles) {
    this.title = title;
    this.icon = icon;
    this.path = path;
    if (roles != null && roles.length > 0) {
      this.requiredRoles = Arrays.asList(roles);
    }
  }

  public void addChild(MenuItem child) {
    children.add(child);
  }

  public boolean hasRequiredRoles() {
    return !requiredRoles.isEmpty();
  }
}

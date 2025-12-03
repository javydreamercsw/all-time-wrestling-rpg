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
package com.github.javydreamercsw.base.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.theme.lumo.LumoUtility.*;

public final class ViewToolbar extends Composite<Header> {

  public ViewToolbar(String viewTitle, Component... components) {
    addClassNames(
        Display.FLEX,
        FlexDirection.COLUMN,
        JustifyContent.BETWEEN,
        AlignItems.STRETCH,
        Gap.MEDIUM,
        FlexDirection.Breakpoint.Medium.ROW,
        AlignItems.Breakpoint.Medium.CENTER);

    var drawerToggle = new DrawerToggle();
    drawerToggle.addClassNames(Margin.NONE);

    var title = new H1(viewTitle);
    title.addClassNames(FontSize.XLARGE, Margin.NONE, FontWeight.LIGHT);

    var toggleAndTitle = new Div(drawerToggle, title);
    toggleAndTitle.addClassNames(Display.FLEX, AlignItems.CENTER);
    getContent().add(toggleAndTitle);

    if (components.length > 0) {
      var actions = new Div(components);
      actions.addClassNames(
          Display.FLEX,
          FlexDirection.COLUMN,
          JustifyContent.BETWEEN,
          Flex.GROW,
          Gap.SMALL,
          FlexDirection.Breakpoint.Medium.ROW);
      getContent().add(actions);
    }
  }

  public static Component group(Component... components) {
    var group = new Div(components);
    group.addClassNames(
        Display.FLEX,
        FlexDirection.COLUMN,
        AlignItems.STRETCH,
        Gap.SMALL,
        FlexDirection.Breakpoint.Medium.ROW,
        AlignItems.Breakpoint.Medium.CENTER);
    return group;
  }
}

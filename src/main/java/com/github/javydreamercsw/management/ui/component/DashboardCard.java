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
package com.github.javydreamercsw.management.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import lombok.NonNull;

/** A reusable card component for dashboard-style layouts. */
public class DashboardCard extends Composite<Div> {

  private final Div content;

  public DashboardCard(@NonNull String title) {
    getContent()
        .addClassNames(
            Display.FLEX,
            FlexDirection.COLUMN,
            Padding.MEDIUM,
            Background.BASE,
            Border.ALL,
            BorderRadius.MEDIUM,
            BoxShadow.SMALL,
            Width.FULL);

    H3 header = new H3(title);
    header.addClassNames(Margin.Top.NONE, Margin.Bottom.MEDIUM, FontSize.MEDIUM, TextColor.PRIMARY);
    getContent().add(header);

    content = new Div();
    content.addClassNames(Display.FLEX, FlexDirection.COLUMN, Width.FULL);
    getContent().add(content);
  }

  public void add(Component... components) {
    content.add(components);
  }

  public void setMaxWidth(String maxWidth) {
    getContent().setMaxWidth(maxWidth);
  }
}

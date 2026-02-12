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

import com.github.javydreamercsw.management.dto.segment.NarrationLineDTO;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import lombok.NonNull;

/** Component for displaying structured match commentary. */
public class CommentaryComponent extends VerticalLayout {

  public CommentaryComponent() {
    setSpacing(true);
    setPadding(false);
    setWidthFull();
    addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Padding.MEDIUM);
  }

  public void setCommentary(@NonNull List<NarrationLineDTO> lines) {
    removeAll();
    if (lines.isEmpty()) {
      Span emptyMessage = new Span("No commentary available.");
      emptyMessage.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
      add(emptyMessage);
      return;
    }

    for (NarrationLineDTO line : lines) {
      add(createLineComponent(line));
    }
  }

  private Div createLineComponent(NarrationLineDTO line) {
    Div container = new Div();
    container.setWidthFull();
    container.addClassNames(
        LumoUtility.Margin.Bottom.SMALL, LumoUtility.Padding.SMALL, LumoUtility.BorderRadius.SMALL);

    Span nameSpan = new Span(line.getCommentatorName() + ":");
    nameSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.Margin.Right.SMALL);

    Span contentSpan = new Span(line.getContent());

    if ("Narrator".equalsIgnoreCase(line.getCommentatorName())) {
      container.addClassNames(LumoUtility.Background.CONTRAST_5);
      nameSpan.addClassNames(LumoUtility.TextColor.PRIMARY);
      contentSpan.getStyle().set("font-style", "italic");
    } else {
      // Style based on some characteristic (could be alignment if passed here)
      container.addClassNames(LumoUtility.Background.BASE);
      nameSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
    }

    container.add(nameSpan, contentSpan);
    return container;
  }
}

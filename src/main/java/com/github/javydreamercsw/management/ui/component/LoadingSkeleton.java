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
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;

/**
 * Shimmer placeholder shown while a section loads its data. The styles live in the shared theme
 * (`.atw-skeleton`, `.atw-skeleton-line`); build the expected layout with placeholder blocks so the
 * section does not jump when real content replaces it.
 *
 * <p>Typical use inside a section container:
 *
 * <pre>{@code
 * Div section = new Div();
 * section.add(LoadingSkeleton.textLines(3));
 * // ... async load completes:
 * section.removeAll();
 * section.add(realContent);
 * }</pre>
 */
public final class LoadingSkeleton {

  private LoadingSkeleton() {}

  /** A single shimmer block of the given height, full width. */
  public static Div block(final String height) {
    Div block = new Div();
    block.addClassNames("atw-skeleton");
    block.getStyle().set("height", height);
    return block;
  }

  /** N full-width text-line placeholders of standard line height. */
  public static List<Div> textLines(final int count) {
    Div[] lines = new Div[count];
    for (int i = 0; i < count; i++) {
      lines[i] = block("var(--lumo-font-size-m)");
    }
    return List.of(lines);
  }

  /** A row of placeholder stat blocks (label over value), for dashboard stat rows. */
  public static Div statRow(final int count) {
    Div row = new Div();
    row.addClassNames("atw-skeleton-row", LumoUtility.Display.FLEX, LumoUtility.Gap.MEDIUM);
    for (int i = 0; i < count; i++) {
      Div cell = block("3.5rem");
      cell.setWidth("7rem");
      row.add(cell);
    }
    return row;
  }

  /** Placeholder for a card grid: count cards of the given min size. */
  public static Div cardGrid(final int count, final String minWidth, final String height) {
    Div row = new Div();
    row.addClassNames("atw-skeleton-row");
    row.getStyle().set("display", "flex");
    row.getStyle().set("flex-wrap", "wrap");
    row.getStyle().set("gap", "var(--lumo-space-m)");
    for (int i = 0; i < count; i++) {
      Div cell = block("9rem");
      cell.getStyle().set("flex", "1 1 " + minWidth);
      row.add(cell);
    }
    return row;
  }

  /** Marks a section as loading while its real content is being built. */
  public static Component withSkeleton(final HasStyle container, final Component... skeleton) {
    for (Component c : skeleton) {
      ((HasComponents) container).add(c);
    }
    return (Component) container;
  }

  /** Small centered loading note for sections that stream content in. */
  public static Span loadingText(final String message) {
    Span span = new Span(message);
    span.addClassNames("atw-skeleton-text");
    return span;
  }
}

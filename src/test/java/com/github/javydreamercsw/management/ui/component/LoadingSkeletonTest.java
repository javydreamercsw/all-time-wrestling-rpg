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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoadingSkeletonTest {

  @Test
  void blockHasSkeletonClassAndHeight() {
    Div block = LoadingSkeleton.block("3rem");
    assertTrue(block.hasClassName("atw-skeleton"));
    assertEquals("3rem", block.getElement().getStyle().get("height"));
  }

  @Test
  void textLinesReturnsRequestedCount() {
    List<Div> lines = LoadingSkeleton.textLines(4);
    assertEquals(4, lines.size());
    lines.forEach(
        line ->
            assertEquals("var(--lumo-font-size-m)", line.getElement().getStyle().get("height")));
  }

  @Test
  void statRowContainsPlaceholderCells() {
    Div row = LoadingSkeleton.statRow(3);
    assertTrue(row.hasClassName("atw-skeleton-row"));
    assertEquals(3, row.getElement().getChildCount());
  }

  @Test
  void cardGridLaysOutFlexCells() {
    Div grid = LoadingSkeleton.cardGrid(2, "12rem", "9rem");
    assertEquals("flex", grid.getElement().getStyle().get("display"));
    assertEquals("wrap", grid.getElement().getStyle().get("flex-wrap"));
    assertEquals(2, grid.getElement().getChildCount());
  }

  @Test
  void withSkeletonAddsComponentsToContainer() {
    Div container = new Div();
    Div skeleton = LoadingSkeleton.block("2rem");
    LoadingSkeleton.withSkeleton(container, skeleton);
    assertEquals(1, container.getComponentCount());
    assertEquals(skeleton, container.getComponentAt(0));
  }

  @Test
  void loadingTextCarriesMessageAndClass() {
    Span span = LoadingSkeleton.loadingText("Loading roster...");
    assertEquals("Loading roster...", span.getText());
    assertTrue(span.hasClassName("atw-skeleton-text"));
  }
}

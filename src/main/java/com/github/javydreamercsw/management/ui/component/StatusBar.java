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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Labeled horizontal meter reused wherever the app shows a bounded stat (health, stamina,
 * condition, morale). Reuses the .status-bar-* CSS shipped for the PlayerCampaignCard so both
 * surfaces stay visually identical.
 *
 * <p>Fill color: below {@code lowLimit} the bar renders in the error color, otherwise in the
 * variant color chosen by {@code styleClass} (e.g. "health", "stamina"). Pass {@code -1} as {@code
 * lowLimit} when there is no low threshold.
 */
public final class StatusBar {

  private StatusBar() {}

  /**
   * Builds a labeled status bar.
   *
   * @param label short name shown above the bar
   * @param valueText right-aligned value text (e.g. "15/20")
   * @param current current value; clamped to [0, max]
   * @param max the bar's full width value; must be &gt; 0
   * @param lowLimit value at or below which the bar turns to the error color, or -1 for none
   * @param styleClass CSS modifier class choosing the fill color family (health/stamina/…)
   */
  public static Div bar(
      final String label,
      final String valueText,
      final int current,
      final int max,
      final int lowLimit,
      final String styleClass) {
    Div container = new Div();
    container.addClassName("status-bar-container");

    Div labels = new Div();
    labels.addClassName("status-bar-label");
    labels.add(new Span(label));
    labels.add(new Span(valueText));
    container.add(labels);

    Div track = new Div();
    track.addClassName("status-bar-track");

    Div fill = new Div();
    fill.addClassName("status-bar-fill");
    fill.addClassName(styleClass);
    int clamped = Math.max(0, Math.min(current, max));
    fill.setWidth((100.0 * clamped / max) + "%");
    if (lowLimit >= 0 && clamped <= lowLimit) {
      fill.addClassName("status-bar-low");
    }
    track.add(fill);

    container.add(track);
    return container;
  }
}

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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Renders guide text that may contain {@code [[icon-name]]} placeholders as a Vaadin component
 * tree. Known icon names are replaced with inline {@link Image} elements served from {@code
 * /icons/<name>.svg}. Unknown names fall back to the raw {@code [[name]]} text and log a warning.
 */
@Slf4j
public final class GuideTextRenderer {

  private static final Pattern ICON_PATTERN = Pattern.compile("\\[\\[([a-z0-9_-]+)]]");

  private GuideTextRenderer() {}

  /**
   * Renders {@code text} into a block-level component, substituting {@code [[icon-name]]} tokens
   * with inline icon images. Returns an empty {@link Div} for null/blank input.
   */
  public static Component render(final String text) {
    if (text == null || text.isBlank()) {
      return new Div();
    }

    Matcher m = ICON_PATTERN.matcher(text);
    if (!m.find()) {
      // Fast path: no icons — wrap in a plain paragraph-style div.
      Div plain = new Div(new Span(text));
      applyParagraphStyle(plain);
      return plain;
    }

    // Slow path: at least one [[icon]] present — build a mixed inline container.
    Div container = new Div();
    applyParagraphStyle(container);
    container
        .getStyle()
        .set("display", "flex")
        .set("flex-wrap", "wrap")
        .set("align-items", "baseline");

    m.reset();
    int last = 0;
    while (m.find()) {
      if (m.start() > last) {
        container.add(new Span(text.substring(last, m.start())));
      }
      String iconName = m.group(1);
      container.add(resolveIcon(iconName));
      last = m.end();
    }
    if (last < text.length()) {
      container.add(new Span(text.substring(last)));
    }
    return container;
  }

  /**
   * Convenience: adds a labelled section (H3 + rendered body) to {@code parent} when {@code text}
   * is non-blank. Drop-in replacement for the manual {@code addSection} helpers in guide views.
   */
  public static void addSection(
      final VerticalLayout parent, final String label, final String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    parent.add(new H3(label));
    parent.add(render(text));
  }

  private static Component resolveIcon(final String name) {
    if (GuideIconRegistry.isValid(name)) {
      Image img = new Image("/icons/" + name + ".svg", name);
      img.getStyle().set("height", "1.1em").set("vertical-align", "middle").set("margin", "0 2px");
      return img;
    }
    log.warn("Unknown icon reference in guide text: [[{}]]", name);
    return new Span("[[" + name + "]]");
  }

  private static void applyParagraphStyle(final Div div) {
    div.getStyle().set("margin", "0.25em 0 0.5em 0");
  }
}

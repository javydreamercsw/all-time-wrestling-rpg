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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Renders guide text that may contain {@code [[icon-name]]} placeholders as a Vaadin component
 * tree. Known icon names are replaced with inline SVG elements loaded from classpath. Unknown names
 * fall back to the raw {@code [[name]]} text and log a warning.
 */
@Slf4j
public final class GuideTextRenderer {

  private static final Pattern ICON_PATTERN = Pattern.compile("\\[\\[([a-z0-9_-]+)]]");

  /** SVG content strings keyed by icon name, loaded once from classpath at class-init time. */
  private static final Map<String, String> SVG_CONTENT;

  static {
    Map<String, String> svgs = new HashMap<>();
    for (String name : GuideIconRegistry.validNames()) {
      try (InputStream is =
          GuideTextRenderer.class.getResourceAsStream("/static/icons/" + name + ".svg")) {
        if (is != null) {
          // Replace hard-coded pixel dimensions with em units so the icon scales with text.
          String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
          svgs.put(
              name, raw.replace("width=\"16\" height=\"16\"", "width=\"1.1em\" height=\"1.1em\""));
        } else {
          log.warn("SVG not found on classpath for icon: {}", name);
        }
      } catch (Exception e) {
        log.warn("Failed to load SVG for icon: {}", name, e);
      }
    }
    SVG_CONTENT = Collections.unmodifiableMap(svgs);
  }

  private GuideTextRenderer() {}

  /**
   * Renders {@code text} into a block-level component, substituting {@code [[icon-name]]} tokens
   * with inline SVG icons. Returns an empty {@link Div} for null/blank input.
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
        .set("align-items", "center");

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
    String svg = SVG_CONTENT.get(name);
    if (svg != null) {
      Span iconWrapper = new Span();
      iconWrapper
          .getStyle()
          .set("display", "inline-flex")
          .set("align-items", "center")
          .set("vertical-align", "middle")
          .set("margin", "0 1px");
      iconWrapper.getElement().setProperty("innerHTML", svg);
      return iconWrapper;
    }
    log.warn("Unknown icon reference in guide text: [[{}]]", name);
    return new Span("[[" + name + "]]");
  }

  private static void applyParagraphStyle(final Div div) {
    div.getStyle().set("margin", "0.25em 0 0.5em 0");
  }
}

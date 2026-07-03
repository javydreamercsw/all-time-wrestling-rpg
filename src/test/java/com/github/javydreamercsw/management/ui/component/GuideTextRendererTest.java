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

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuideTextRendererTest {

  @Test
  void nullText_returnsEmptyDiv() {
    Component result = GuideTextRenderer.render(null);
    assertThat(result).isInstanceOf(Div.class);
    assertThat(((Div) result).getChildren().count()).isZero();
  }

  @Test
  void blankText_returnsEmptyDiv() {
    Component result = GuideTextRenderer.render("   ");
    assertThat(result).isInstanceOf(Div.class);
    assertThat(((Div) result).getChildren().count()).isZero();
  }

  @Test
  void plainText_noIcons_returnsSpanWrappedInDiv() {
    Component result = GuideTextRenderer.render("No icons here.");
    assertThat(result).isInstanceOf(Div.class);
    List<Component> children = ((Div) result).getChildren().toList();
    assertThat(children).hasSize(1);
    assertThat(children.get(0)).isInstanceOf(Span.class);
    assertThat(((Span) children.get(0)).getText()).isEqualTo("No icons here.");
  }

  @Test
  void knownIcon_rendersInlineImage() {
    // "pin" is a known icon in the manifest
    Component result = GuideTextRenderer.render("Execute a [[pin]] to win.");
    assertThat(result).isInstanceOf(Div.class);
    List<Component> children = ((Div) result).getChildren().toList();

    assertThat(children).hasSize(3);
    assertThat(children.get(0)).isInstanceOf(Span.class);
    assertThat(((Span) children.get(0)).getText()).isEqualTo("Execute a ");
    assertThat(children.get(1)).isInstanceOf(Image.class);
    assertThat(((Image) children.get(1)).getSrc()).isEqualTo("/icons/pin.svg");
    assertThat(children.get(2)).isInstanceOf(Span.class);
    assertThat(((Span) children.get(2)).getText()).isEqualTo(" to win.");
  }

  @Test
  void unknownIcon_rendersLiteralFallback() {
    Component result = GuideTextRenderer.render("Spend [[mana]] to cast.");
    assertThat(result).isInstanceOf(Div.class);
    List<Component> children = ((Div) result).getChildren().toList();

    boolean hasFallbackSpan =
        children.stream()
            .filter(c -> c instanceof Span)
            .anyMatch(c -> ((Span) c).getText().contains("[[mana]]"));
    assertThat(hasFallbackSpan).isTrue();
  }

  @Test
  void multipleIcons_rendersAllInOrder() {
    Component result = GuideTextRenderer.render("Spend [[stamina]] to play a [[card]].");
    List<Component> children = ((Div) result).getChildren().toList();

    long imageCount = children.stream().filter(c -> c instanceof Image).count();
    assertThat(imageCount).isEqualTo(2);

    List<String> imageSrcs =
        children.stream().filter(c -> c instanceof Image).map(c -> ((Image) c).getSrc()).toList();
    assertThat(imageSrcs).containsExactly("/icons/stamina.svg", "/icons/card.svg");
  }

  @Test
  void iconAtStart_rendersCorrectly() {
    Component result = GuideTextRenderer.render("[[pin]] wins the match.");
    List<Component> children = ((Div) result).getChildren().toList();
    assertThat(children.get(0)).isInstanceOf(Image.class);
    assertThat(((Image) children.get(0)).getSrc()).isEqualTo("/icons/pin.svg");
  }

  @Test
  void iconAtEnd_rendersCorrectly() {
    Component result = GuideTextRenderer.render("Win with a [[finisher]]");
    List<Component> children = ((Div) result).getChildren().toList();
    assertThat(children.get(children.size() - 1)).isInstanceOf(Image.class);
    assertThat(((Image) children.get(children.size() - 1)).getSrc())
        .isEqualTo("/icons/finisher.svg");
  }

  @Test
  void addSection_nullText_addsNothing() {
    VerticalLayout layout = new VerticalLayout();
    GuideTextRenderer.addSection(layout, "Overview", null);
    assertThat(layout.getComponentCount()).isZero();
  }

  @Test
  void addSection_withText_addsHeadingAndBody() {
    VerticalLayout layout = new VerticalLayout();
    GuideTextRenderer.addSection(layout, "Setup", "Place your [[health]] cubes.");
    assertThat(layout.getComponentCount()).isEqualTo(2);
  }

  @Test
  void registryLoaded_knownIconsPresent() {
    assertThat(GuideIconRegistry.isValid("pin")).isTrue();
    assertThat(GuideIconRegistry.isValid("stamina")).isTrue();
    assertThat(GuideIconRegistry.isValid("health")).isTrue();
    assertThat(GuideIconRegistry.isValid("card")).isTrue();
    assertThat(GuideIconRegistry.isValid("momentum")).isTrue();
    assertThat(GuideIconRegistry.isValid("reversal")).isTrue();
    assertThat(GuideIconRegistry.isValid("tag")).isTrue();
    assertThat(GuideIconRegistry.isValid("finisher")).isTrue();
  }

  @Test
  void registryLoaded_unknownIconsAbsent() {
    assertThat(GuideIconRegistry.isValid("mana")).isFalse();
    assertThat(GuideIconRegistry.isValid("")).isFalse();
    assertThat(GuideIconRegistry.isValid("PIN")).isFalse(); // case-sensitive
  }
}

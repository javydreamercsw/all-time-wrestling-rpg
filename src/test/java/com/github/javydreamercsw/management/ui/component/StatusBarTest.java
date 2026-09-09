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
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatusBarTest {

  @Test
  @DisplayName("bar renders label, value text, and percentage width")
  void bar_rendersLabelValueAndWidth() {
    Div bar = StatusBar.bar("Physical Condition", "80%", 80, 100, 50, "condition");
    assertThat(bar.hasClassName("status-bar-container")).isTrue();

    Div track = (Div) bar.getComponentAt(1);
    Div fill = (Div) track.getComponentAt(0);
    assertEquals("80.0%", fill.getWidth());
    assertThat(fill.hasClassName("condition")).isTrue();
    assertThat(fill.hasClassName("status-bar-low")).isFalse();
  }

  @Test
  @DisplayName("values at or below the low limit get the low class")
  void bar_lowValue_getsLowClass() {
    Div bar = StatusBar.bar("Physical Condition", "40%", 40, 100, 50, "condition");
    Div track = (Div) bar.getComponentAt(1);
    Div fill = (Div) track.getComponentAt(0);
    assertThat(fill.hasClassName("status-bar-low")).isTrue();
  }

  @Test
  @DisplayName("current is clamped to [0, max]")
  void bar_clampsCurrent() {
    Div over = StatusBar.bar("Health", "25/20", 25, 20, -1, "health");
    Div track = (Div) over.getComponentAt(1);
    Div fill = (Div) track.getComponentAt(0);
    assertEquals("100.0%", fill.getWidth());

    Div empty = StatusBar.bar("Health", "0/20", 0, 20, -1, "health");
    Div emptyTrack = (Div) empty.getComponentAt(1);
    Div emptyFill = (Div) emptyTrack.getComponentAt(0);
    assertEquals("0.0%", emptyFill.getWidth());
  }

  @Test
  @DisplayName("lowLimit -1 never applies the low class")
  void bar_noLowLimit() {
    Div bar = StatusBar.bar("Stamina", "5/100", 5, 100, -1, "condition");
    Div track = (Div) bar.getComponentAt(1);
    Div fill = (Div) track.getComponentAt(0);
    assertThat(fill.hasClassName("status-bar-low")).isFalse();
  }
}

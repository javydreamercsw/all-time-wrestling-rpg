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
package com.github.javydreamercsw.management.ui.view.match;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.html.Span;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QrCodeDialogTest extends AbstractViewTest {

  private QrCodeDialog dialog;

  @BeforeEach
  void setup() {
    dialog = new QrCodeDialog(42L);
  }

  @Test
  @DisplayName("QrCodeDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "QrCodeDialog should not be null");
  }

  @Test
  @DisplayName("Dialog should contain a Span with the match URL")
  void urlSpanExists() {
    List<Span> spans = _find(dialog, Span.class);
    assertFalse(spans.isEmpty(), "Expected at least one Span with the match URL");
  }
}

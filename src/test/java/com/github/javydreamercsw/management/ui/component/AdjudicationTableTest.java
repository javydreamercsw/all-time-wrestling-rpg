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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.vaadin.flow.component.textfield.IntegerField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdjudicationTableTest {

  @Test
  @DisplayName("Row renders stable element ids and prefilled values")
  void row_idsAndPrefill() {
    AdjudicationTable.RowWithFields built =
        AdjudicationTable.wrestlerRow("Test Wrestler", 42L, 20, 3, 2, 15);

    IntegerField momentum = built.momentum();
    IntegerField stamina = built.stamina();
    IntegerField health = built.health();

    assertEquals("final-momentum-42", momentum.getId().orElse(""));
    assertEquals("final-stamina-42", stamina.getId().orElse(""));
    assertEquals("final-health-42", health.getId().orElse(""));

    assertEquals(3, momentum.getValue());
    assertEquals(2, stamina.getValue());
    assertEquals(15, health.getValue());
  }

  @Test
  @DisplayName("Null existing values leave the fields empty and health is bounded")
  void row_nullPrefillAndBounds() {
    AdjudicationTable.RowWithFields built =
        AdjudicationTable.wrestlerRow("Other Wrestler", 7L, 20, null, null, null);

    assertNull(built.momentum().getValue());
    assertNull(built.stamina().getValue());
    assertNull(built.health().getValue());
    assertEquals(0, built.health().getMin());
    assertEquals(20, built.health().getMax());
  }
}

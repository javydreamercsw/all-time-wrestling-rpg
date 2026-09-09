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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Per-wrestler final-stat entry for match adjudication. One row per wrestler with momentum /
 * stamina / health side by side, replacing three stacked full-width field groups that made the
 * panel scroll for screenfuls on phone.
 *
 * <p>Field ids stay {@code final-momentum-<id>} / {@code final-stamina-<id>} / {@code
 * final-health-<id>} — the E2E and unit suites locate them individually.
 */
public final class AdjudicationTable {

  private AdjudicationTable() {}

  /**
   * Builds a compact stats row for one wrestler.
   *
   * @param wrestlerName display name in the row header
   * @param wrestlerId stable id used for the element ids of each field
   * @param startingHealth max for the health field
   * @param existing current values to prefill; any may be null
   * @return the row plus the three fields for the caller's maps
   */
  public static RowWithFields wrestlerRow(
      final String wrestlerName,
      final Long wrestlerId,
      final int startingHealth,
      final Integer existingMomentum,
      final Integer existingStamina,
      final Integer existingHealth) {

    Div row = new Div();
    row.addClassNames(
        "adjudication-row",
        LumoUtility.Display.FLEX,
        LumoUtility.FlexWrap.WRAP,
        LumoUtility.Gap.SMALL,
        LumoUtility.AlignItems.END,
        LumoUtility.Padding.Bottom.SMALL,
        LumoUtility.Margin.Bottom.SMALL);
    row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

    Span name = new Span(wrestlerName);
    name.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.SMALL);
    Div nameCell = new Div(name);
    nameCell.getStyle().set("flex", "1 1 100%");

    IntegerField momentum = new IntegerField("Momentum");
    momentum.setId("final-momentum-" + wrestlerId);
    momentum.setPlaceholder("e.g. 3");
    momentum.setWidth("7rem");

    IntegerField stamina = new IntegerField("Stamina");
    stamina.setId("final-stamina-" + wrestlerId);
    stamina.setPlaceholder("e.g. 3");
    stamina.setWidth("7rem");

    IntegerField health = new IntegerField("Health");
    health.setId("final-health-" + wrestlerId);
    health.setPlaceholder("Starting: " + startingHealth);
    health.setMin(0);
    health.setMax(startingHealth);
    health.setWidth("7rem");

    momentum.setValue(existingMomentum);
    stamina.setValue(existingStamina);
    health.setValue(existingHealth);

    Div momentumCell = new Div(momentum);
    Div staminaCell = new Div(stamina);
    Div healthCell = new Div(health);
    momentumCell.getStyle().set("flex", "1 1 7rem");
    staminaCell.getStyle().set("flex", "1 1 7rem");
    healthCell.getStyle().set("flex", "1 1 7rem");

    row.add(nameCell, momentumCell, staminaCell, healthCell);
    return new RowWithFields(row, momentum, stamina, health);
  }

  /** The rendered row plus its three inputs, ready to register in the caller's maps. */
  public record RowWithFields(
      Div row, IntegerField momentum, IntegerField stamina, IntegerField health) {}
}

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

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;

/** Visual representation of the Face/Heel progress track. */
public class AlignmentTrackComponent extends Div {

  public AlignmentTrackComponent(WrestlerAlignment alignment) {
    addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Gap.SMALL,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.Width.FULL);

    AlignmentType type = alignment.getAlignmentType();
    int currentLevel = alignment.getLevel();

    Span title =
        new Span(
            "Campaign Alignment: " + (type == AlignmentType.NEUTRAL ? "NEUTRAL" : type.name()));
    title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.PRIMARY);
    add(title);

    Div trackContainer = new Div();
    trackContainer.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.JustifyContent.BETWEEN,
        LumoUtility.AlignItems.CENTER,
        LumoUtility.Position.RELATIVE,
        LumoUtility.Padding.Horizontal.LARGE,
        LumoUtility.Margin.Vertical.MEDIUM);
    trackContainer.setHeight("40px");

    // Multi-color gradient for bidirectional track
    trackContainer
        .getStyle()
        .set("background", "linear-gradient(to right, #ef9a9a 0%, #eeeeee 50%, #a5d6a7 100%)");
    trackContainer.getStyle().set("border-radius", "20px");

    // Heel Side (5 down to 1)
    for (int i = 5; i >= 1; i--) {
      trackContainer.add(createSpot(AlignmentType.HEEL, i, type, currentLevel));
    }

    // Neutral Center (0)
    trackContainer.add(createSpot(AlignmentType.NEUTRAL, 0, type, currentLevel));

    // Face Side (1 to 5)
    for (int i = 1; i <= 5; i++) {
      trackContainer.add(createSpot(AlignmentType.FACE, i, type, currentLevel));
    }

    add(trackContainer);

    // Legend / Milestone Description (Side-by-Side)
    Div legendContainer = new Div();
    legendContainer.addClassNames(
        LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.Gap.MEDIUM);

    Div heelLegend = new Div();
    heelHelegendStyle(heelLegend);
    addMilestoneText(heelLegend, "1", "Unlock first Level 1 Heel Card.");
    addMilestoneText(heelLegend, "4", "Unlock Level 2 Card. (Lose 1 L1 card)");
    addMilestoneText(heelLegend, "5", "Regain slot with another Level 1 Card.");

    Div faceLegend = new Div();
    faceLegendStyle(faceLegend);
    addMilestoneText(faceLegend, "1", "Unlock first Level 1 Face Card.");
    addMilestoneText(faceLegend, "4", "Unlock powerful Level 2 Card.");
    addMilestoneText(faceLegend, "5", "Unlock Level 3 Finisher! (Lose 1 L1 card)");

    legendContainer.add(heelLegend, faceLegend);
    add(legendContainer);
  }

  private void heelHelegendStyle(Div div) {
    div.addClassNames(
        LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.XSMALL);
    div.setWidth("50%");
    div.getStyle().set("border-left", "3px solid #ef9a9a");
    div.getStyle().set("padding-left", "10px");
  }

  private void faceLegendStyle(Div div) {
    div.addClassNames(
        LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.XSMALL);
    div.setWidth("50%");
    div.getStyle().set("border-left", "3px solid #a5d6a7");
    div.getStyle().set("padding-left", "10px");
  }

  private Div createSpot(
      AlignmentType spotType, int level, AlignmentType currentType, int currentLevel) {
    Div spot = new Div();
    spot.setWidth("30px");
    spot.setHeight("30px");
    spot.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.AlignItems.CENTER,
        LumoUtility.JustifyContent.CENTER,
        LumoUtility.BorderRadius.FULL,
        LumoUtility.FontWeight.BOLD,
        LumoUtility.FontSize.XSMALL);

    spot.getStyle().set("border", "2px solid white");
    spot.setText(level == 0 ? "N" : String.valueOf(level));

    // Tooltip Info
    String tooltip = getTooltipForMilestone(spotType, level);
    if (!tooltip.isEmpty()) {
      spot.getElement().setAttribute("title", tooltip);
    }

    boolean isCurrent =
        (spotType == currentType && level == currentLevel)
            || (level == 0 && currentType == AlignmentType.NEUTRAL);

    if (isCurrent) {
      if (spotType == AlignmentType.HEEL) spot.getStyle().set("background-color", "#f44336");
      else if (spotType == AlignmentType.FACE) spot.getStyle().set("background-color", "#4caf50");
      else spot.getStyle().set("background-color", "#9e9e9e"); // Neutral

      spot.getStyle().set("color", "white");
      spot.getStyle().set("transform", "scale(1.4)");
      spot.getStyle().set("box-shadow", "0 0 15px rgba(0,0,0,0.3)");
      spot.getStyle().set("z-index", "1");
    } else {
      if (spotType == AlignmentType.HEEL) {
        spot.getStyle().set("background-color", "#ffcdd2");
        spot.getStyle().set("color", "rgba(0,0,0,0.3)");
      } else if (spotType == AlignmentType.FACE) {
        spot.getStyle().set("background-color", "#c8e6c9");
        spot.getStyle().set("color", "rgba(0,0,0,0.3)");
      } else {
        spot.getStyle().set("background-color", "white");
        spot.getStyle().set("color", "#9e9e9e");
      }
    }

    // Milestone Markers
    if (level == 1 || level == 4 || level == 5) {
      spot.getStyle().set("border-color", "#fb8c00");
    }

    return spot;
  }

  private String getTooltipForMilestone(AlignmentType type, int level) {
    if (level == 0) return "Starting Point: Neutral";
    if (level == 1) return "Milestone: Pick your first Level 1 Ability Card.";
    if (type == AlignmentType.FACE) {
      if (level == 4) return "Milestone: Unlock a Level 2 Ability Card.";
      if (level == 5) return "Milestone: Unlock Level 3 Finisher! (Lose 1 Level 1 card)";
    } else if (type == AlignmentType.HEEL) {
      if (level == 4) return "Milestone: Unlock Level 2 Ability Card. (Lose 1 Level 1 card)";
      if (level == 5) return "Milestone: Regain lost slot with another Level 1 Ability.";
    }
    return "";
  }

  private void addMilestoneText(Div container, String level, String text) {
    Paragraph p = new Paragraph();
    p.addClassNames(
        LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.NONE);
    Span bold = new Span("Level " + level + ": ");
    bold.addClassNames(LumoUtility.FontWeight.BOLD);
    p.add(bold, new Span(text));
    container.add(p);
  }
}

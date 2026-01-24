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

import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;

/** Component to display a Campaign Ability Card with alignment-based styling. */
public class CampaignAbilityCardComponent extends Div {

  public CampaignAbilityCardComponent(CampaignAbilityCard card) {
    addClassNames(
        LumoUtility.Padding.MEDIUM,
        LumoUtility.Border.ALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Display.FLEX,
        LumoUtility.FlexDirection.COLUMN,
        LumoUtility.Gap.SMALL,
        LumoUtility.BoxShadow.SMALL);

    setMinWidth("250px");
    setMaxWidth("300px");

    // Alignment-based colors
    if (card.getAlignmentType() == AlignmentType.HEEL) {
      getStyle().set("background-color", "#fff5f5"); // Very light red
      getStyle().set("border-left", "5px solid #ffcdd2"); // Light red border
    } else {
      getStyle().set("background-color", "#f1f8e9"); // Very light green
      getStyle().set("border-left", "5px solid #c8e6c9"); // Light green border
    }

    // Title and Level
    H4 title = new H4(card.getName());
    title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.MEDIUM);

    Span levelBadge = new Span("LVL " + card.getLevel());
    levelBadge.getElement().getThemeList().add("badge pill small");
    if (card.getAlignmentType() == AlignmentType.HEEL) {
      levelBadge.getStyle().set("background-color", "#ef9a9a");
      levelBadge.getStyle().set("color", "white");
    } else {
      levelBadge.getStyle().set("background-color", "#a5d6a7");
      levelBadge.getStyle().set("color", "white");
    }

    Div header = new Div(title, levelBadge);
    header.addClassNames(
        LumoUtility.Display.FLEX,
        LumoUtility.JustifyContent.BETWEEN,
        LumoUtility.AlignItems.CENTER,
        LumoUtility.Width.FULL);

    // Description
    Paragraph desc = new Paragraph(card.getDescription());
    desc.addClassNames(
        LumoUtility.FontSize.SMALL,
        LumoUtility.TextColor.SECONDARY,
        LumoUtility.Margin.Bottom.SMALL);
    desc.getStyle().set("font-style", "italic");

    add(header, desc);

    // Primary effect
    addEffect(card.getTiming(), card.getEffectScript(), card.isOneTimeUse(), "Primary Effect");

    // Secondary effect (if present)
    if (card.getSecondaryEffectScript() != null && !card.getSecondaryEffectScript().isBlank()) {
      Div divider = new Div();
      divider.setHeight("1px");
      divider.getStyle().set("background-color", "rgba(0,0,0,0.1)");
      divider.addClassNames(LumoUtility.Margin.Vertical.SMALL);
      add(divider);
      addEffect(
          card.getSecondaryTiming(),
          card.getSecondaryEffectScript(),
          card.isSecondaryOneTimeUse(),
          "Secondary Effect");
    }
  }

  private void addEffect(AbilityTiming timing, String script, boolean oneTime, String label) {
    Div effectDiv = new Div();
    effectDiv.addClassNames(
        LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Gap.XSMALL);

    HorizontalLayout badges = new HorizontalLayout();
    badges.addClassNames(LumoUtility.Gap.XSMALL, LumoUtility.AlignItems.CENTER);

    Span timingBadge = new Span(timing != null ? timing.name() : "ANY");
    timingBadge.getElement().getThemeList().add("badge small contrast");

    effectDiv.add(timingBadge);

    if (oneTime) {
      Span oneTimeBadge = new Span("Single Use");
      oneTimeBadge.getElement().setAttribute("title", "Discarded from inventory after one use.");
      oneTimeBadge.getElement().getThemeList().add("badge small error");
      effectDiv.add(oneTimeBadge);
    } else {
      Span activeBadge = new Span("Once per Match");
      activeBadge.getElement().setAttribute("title", "Can be activated once in every match.");
      activeBadge.getElement().getThemeList().add("badge small success");
      effectDiv.add(activeBadge);
    }

    Span scriptSpan = new Span(script);
    scriptSpan.addClassNames(
        LumoUtility.FontSize.XSMALL,
        LumoUtility.Background.CONTRAST_5,
        LumoUtility.Padding.XSMALL,
        LumoUtility.BorderRadius.SMALL);
    scriptSpan.getStyle().set("font-family", "monospace");

    effectDiv.add(scriptSpan);
    add(effectDiv);
  }

  private static class HorizontalLayout extends Div {
    public HorizontalLayout() {
      addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW);
    }
  }
}

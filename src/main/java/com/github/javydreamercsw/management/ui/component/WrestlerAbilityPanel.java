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
import com.github.javydreamercsw.management.domain.wrestler.AbilityType;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbility;
import com.github.javydreamercsw.management.service.wrestler.AbilityReminderTextService;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Renders a wrestler's ability list grouped into core, wrestler-specific, and unlockable sections.
 */
public class WrestlerAbilityPanel extends VerticalLayout {

  public static final Set<String> CORE_ABILITY_NAMES =
      Set.of("Discard Block", "Stamina Block", "Recover", "Taunt");

  @Nullable private final AbilityReminderTextService reminderTextService;

  public WrestlerAbilityPanel(final List<WrestlerAbility> abilities) {
    this(abilities, null);
  }

  /**
   * Reminder mode: when a {@link AbilityReminderTextService} is supplied, each entry additionally
   * shows a color-coded timing chip and a human-readable trigger line so players remember when to
   * apply the ability at the table.
   */
  public WrestlerAbilityPanel(
      final List<WrestlerAbility> abilities,
      @Nullable final AbilityReminderTextService reminderTextService) {
    this.reminderTextService = reminderTextService;
    setPadding(false);
    setSpacing(false);
    build(abilities);
  }

  private void build(final List<WrestlerAbility> abilities) {
    if (abilities.isEmpty()) {
      add(new Paragraph("No abilities defined for this wrestler."));
      return;
    }

    List<WrestlerAbility> core =
        abilities.stream()
            .filter(a -> a.isDefault() && CORE_ABILITY_NAMES.contains(a.getName()))
            .sorted(Comparator.comparing(WrestlerAbility::getName))
            .collect(Collectors.toList());
    List<WrestlerAbility> specific =
        abilities.stream()
            .filter(a -> a.isDefault() && !CORE_ABILITY_NAMES.contains(a.getName()))
            .sorted(Comparator.comparing(WrestlerAbility::getName))
            .collect(Collectors.toList());
    List<WrestlerAbility> unlockable =
        abilities.stream()
            .filter(a -> !a.isDefault())
            .sorted(Comparator.comparing(WrestlerAbility::getName))
            .collect(Collectors.toList());

    if (!core.isEmpty()) {
      add(new H3("Core Abilities"));
      core.forEach(a -> add(buildEntry(a, reminderTextService)));
    }
    if (!specific.isEmpty()) {
      add(new H3("Wrestler Abilities"));
      specific.forEach(a -> add(buildEntry(a, reminderTextService)));
    }
    if (!unlockable.isEmpty()) {
      VerticalLayout unlockableContent = new VerticalLayout();
      unlockableContent.setPadding(false);
      unlockableContent.setSpacing(false);
      unlockable.forEach(a -> unlockableContent.add(buildEntry(a, reminderTextService)));
      Details unlockableSection = new Details("Unlockable Abilities", unlockableContent);
      unlockableSection.setOpened(false);
      add(unlockableSection);
    }
  }

  public static Div buildEntry(final WrestlerAbility ability) {
    return buildEntry(ability, null);
  }

  public static Div buildEntry(
      final WrestlerAbility ability,
      @Nullable final AbilityReminderTextService reminderTextService) {
    Div card = new Div();
    card.addClassNames(
        LumoUtility.Padding.SMALL,
        LumoUtility.Margin.Bottom.XSMALL,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.Background.CONTRAST_5);

    HorizontalLayout header = new HorizontalLayout();
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    header.setSpacing(true);

    Span nameSpan = new Span(ability.getName());
    nameSpan.getStyle().set("font-weight", "bold");

    Span badge = buildBadge(ability);

    if (!ability.isDefault() && ability.getSwapCondition() != null) {
      Span swappable = new Span("swappable");
      swappable.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.XSMALL);
      header.add(nameSpan, badge, swappable);
    } else {
      header.add(nameSpan, badge);
    }
    if (reminderTextService != null && ability.getTiming() != null) {
      header.add(buildTimingChip(ability.getTiming()));
    }
    card.add(header);

    if (reminderTextService != null) {
      String trigger = reminderTextService.triggerText(ability);
      if (!trigger.isBlank()) {
        Span triggerLine = new Span("Trigger: " + trigger);
        triggerLine.setId("ability-trigger-" + (ability.getId() != null ? ability.getId() : ""));
        triggerLine.addClassNames(
            LumoUtility.FontSize.XSMALL,
            LumoUtility.TextColor.SECONDARY,
            LumoUtility.Display.BLOCK);
        card.add(triggerLine);
      }
    }

    if (ability.getDescription() != null && !ability.getDescription().isBlank()) {
      Div desc = (Div) GuideTextRenderer.render(ability.getDescription());
      desc.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.Margin.NONE);
      card.add(desc);
    }

    return card;
  }

  /** Color-coded chip for when the ability applies (offense/defense/pinned/backstage). */
  public static Span buildTimingChip(final AbilityTiming timing) {
    Span chip =
        new Span(
            switch (timing) {
              case OFFENSE -> "Offense";
              case DEFENSE -> "Defense";
              case PINNED -> "Pinned";
              case BACKSTAGE -> "Backstage";
            });
    chip.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.Padding.Horizontal.SMALL);
    chip.getStyle().set("border-radius", "var(--lumo-border-radius-l)").set("font-weight", "600");
    switch (timing) {
      case OFFENSE -> {
        chip.getStyle().set("background", "var(--lumo-error-color-10pct)");
        chip.getStyle().set("color", "var(--lumo-error-text-color)");
      }
      case DEFENSE -> {
        chip.getStyle().set("background", "var(--lumo-primary-color-10pct)");
        chip.getStyle().set("color", "var(--lumo-primary-text-color)");
      }
      case PINNED -> {
        chip.getStyle().set("background", "var(--lumo-warning-color-10pct)");
        chip.getStyle().set("color", "var(--lumo-warning-text-color)");
      }
      case BACKSTAGE -> {
        chip.getStyle().set("background", "var(--lumo-contrast-10pct)");
        chip.getStyle().set("color", "var(--lumo-secondary-text-color)");
      }
    }
    return chip;
  }

  public static Span buildBadge(final WrestlerAbility ability) {
    Span badge;
    if (ability.getAbilityType() == AbilityType.USES_LIMITED && ability.getMaxUses() != null) {
      int uses = ability.getMaxUses();
      badge = new Span(uses + (uses == 1 ? " use" : " uses"));
      badge.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.Padding.Horizontal.SMALL);
      badge.getStyle().set("background", "var(--lumo-primary-color-10pct)");
      badge.getStyle().set("color", "var(--lumo-primary-text-color)");
      badge.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
      badge.getStyle().set("font-weight", "600");
    } else if (ability.getAbilityType() == AbilityType.CONDITIONAL) {
      badge = new Span("triggered");
      badge.addClassNames(
          LumoUtility.FontSize.XSMALL,
          LumoUtility.TextColor.SECONDARY,
          LumoUtility.Padding.Horizontal.XSMALL);
      badge.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
      badge.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
    } else {
      badge = new Span();
    }
    return badge;
  }
}

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

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.wrestler.AbilityType;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbility;
import com.github.javydreamercsw.management.service.wrestler.AbilityReminderTextService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Karibu tests for the reminder/interactive modes of the ability panel. */
class WrestlerAbilityPanelTest extends AbstractViewTest {

  private final AbilityReminderTextService reminderTextService = new AbilityReminderTextService();

  private WrestlerAbility ability(
      final long id, final String name, final AbilityType type, final Integer maxUses) {
    WrestlerAbility a = new WrestlerAbility();
    a.setId(id);
    a.setName(name);
    a.setDescription("Does something impressive.");
    a.setAbilityType(type);
    a.setMaxUses(maxUses);
    a.setTiming(AbilityTiming.OFFENSE);
    a.setUnlockCondition("event == 'ATTACK_SUCCESS'");
    return a;
  }

  @Test
  void reminderModeShowsTriggerLine() {
    WrestlerAbilityPanel panel =
        new WrestlerAbilityPanel(
            List.of(ability(1L, "Frog Splash", AbilityType.CONDITIONAL, null)),
            reminderTextService);
    UI.getCurrent().add(panel);

    Span trigger = _get(Span.class, spec -> spec.withId("ability-trigger-1"));
    assertThat(trigger.getText()).isEqualTo("Trigger: After your attack succeeds");
  }

  @Test
  void basicModeHasNoTriggerLineOrButtons() {
    WrestlerAbilityPanel panel =
        new WrestlerAbilityPanel(
            List.of(ability(1L, "Frog Splash", AbilityType.CONDITIONAL, null)));
    UI.getCurrent().add(panel);

    assertThat(_find(Span.class, spec -> spec.withId("ability-trigger-1"))).isEmpty();
    assertThat(_find(Button.class, spec -> spec.withId("ability-mark-used-1"))).isEmpty();
  }

  @Test
  void markUsedFiresListenerAndDecrementsCounter() {
    List<WrestlerAbility> used = new ArrayList<>();
    WrestlerAbilityPanel panel =
        new WrestlerAbilityPanel(
            List.of(ability(7L, "Power Kickout", AbilityType.USES_LIMITED, 2)),
            reminderTextService,
            used::add,
            Map.of());
    UI.getCurrent().add(panel);

    Span counter = _get(Span.class, spec -> spec.withId("ability-uses-left-7"));
    assertThat(counter.getText()).isEqualTo("2 left");

    Button markUsed = _get(Button.class, spec -> spec.withId("ability-mark-used-7"));
    markUsed.click();
    assertThat(used).hasSize(1);
    assertThat(counter.getText()).isEqualTo("1 left");

    markUsed.click();
    markUsed.click(); // advisory: goes to 0 and stays there, never disables
    assertThat(counter.getText()).isEqualTo("0 left");
    assertThat(used).hasSize(3);
    assertThat(markUsed.isEnabled()).isTrue();
  }

  @Test
  void counterSeedsFromInitialUsedCounts() {
    WrestlerAbilityPanel panel =
        new WrestlerAbilityPanel(
            List.of(ability(9L, "Power Kickout", AbilityType.USES_LIMITED, 3)),
            reminderTextService,
            a -> {},
            Map.of("Power Kickout", 2));
    UI.getCurrent().add(panel);

    Span counter = _get(Span.class, spec -> spec.withId("ability-uses-left-9"));
    assertThat(counter.getText()).isEqualTo("1 left");
  }

  @Test
  void alwaysOnAbilitiesGetNoMarkUsedButton() {
    WrestlerAbilityPanel panel =
        new WrestlerAbilityPanel(
            List.of(ability(3L, "Iron Chin", AbilityType.ALWAYS_ON, null)),
            reminderTextService,
            a -> {},
            Map.of());
    UI.getCurrent().add(panel);

    assertThat(_find(Button.class, spec -> spec.withId("ability-mark-used-3"))).isEmpty();
  }
}

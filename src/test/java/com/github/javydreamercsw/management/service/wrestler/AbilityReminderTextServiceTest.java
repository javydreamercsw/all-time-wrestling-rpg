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
package com.github.javydreamercsw.management.service.wrestler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.wrestler.AbilityCategory;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbility;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Unit tests for the static (non-evaluating) ability reminder text humanizer. */
class AbilityReminderTextServiceTest {

  private final AbilityReminderTextService service = new AbilityReminderTextService();

  private WrestlerAbility ability(final String unlockCondition) {
    WrestlerAbility a = new WrestlerAbility();
    a.setName("Test Ability");
    a.setUnlockCondition(unlockCondition);
    return a;
  }

  // ── triggerText: event tokens ─────────────────────────────────────────────

  @Test
  void simpleEventConditionIsHumanized() {
    assertThat(service.triggerText(ability("event == 'ATTACK_INCOMING'")))
        .isEqualTo("When an attack is incoming");
  }

  @Test
  void eventWithQualifierIsHumanized() {
    assertThat(
            service.triggerText(
                ability("event == 'ATTACK_SUCCESS' && match.attackCardType == 'AERIAL'")))
        .isEqualTo("After your attack succeeds and the attack is an aerial");
  }

  @Test
  void orQualifiersAreJoined() {
    assertThat(
            service.triggerText(
                ability(
                    "event == 'ATTACK_INCOMING' && match.attackCardType == 'KICK'"
                        + " || match.attackCardType == 'KNEE'")))
        .isEqualTo("When an attack is incoming and the attack is a kick or the attack is a knee");
  }

  @Test
  void numericQualifiersAreHumanized() {
    assertThat(service.triggerText(ability("event == 'DAMAGE_TAKEN' && match.damageAmount >= 3")))
        .isEqualTo("After taking damage and the damage is 3 or more");
    assertThat(service.triggerText(ability("wrestler.momentum >= 3")))
        .isEqualTo("your momentum is 3 or more");
  }

  @Test
  void abilityTokensQualifierIsHumanized() {
    assertThat(service.triggerText(ability("wrestler.abilityTokens['Beast Rage'] >= 4")))
        .isEqualTo("you have 4 or more Beast Rage tokens");
  }

  // ── triggerText: fallbacks ────────────────────────────────────────────────

  @Test
  void unknownConditionFallsBackToTiming() {
    WrestlerAbility a = ability("match.totallyUnknownVariable == 'X'");
    a.setTiming(AbilityTiming.DEFENSE);
    assertThat(service.triggerText(a)).isEqualTo("On defense");
  }

  @Test
  void unknownConditionWithoutTimingFallsBackToCategory() {
    WrestlerAbility a = ability("match.totallyUnknownVariable == 'X'");
    a.setCategory(AbilityCategory.PASSIVE);
    assertThat(service.triggerText(a)).isEqualTo("Passive — always on");
  }

  @Test
  void unknownConditionWithoutTimingOrCategoryFallsBackToRawText() {
    WrestlerAbility a = ability("match.totallyUnknownVariable == 'X'");
    assertThat(service.triggerText(a)).isEqualTo("match.totallyUnknownVariable == 'X'");
  }

  @Test
  void noConditionUsesTimingThenCategoryThenEmpty() {
    WrestlerAbility a = ability(null);
    assertThat(service.triggerText(a)).isEmpty();
    a.setCategory(AbilityCategory.ACTION);
    assertThat(service.triggerText(a)).isEqualTo("Action");
    a.setTiming(AbilityTiming.OFFENSE);
    assertThat(service.triggerText(a)).isEqualTo("On offense");
  }

  @Test
  void malformedConditionNeverThrows() {
    for (String bad :
        List.of(
            "event == 'DAMAGE_TAKEN' ||  && match.damageAmount >= 2",
            "(((",
            "&&",
            "event == ",
            "\u0000 weird control chars")) {
      WrestlerAbility a = ability(bad);
      assertThatCode(() -> service.triggerText(a)).doesNotThrowAnyException();
    }
  }

  // ── data-driven: every shipped event token must humanize ─────────────────

  @Test
  void everyShippedEventTokenHumanizes() throws IOException {
    Pattern eventPattern = Pattern.compile("event\\s*==\\s*'([A-Z_]+)'");
    List<String> unhandled = new ArrayList<>();
    for (String condition : shippedUnlockConditions()) {
      Matcher m = eventPattern.matcher(condition);
      while (m.find()) {
        String token = m.group(1);
        String text = service.triggerText(ability("event == '" + token + "'"));
        if (text.startsWith("event ==")) { // raw fallback ⇒ token missing from the table
          unhandled.add(token);
        }
      }
    }
    assertThat(unhandled)
        .as("Event tokens shipped in wrestlers/*.json missing from the reminder token table")
        .isEmpty();
  }

  private List<String> shippedUnlockConditions() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    List<String> conditions = new ArrayList<>();
    Path wrestlersDir = Path.of("src/main/resources/wrestlers");
    try (Stream<Path> files = Files.list(wrestlersDir)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
        collectConditions(mapper.readTree(Files.newInputStream(file)), conditions);
      }
    }
    try (InputStream universal =
        getClass().getResourceAsStream("/wrestler_abilities_universal.json")) {
      if (universal != null) {
        collectConditions(mapper.readTree(universal), conditions);
      }
    }
    assertThat(conditions).as("shipped unlock conditions should be found").isNotEmpty();
    return conditions;
  }

  private void collectConditions(final JsonNode node, final List<String> out) {
    if (node.isArray()) {
      node.forEach(n -> collectConditions(n, out));
      return;
    }
    JsonNode abilities = node.get("abilities");
    if (abilities != null && abilities.isArray()) {
      abilities.forEach(a -> collectConditions(a, out));
    }
    JsonNode condition = node.get("unlockCondition");
    if (condition != null && condition.isTextual() && !condition.asText().isBlank()) {
      out.add(condition.asText());
    }
  }

  // ── plainText: [[icon]] tokens become words, never dangling suffixes ──────

  @Test
  void plainTextReplacesIconTokensWithTheirNames() {
    assertThat(
            service.plainText(
                "Discard 2 [[card]]s (except [[finisher]]s) to block an attack, then gain the"
                    + " initiative."))
        .isEqualTo(
            "Discard 2 cards (except finishers) to block an attack, then gain the"
                + " initiative.");
  }

  @Test
  void plainTextHandlesNullAndBlank() {
    assertThat(service.plainText(null)).isEmpty();
    assertThat(service.plainText("   ")).isEmpty();
  }

  @Test
  void usageNoteLineKeepsIconTokenWordsInsteadOfDroppingThem() {
    WrestlerAbility a = ability(null);
    a.setName("Discard Block");
    a.setDescription("Discard 2 [[card]]s (except [[finisher]]s) to block. Second sentence.");
    assertThat(service.usageNoteLine(a, "Matt Cardona"))
        .isEqualTo(
            "[Ability] Matt Cardona: Discard Block — Discard 2 cards (except finishers) to"
                + " block.");
  }

  // ── usageNoteLine + countUses round-trip ──────────────────────────────────

  @Test
  void usageNoteLineFormatsWrestlerAbilityAndEffect() {
    WrestlerAbility a = ability(null);
    a.setName("Five Star Frog Splash");
    a.setDescription("Deal +2 damage on aerial attacks. Some second sentence.");
    assertThat(service.usageNoteLine(a, "RVD"))
        .isEqualTo("[Ability] RVD: Five Star Frog Splash — Deal +2 damage on aerial attacks.");
  }

  @Test
  void usageNoteLineWithoutDescriptionUsesTriggerText() {
    WrestlerAbility a = ability("event == 'TAUNT'");
    a.setName("Mind Games");
    assertThat(service.usageNoteLine(a, "Raven"))
        .isEqualTo("[Ability] Raven: Mind Games — When you taunt");
  }

  @Test
  void countUsesRoundTrips() {
    WrestlerAbility a = ability(null);
    a.setName("Power Kickout");
    a.setDescription("Reroll a kickout die.");
    String line = service.usageNoteLine(a, "Jordynne Grace");
    String notes = "Some player note\n" + line + "\nAnother note\n" + line;
    assertThat(service.countUses(notes, "Power Kickout", "Jordynne Grace")).isEqualTo(2);
    assertThat(service.countUses(notes, "Power Kickout", "Someone Else")).isZero();
    assertThat(service.countUses(notes, "Other Ability", "Jordynne Grace")).isZero();
  }

  @Test
  void countUsesDoesNotMatchAbilityNamePrefixes() {
    String notes = "[Ability] A: Power Kickout Deluxe — something";
    assertThat(service.countUses(notes, "Power Kickout", "A")).isZero();
  }

  @Test
  void countUsesToleratesNullAndEditedNotes() {
    assertThat(service.countUses(null, "X", "Y")).isZero();
    assertThat(service.countUses("free text the player wrote", "X", "Y")).isZero();
  }
}

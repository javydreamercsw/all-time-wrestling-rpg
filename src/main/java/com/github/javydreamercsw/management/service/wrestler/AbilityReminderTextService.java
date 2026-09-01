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

import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbility;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Turns ability metadata into human-readable, table-side reminder text.
 *
 * <p>Ability scripts ({@code effectScript}, {@code costScript}, {@code unlockCondition}) are design
 * metadata — they are never executed (product decision: the physical card game is played at the
 * table; the app reminds and records, it does not enforce rules). This service performs <b>static
 * string humanization only</b>: a bounded token table translates the event/qualifier vocabulary
 * observed in {@code wrestlers/*.json} into plain English, with a graceful fallback to the timing
 * label, category label, or the raw condition. It must never throw on arbitrary content.
 */
@Service
@Slf4j
public class AbilityReminderTextService {

  /** Marker prefix for machine-countable ability usage lines in {@code Segment.notes}. */
  public static final String USAGE_PREFIX = "[Ability] ";

  private static final Map<String, String> EVENT_PHRASES =
      Map.ofEntries(
          Map.entry("ATTACK_INCOMING", "When an attack is incoming"),
          Map.entry("ATTACK_SUCCESS", "After your attack succeeds"),
          Map.entry("TAUNT", "When you taunt"),
          Map.entry("ATTACK_PLAY", "When you play an attack card"),
          Map.entry("DAMAGE_TAKEN", "After taking damage"),
          Map.entry("KICKOUT_ROLL", "When rolling a kickout"),
          Map.entry("COMBO_SUCCESS", "After a successful combo"),
          Map.entry("MATCH_START", "At the start of the match"),
          Map.entry("AERIAL_ATTACK_SUCCESS", "After a successful aerial attack"),
          Map.entry("KICKOUT_ATTEMPT", "When attempting a kickout"),
          Map.entry("PIN_TRIGGERED", "When a pin is triggered"),
          Map.entry("GRAPPLE_SUCCESS", "After a successful grapple"),
          Map.entry("DAMAGE_DEALT", "After dealing damage"),
          Map.entry("STRIKE_SUCCESS", "After a successful strike"),
          Map.entry("THROW_SUCCESS", "After a successful throw"),
          Map.entry("OPPONENT_THROW_ATTEMPT", "When the opponent attempts a throw"),
          Map.entry("ATTACK_ROLL", "When rolling an attack"),
          Map.entry("KICKOUT", "On a kickout"),
          Map.entry("ATTACK_START", "When an attack starts"),
          Map.entry("RECOVER", "When you recover"),
          Map.entry("INITIATIVE_LOST", "When you lose initiative"),
          Map.entry("SUPLEX_SUCCESS", "After a successful suplex"),
          Map.entry(
              "OPPONENT_DEFENSIVE_WINDOW_ENDED", "After the opponent's defensive window ends"),
          Map.entry("HURT_LOCK_SUCCESS", "After a successful Hurt Lock"),
          Map.entry("SPEAR_SUCCESS", "After a successful spear"));

  private static final Pattern EVENT_ATOM = Pattern.compile("^event\\s*==\\s*'([A-Z_]+)'$");
  private static final Pattern CARD_TYPE_ATOM =
      Pattern.compile("^match\\.(?:attackCardType|attackType)\\s*==\\s*'([A-Z_]+)'$");
  private static final Pattern LAST_MOVE_ATOM =
      Pattern.compile("^match\\.lastMove\\s*==\\s*'([^']+)'$");
  private static final Pattern NUMERIC_ATOM =
      Pattern.compile(
          "^(match\\.attackCardBaseDamage|match\\.damageAmount|match\\.incomingDamage"
              + "|match\\.comboCategoriesUsed|match\\.consecutiveAttackCount"
              + "|wrestler\\.momentum)\\s*(>=|<=|==)\\s*(\\d+)$");
  private static final Pattern TOKEN_ATOM =
      Pattern.compile("^wrestler\\.abilityTokens\\['([^']+)'\\]\\s*(>=|<=|==)\\s*(\\d+)$");
  private static final Pattern NEGATED_FLAG_ATOM = Pattern.compile("^!match\\.is(\\w+)$");

  private static final Map<String, String> NUMERIC_SUBJECTS =
      Map.of(
          "match.attackCardBaseDamage", "the attack card's base damage",
          "match.damageAmount", "the damage",
          "match.incomingDamage", "the incoming damage",
          "match.comboCategoriesUsed", "combo categories used",
          "match.consecutiveAttackCount", "your consecutive attacks",
          "wrestler.momentum", "your momentum");

  /**
   * Human-readable trigger/when text for an ability. Fallback chain: humanized {@code
   * unlockCondition} → timing label → category label → raw condition text. Never throws.
   */
  public String triggerText(@NonNull final WrestlerAbility ability) {
    String condition = ability.getUnlockCondition();
    if (condition != null && !condition.isBlank()) {
      try {
        String humanized = humanizeCondition(condition.trim());
        if (humanized != null) {
          return humanized;
        }
      } catch (RuntimeException e) {
        log.debug("Could not humanize unlock condition '{}': {}", condition, e.getMessage());
      }
    }
    if (ability.getTiming() != null) {
      return switch (ability.getTiming()) {
        case OFFENSE -> "On offense";
        case DEFENSE -> "On defense";
        case PINNED -> "While pinned";
        case BACKSTAGE -> "Backstage";
      };
    }
    if (ability.getCategory() != null) {
      return switch (ability.getCategory()) {
        case SIGNATURE -> "Signature ability";
        case PASSIVE -> "Passive — always on";
        case ACTION -> "Action";
      };
    }
    return condition != null ? condition : "";
  }

  /**
   * Machine-countable usage note line appended to match notes when an ability is marked used:
   * {@code [Ability] <wrestler>: <name> — <first sentence of description>.}
   */
  public String usageNoteLine(
      @NonNull final WrestlerAbility ability, @NonNull final String wrestlerName) {
    StringBuilder sb =
        new StringBuilder(USAGE_PREFIX).append(wrestlerName).append(": ").append(ability.getName());
    String effect = firstSentence(ability.getDescription());
    if (effect.isBlank()) {
      effect = triggerText(ability);
    }
    if (!effect.isBlank()) {
      sb.append(" — ").append(effect);
    }
    return sb.toString();
  }

  /**
   * Counts how many usage lines for the given ability/wrestler exist in the notes text. Used to
   * seed advisory uses-left counters after a reload. Tolerates arbitrary player edits: anything
   * that does not match the usage line prefix simply does not count.
   */
  public int countUses(
      @Nullable final String notes,
      @NonNull final String abilityName,
      @NonNull final String wrestlerName) {
    if (notes == null || notes.isBlank()) {
      return 0;
    }
    String prefix = USAGE_PREFIX + wrestlerName + ": " + abilityName;
    int count = 0;
    for (String line : notes.split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.startsWith(prefix)) {
        String rest = trimmed.substring(prefix.length());
        // Exact ability-name boundary: end of line or the effect separator.
        if (rest.isEmpty() || rest.startsWith(" —") || rest.startsWith(" -")) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Translates a flat boolean condition into English. Returns null when any atom is outside the
   * known vocabulary so callers fall back rather than show half-translated text.
   */
  @Nullable private String humanizeCondition(final String condition) {
    // Split on top-level boolean operators, keeping them as separators. The shipped
    // conditions are flat (no parentheses); anything unexpected falls back via null.
    if (condition.contains("(") || condition.contains(")")) {
      return null;
    }
    String[] parts = condition.split("(?=&&)|(?=\\|\\|)");
    StringBuilder sb = new StringBuilder();
    for (String rawPart : parts) {
      String part = rawPart.trim();
      String joiner = "";
      if (part.startsWith("&&")) {
        joiner = " and ";
        part = part.substring(2).trim();
      } else if (part.startsWith("||")) {
        joiner = " or ";
        part = part.substring(2).trim();
      }
      if (part.isEmpty()) {
        return null; // malformed content — fall back to raw text
      }
      String atom = humanizeAtom(part);
      if (atom == null) {
        return null;
      }
      // The leading event phrase keeps its capitalization; qualifiers are lower-case clauses.
      sb.append(joiner).append(sb.isEmpty() ? atom : atom);
    }
    return sb.toString();
  }

  @Nullable private String humanizeAtom(final String atom) {
    Matcher m = EVENT_ATOM.matcher(atom);
    if (m.matches()) {
      return EVENT_PHRASES.get(m.group(1));
    }
    m = CARD_TYPE_ATOM.matcher(atom);
    if (m.matches()) {
      String type = m.group(1).toLowerCase().replace('_', ' ');
      return ("aeiou".indexOf(type.charAt(0)) >= 0 ? "the attack is an " : "the attack is a ")
          + type;
    }
    m = LAST_MOVE_ATOM.matcher(atom);
    if (m.matches()) {
      return "the last move was " + m.group(1);
    }
    m = NUMERIC_ATOM.matcher(atom);
    if (m.matches()) {
      String subject = NUMERIC_SUBJECTS.get(m.group(1));
      if (subject == null) {
        return null;
      }
      return subject + comparisonText(m.group(2), m.group(3));
    }
    m = TOKEN_ATOM.matcher(atom);
    if (m.matches()) {
      return "you have"
          + comparisonText(m.group(2), m.group(3)).replace(" is", "")
          + " "
          + m.group(1)
          + " tokens";
    }
    m = NEGATED_FLAG_ATOM.matcher(atom);
    if (m.matches()) {
      return "not " + camelToWords(m.group(1));
    }
    return null;
  }

  private String comparisonText(final String operator, final String value) {
    return switch (operator) {
      case ">=" -> " is " + value + " or more";
      case "<=" -> " is " + value + " or less";
      default -> " is exactly " + value;
    };
  }

  private String camelToWords(final String camel) {
    return camel.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
  }

  private String firstSentence(@Nullable final String description) {
    String plain = plainText(description);
    int end = plain.indexOf(". ");
    return end > 0 ? plain.substring(0, end + 1) : plain;
  }

  /**
   * Converts text containing {@code [[icon]]} placeholders (rendered as inline SVGs by {@code
   * GuideTextRenderer}) into readable plain text for tooltip/log contexts, replacing each token
   * with its name: {@code "Discard 2 [[card]]s"} becomes {@code "Discard 2 cards"}. Deleting the
   * tokens instead would leave dangling plural suffixes ("Discard 2 s").
   */
  public String plainText(@Nullable final String description) {
    if (description == null || description.isBlank()) {
      return "";
    }
    return description.replaceAll("\\[\\[([^]]*)]]", "$1").replaceAll("\\s+", " ").trim();
  }
}

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
package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Context object exposed to Groovy scripts in Titles. Provides methods to manipulate segment
 * context and match flow.
 *
 * <p>Two activation paths:
 *
 * <ul>
 *   <li><b>Narration-time</b> — constructed with just {@code segmentContext} and {@code champion};
 *       use {@link #gainInitiative()}, {@link #gainHitPoints(int)}, {@link #modifyRoll(int)}.
 *   <li><b>Match-time</b> — constructed with the additional {@code segment} and {@code
 *       segmentRuleRepository}; use {@link #addMatchRule(String)} and {@link #setupWeaponCard()}.
 * </ul>
 */
@Slf4j
public class TitleEffectContext {

  private final SegmentNarrationContext segmentContext;
  private final WrestlerContext champion;
  private final Segment segment;
  private final SegmentRuleRepository segmentRuleRepository;

  @Getter private boolean weaponCardSetupNeeded = false;

  /** Narration-time constructor — no live segment access. */
  public TitleEffectContext(
      final SegmentNarrationContext segmentContext, final WrestlerContext champion) {
    this(segmentContext, champion, null, null);
  }

  /** Match-time constructor — enables {@link #addMatchRule} and {@link #setupWeaponCard}. */
  public TitleEffectContext(
      final SegmentNarrationContext segmentContext,
      final WrestlerContext champion,
      final Segment segment,
      final SegmentRuleRepository segmentRuleRepository) {
    this.segmentContext = segmentContext;
    this.champion = champion;
    this.segment = segment;
    this.segmentRuleRepository = segmentRuleRepository;
  }

  // ── Narration-time methods ──────────────────────────────────────────────

  public void gainInitiative() {
    String instr = segmentContext.getInstructions();
    if (instr == null) {
      instr = "";
    }
    instr +=
        "\n\nIMPORTANT: The champion, "
            + champion.getName()
            + ", has the initiative at the start of the match.";
    segmentContext.setInstructions(instr.trim());
    log.info("[Title Script] {} gained initiative", champion.getName());
  }

  public void gainHitPoints(final int amount) {
    champion.setHealth(champion.getHealth() + amount);

    String instr = segmentContext.getInstructions();
    if (instr == null) {
      instr = "";
    }
    instr +=
        "\n\nNOTE: "
            + champion.getName()
            + " starts the match with extra physical endurance (+ "
            + amount
            + " HP) due to being the champion.";
    segmentContext.setInstructions(instr.trim());
    log.info("[Title Script] {} gained {} HP", champion.getName(), amount);
  }

  public void modifyRoll(final int modifier) {
    String instr = segmentContext.getInstructions();
    if (instr == null) {
      instr = "";
    }
    instr +=
        "\n\nNOTE: "
            + champion.getName()
            + " has a championship advantage—they have a one-time bonus (+ "
            + modifier
            + ") to a critical roll during the match.";
    segmentContext.setInstructions(instr.trim());
    log.info("[Title Script] {} granted roll bonus: {}", champion.getName(), modifier);
  }

  // ── Match-time methods ──────────────────────────────────────────────────

  /**
   * Adds a named rule to the live segment. Only available in match-time activation path.
   *
   * @param ruleName the name of the {@code SegmentRule} to inject (e.g. {@code "No DQ"})
   */
  public void addMatchRule(final String ruleName) {
    if (segment == null || segmentRuleRepository == null) {
      log.warn(
          "[Title Script] addMatchRule('{}') called outside match-time context — ignored",
          ruleName);
      return;
    }
    segmentRuleRepository
        .findByName(ruleName)
        .ifPresentOrElse(
            rule -> {
              segment.addSegmentRule(rule);
              log.info("[Title Script] Added rule '{}' to segment {}", ruleName, segment.getId());
            },
            () -> log.warn("[Title Script] SegmentRule '{}' not found — skipping", ruleName));
  }

  /**
   * Signals that a weapon card must be set up from the physical display. The caller reads {@link
   * #isWeaponCardSetupNeeded()} and prompts the user accordingly. Only meaningful in match-time
   * activation path.
   */
  public void setupWeaponCard() {
    this.weaponCardSetupNeeded = true;
    log.info("[Title Script] Weapon card setup requested by {}", champion.getName());
  }
}

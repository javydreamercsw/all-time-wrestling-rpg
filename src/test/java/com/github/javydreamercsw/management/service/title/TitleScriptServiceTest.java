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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TitleScriptServiceTest {

  private TitleScriptService titleScriptService;
  private SegmentNarrationContext context;
  private WrestlerContext championContext;
  private Title title;
  private Wrestler champion;

  @BeforeEach
  public void setUp() {
    titleScriptService = new TitleScriptService(mock(SegmentRuleRepository.class));
    context = new SegmentNarrationContext();
    championContext = new WrestlerContext();
    championContext.setName("John Cena");
    championContext.setHealth(10);
    context.setWrestlers(List.of(championContext));

    champion = new Wrestler();
    champion.setName("John Cena");

    title = new Title();
    title.setName("World Championship");
    title.getChampion().add(champion);
  }

  @Test
  void testGainInitiativeEffect() {
    title.setEffectScript("gainInitiative()");
    titleScriptService.applyTitleEffects(context, Collections.singletonList(title));

    assertTrue(
        context.getInstructions().contains("initiative at the start of the match"),
        "Instructions should contain initiative info");
  }

  @Test
  void testGainHitPointsEffect() {
    title.setEffectScript("gainHitPoints(5)");
    titleScriptService.applyTitleEffects(context, Collections.singletonList(title));

    assertEquals(15, championContext.getHealth(), "Health should be increased by 5");
    assertTrue(
        context.getInstructions().contains("extra physical endurance (+ 5 HP)"),
        "Instructions should contain HP info");
  }

  @Test
  void testModifyRollEffect() {
    title.setEffectScript("modifyRoll(2)");
    titleScriptService.applyTitleEffects(context, Collections.singletonList(title));

    assertTrue(
        context.getInstructions().contains("one-time bonus (+ 2)"),
        "Instructions should contain roll bonus info");
  }

  @Test
  void testAddMatchRuleInjectsRuleIntoSegment() {
    SegmentRule noDqRule = new SegmentRule();
    noDqRule.setName("No DQ");

    SegmentRuleRepository ruleRepo = mock(SegmentRuleRepository.class);
    when(ruleRepo.findByName("No DQ")).thenReturn(Optional.of(noDqRule));
    TitleScriptService service = new TitleScriptService(ruleRepo);

    Segment segment = new Segment();
    segment.setSegmentRules(new HashSet<>());

    title.setEffectScript("addMatchRule('No DQ')");
    title.getChampion().clear();
    title.getChampion().add(champion);

    WrestlerContext wc = new WrestlerContext();
    wc.setName("John Cena");

    service.activateTitleSkill(title, segment, wc);

    verify(ruleRepo).findByName("No DQ");
    assertTrue(
        segment.getSegmentRules().contains(noDqRule), "No DQ rule should be added to segment");
  }

  @Test
  void testSetupWeaponCardReturnsTrueInResult() {
    SegmentRuleRepository ruleRepo = mock(SegmentRuleRepository.class);
    TitleScriptService service = new TitleScriptService(ruleRepo);

    Segment segment = new Segment();
    segment.setSegmentRules(new HashSet<>());

    title.setEffectScript("setupWeaponCard()");
    title.getChampion().clear();
    title.getChampion().add(champion);

    WrestlerContext wc = new WrestlerContext();
    wc.setName("John Cena");

    var result = service.activateTitleSkill(title, segment, wc);

    assertTrue(result.weaponCardSetupNeeded(), "Weapon card flag should be true");
  }

  @Test
  void testAddMatchRuleOutsideMatchTimeContextLogsWarning() {
    // When addMatchRule is called via applyTitleEffects (narration-time, no segment), it should
    // not throw — the method logs a warning and returns silently.
    SegmentRuleRepository ruleRepo = mock(SegmentRuleRepository.class);
    TitleScriptService service = new TitleScriptService(ruleRepo);

    title.setEffectScript("addMatchRule('No DQ')");
    SegmentNarrationContext ctx = new SegmentNarrationContext();
    WrestlerContext wc = new WrestlerContext();
    wc.setName("John Cena");
    ctx.setWrestlers(List.of(wc));

    service.applyTitleEffects(ctx, Collections.singletonList(title));

    assertFalse(
        ruleRepo.findByName("No DQ").isPresent(),
        "Repository should not be queried outside match-time path");
  }
}

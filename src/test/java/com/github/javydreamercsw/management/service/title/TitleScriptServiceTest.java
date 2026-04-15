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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TitleScriptServiceTest {

  private TitleScriptService titleScriptService;
  private SegmentNarrationContext context;
  private WrestlerContext championContext;
  private Title title;
  private Wrestler champion;

  @BeforeEach
  void setUp() {
    titleScriptService = new TitleScriptService();
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
}

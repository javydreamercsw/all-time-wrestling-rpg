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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

class FeudScriptDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private RivalryService rivalryService;
  @Autowired private WrestlerRepository wrestlerRepository;

  /**
   * The docs data initializer does not seed rivalries, so each capture creates (or reuses) one
   * deterministically instead of depending on grid content.
   */
  private Rivalry ensureRivalry() {
    List<Rivalry> active = rivalryService.getActiveRivalries();
    if (!active.isEmpty()) {
      return active.get(0);
    }
    List<Wrestler> wrestlers = wrestlerRepository.findAll();
    Wrestler wrestler1 =
        wrestlers.isEmpty()
            ? wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Feud Wrestler 1"))
            : wrestlers.get(0);
    Wrestler wrestler2 =
        wrestlers.size() > 1
            ? wrestlers.get(1)
            : wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Feud Wrestler 2"));
    Rivalry rivalry =
        rivalryService
            .createRivalry(
                wrestler1.getId(), wrestler2.getId(), "A grudge that has boiled over for months.")
            .orElseThrow();
    rivalryService.addHeat(rivalry.getId(), 40, "Docs seed heat");
    return rivalry;
  }

  @Test
  void captureRivalryListView() {
    ensureRivalry();
    navigateTo("rivalry-list");
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    documentFeature(
        "Booker",
        "Rivalry List",
        "Browse all active rivalries between wrestlers. Click any row to open the rivalry detail"
            + " view where you can manage story arc scripts.",
        "booker-rivalry-list");
  }

  @Test
  void captureRivalryDetailView() {
    Rivalry rivalry = ensureRivalry();
    navigateTo("rivalry/" + rivalry.getId());

    waitForVaadinElement(driver, By.xpath("//h2[contains(text(),' vs ')]"));
    documentFeature(
        "Booker",
        "Rivalry Detail",
        "View rivalry details including heat, storyline notes, and dates. Use the Story Arc button"
            + " to script a multi-beat feud arc for this rivalry.",
        "booker-rivalry-detail");
  }

  @Test
  void captureStoryArcWizard() {
    Rivalry rivalry = ensureRivalry();
    navigateTo("rivalry/" + rivalry.getId());

    waitForVaadinElement(driver, By.xpath("//vaadin-button[normalize-space()='Story Arc']"));
    clickElement(By.xpath("//vaadin-button[normalize-space()='Story Arc']"));

    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 1')]"));
    documentFeature(
        "Booker",
        "Story Arc Wizard",
        "Three-step wizard for scripting a feud arc: select wrestlers, name the arc and choose"
            + " its length, then add ordered beats (match type, stipulation, winner control).",
        "booker-story-arc-wizard");
  }
}

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
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

class FeudScriptDocsE2ETest extends AbstractDocsE2ETest {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  @Autowired private RivalryService rivalryService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FeudScriptService feudScriptService;

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

  /**
   * Creates a rivalry that no other test has touched, so arc-creating captures see exactly the arcs
   * they create themselves instead of whatever earlier tests left on the shared rivalry.
   */
  private Rivalry ensureFreshRivalry(String label) {
    int n = COUNTER.incrementAndGet();
    Wrestler wrestler1 =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler(label + " Wrestler " + n + "A"));
    Wrestler wrestler2 =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler(label + " Wrestler " + n + "B"));
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

  @Test
  void captureStoryArcWizardStep2() {
    Rivalry rivalry = ensureRivalry();
    navigateTo("rivalry/" + rivalry.getId());

    waitForVaadinElement(driver, By.xpath("//vaadin-button[normalize-space()='Story Arc']"));
    clickElement(By.xpath("//vaadin-button[normalize-space()='Story Arc']"));

    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 1')]"));
    // Step 1 pre-selects the rivalry's two wrestlers; advance to step 2.
    clickElement(By.xpath("//vaadin-button[normalize-space()='Next']"));

    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 2')]"));
    documentFeature(
        "Booker",
        "Story Arc Details",
        "Step 2 names the arc (pre-filled from the selected wrestlers) and picks its length:"
            + " Short spans 1 PLE, Medium 2, Long 3. The PLE count is a hard ceiling — once a"
            + " feud has appeared on that many premium live events, it must culminate.",
        "booker-story-arc-details");
  }

  @Test
  void captureStoryArcWizardStep3() {
    Rivalry rivalry = ensureRivalry();
    navigateTo("rivalry/" + rivalry.getId());

    waitForVaadinElement(driver, By.xpath("//vaadin-button[normalize-space()='Story Arc']"));
    clickElement(By.xpath("//vaadin-button[normalize-space()='Story Arc']"));

    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 1')]"));
    clickElement(By.xpath("//vaadin-button[normalize-space()='Next']"));
    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 2')]"));
    clickElement(By.xpath("//vaadin-button[normalize-space()='Next']"));

    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 3')]"));
    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Winner')]"));
    documentFeature(
        "Booker",
        "Story Arc Beats",
        "Step 3 defines the match sequence. Each beat picks a match type and optional"
            + " stipulation, and decides who controls the outcome: the booker names the planned"
            + " winner, the AI picks based on story notes, or the system rolls. Mark a beat as"
            + " the Culmination / Blowoff to close the feud. Roster wrestlers outside the arc can"
            + " join a beat as an external opponent or run-in extra.",
        "booker-story-arc-beats");
  }

  @Test
  void captureStoryArcCard() {
    Rivalry rivalry = ensureFreshRivalry("Showcase");

    FeudScript script =
        feudScriptService.createFromWizard(
            "Docs Showcase Arc", List.of(rivalry.getWrestler1(), rivalry.getWrestler2()), 3);
    FeudScriptBeat opener = new FeudScriptBeat();
    opener.setSegmentType("One on One");
    opener.setSegmentRule("Ladder Match");
    opener.setNotes("The rivalry boils over — both want the briefcase as leverage.");
    feudScriptService.addBeat(script, opener);
    FeudScriptBeat blowoff = new FeudScriptBeat();
    blowoff.setSegmentType("One on One");
    blowoff.setSegmentRule("Cage");
    blowoff.setWinnerControl(FeudScriptWinnerControl.BOOKER_PICKS);
    blowoff.setPlannedWinner(rivalry.getWrestler1());
    blowoff.setCulmination(true);
    blowoff.setNotes("Career vs. career — the loser leaves the promotion.");
    feudScriptService.addBeat(script, blowoff);

    navigateTo("rivalry/" + rivalry.getId());

    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Story Arcs')]"));
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    documentFeature(
        "Booker",
        "Story Arc Management",
        "Every saved arc lives on the rivalry detail as a card with a status badge (Active,"
            + " Completed, Cancelled). The beat grid lists each beat in order — match type,"
            + " stipulation, external participants, winner control, blowoff marker and status."
            + " Pending beats can be edited or removed and renumber automatically; the arc"
            + " itself can be renamed, extended with more beats, or cancelled before its"
            + " culmination.",
        "booker-story-arc-card");
  }

  @Test
  void captureEditBeatDialog() {
    Rivalry rivalry = ensureFreshRivalry("Edit-Beat");

    FeudScript script =
        feudScriptService.createFromWizard(
            "Docs Edit-Beat Arc", List.of(rivalry.getWrestler1(), rivalry.getWrestler2()), 2);
    FeudScriptBeat opener = new FeudScriptBeat();
    opener.setSegmentType("One on One");
    opener.setSegmentRule("Submission");
    opener.setNotes("Originally planned as a technical showcase.");
    feudScriptService.addBeat(script, opener);

    navigateTo("rivalry/" + rivalry.getId());

    waitForVaadinElement(driver, By.xpath("//vaadin-button[normalize-space()='✎']"));
    clickElement(By.xpath("//vaadin-button[normalize-space()='✎']"));

    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Edit Beat #1 — Docs Edit-Beat Arc')]"));
    documentFeature(
        "Booker",
        "Edit Story Arc Beat",
        "Pending beats can be retooled at any time before they play out. The edit dialog loads"
            + " the beat's current match type, stipulation, winner control, planned winner,"
            + " blowoff flag, notes and external participants — saving re-runs the same"
            + " validation as creation and keeps any show or PLE reservation attached to the"
            + " beat.",
        "booker-story-arc-edit-beat");
  }

  @Test
  void captureAddBeatToArc() {
    Rivalry rivalry = ensureFreshRivalry("Add-Beat");

    FeudScript script =
        feudScriptService.createFromWizard(
            "Docs Add-Beat Arc", List.of(rivalry.getWrestler1(), rivalry.getWrestler2()), 2);
    FeudScriptBeat opener = new FeudScriptBeat();
    opener.setSegmentType("Singles Match");
    feudScriptService.addBeat(script, opener);

    navigateTo("rivalry/" + rivalry.getId());

    waitForVaadinElement(driver, By.xpath("//vaadin-button[normalize-space()='+ Add Beat']"));
    clickElement(By.xpath("//vaadin-button[normalize-space()='+ Add Beat']"));

    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Add Beat — Docs Add-Beat Arc')]"));
    documentFeature(
        "Booker",
        "Add Beat to Story Arc",
        "Append new beats to an existing story arc after it has been saved. The Add Beat dialog"
            + " offers the same match type, stipulation, winner control and notes fields as the"
            + " creation wizard, plus external participants — an opponent and run-in extras from"
            + " outside the feud, with an AI-assisted opponent suggestion.",
        "booker-story-arc-add-beat");
  }
}

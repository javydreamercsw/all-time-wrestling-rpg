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
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Video walkthrough of the story arc workflow: opening the wizard from the rivalry detail view,
 * stepping through wrestler selection, arc details, and beat planning, then reviewing and editing
 * the saved arc on the rivalry card.
 */
@Tag("video")
class StoryArcVideoDocsE2ETest extends AbstractDocsE2ETest {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  @Autowired private RivalryService rivalryService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FeudScriptService feudScriptService;

  @Test
  void recordStoryArcWorkflow() {
    setVideoInfo("Booker", "Story Arc Scripting", "booker-story-arc-workflow");

    Rivalry rivalry = ensureRivalry("Story Arc");

    navigateTo("rivalry/" + rivalry.getId());
    waitForVaadinElement(driver, By.xpath("//h2[contains(text(),' vs ')]"));
    captureCaption(
        "Story arcs are scripted feud plans attached to a rivalry. From the rivalry detail"
            + " view, the Story Arc button opens a three-step wizard.",
        3500);

    // ── Step 1: wrestlers ─────────────────────────────────────────────────
    waitForVaadinElement(driver, By.xpath("//vaadin-button[normalize-space()='Story Arc']"));
    clickElement(By.xpath("//vaadin-button[normalize-space()='Story Arc']"));
    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 1')]"));
    captureCaption(
        "Step 1 selects the feud's wrestlers — two for a rivalry arc, three or more for a"
            + " multi-wrestler feud. The rivalry's two wrestlers come pre-selected.",
        3500);

    clickElement(By.xpath("//vaadin-button[normalize-space()='Next']"));
    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 2')]"));
    // ── Step 2: arc details ───────────────────────────────────────────────
    captureCaption(
        "Step 2 names the arc and sets its length. Short feuds span one premium live event,"
            + " Medium two, and Long three — that PLE count is a hard ceiling, so a feud"
            + " must culminate before running out of big-show appearances.",
        4000);

    clickElement(By.xpath("//vaadin-button[normalize-space()='Next']"));
    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Step 3')]"));
    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Winner')]"));
    // ── Step 3: beats ─────────────────────────────────────────────────────
    captureCaption(
        "Step 3 plans the beats — the ordered match sequence of the arc. Each beat picks a"
            + " match type and optional stipulation, and decides who controls the outcome:"
            + " the booker, the AI, or a system roll. Roster wrestlers outside the feud can"
            + " join as external opponents or run-in extras.",
        4500);

    // Fill the beat row so the wizard can be completed on screen.
    fillBeatRow();
    clickElement(By.xpath("//vaadin-button[normalize-space()='Finish']"));
    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Story Arcs')]"));

    // ── Saved arc card ────────────────────────────────────────────────────
    captureCaption(
        "Finishing the wizard saves the arc back on the rivalry detail view. Each arc card"
            + " shows its status badge and the full beat grid — order, match type, stipulation,"
            + " winner control, and blowoff markers. Pending beats can be edited or removed,"
            + " and the arc can be extended at any time with the Add Beat button.",
        4500);

    // ── Edit a beat ───────────────────────────────────────────────────────
    waitForVaadinElement(driver, By.xpath("//vaadin-button[normalize-space()='✎']"));
    clickElement(By.xpath("//vaadin-button[normalize-space()='✎']"));
    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Edit Beat #1')]"));
    captureCaption(
        "The edit dialog loads a pending beat with all of its current values. Bookers routinely"
            + " retool beats here as the story evolves — changed match types, new stipulations,"
            + " or a different planned winner. Saving re-runs the same validation as creation.",
        4000);

    clickElement(By.xpath("//vaadin-button[normalize-space()='Cancel']"));
    waitForVaadinElement(driver, By.xpath("//*[contains(.,'Story Arcs')]"));
    captureCaption(
        "Back on the arc card, beats remain editable until they play out on a show. When a"
            + " scripted segment completes, its beat is marked completed automatically — once"
            + " every beat is done, the arc completes itself and the feud is over.",
        4000);
  }

  /**
   * Creates a dedicated rivalry so the walkthrough only sees the arc it creates itself, whatever
   * other tests have left in the database.
   */
  private Rivalry ensureRivalry(String label) {
    int n = COUNTER.incrementAndGet();
    Wrestler wrestler1 =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler(label + " Wrestler " + n + "A"));
    Wrestler wrestler2 =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler(label + " Wrestler " + n + "B"));
    Rivalry rivalry =
        rivalryService
            .createRivalry(
                wrestler1.getId(), wrestler2.getId(), "A scripted grudge months in the making.")
            .orElseThrow();
    rivalryService.addHeat(rivalry.getId(), 40, "Story arc video seed heat");
    return rivalry;
  }

  /** Fills the wizard's first beat row so the arc can be completed on camera. */
  private void fillBeatRow() {
    // Vaadin 25 renders the wizard as vaadin-dialog (no vaadin-dialog-overlay in the light DOM).
    // The beat editor's Match Type combo is identified by its host label property.
    List<WebElement> combos = driver.findElements(By.tagName("vaadin-combo-box"));
    WebElement matchTypeCombo =
        combos.stream()
            .filter(
                combo -> "Match Type".equals(combo.getDomProperty("label")) && combo.isDisplayed())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Match Type combo not found in wizard"));
    selectFromVaadinComboBox(matchTypeCombo, "One on One");
  }
}

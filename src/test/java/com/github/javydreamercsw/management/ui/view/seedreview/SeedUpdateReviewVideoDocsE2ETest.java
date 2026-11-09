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
package com.github.javydreamercsw.management.ui.view.seedreview;

import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecision;
import com.github.javydreamercsw.management.domain.seedreview.SeedUpdateDecisionRepository;
import com.github.javydreamercsw.management.sync.review.SeedReviewGate;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Video walkthrough of the seed-update review workflow (seed-update review feature, Task 11): the
 * freeze gate intercepts navigation, the admin reviews the diff-approval tree, decides per field,
 * applies, and the app unlocks.
 *
 * <p>{@link SeedReviewVideoAppliers} is {@code @Import}-ed (a nested {@link TestConfiguration} is
 * NOT auto-detected because the E2E base pins {@code @SpringBootTest(classes = Application.class)})
 * and registers no-op appliers for the domains the wiring PR has not covered yet (the wrestler
 * domain uses the REAL applier that shipped there); the video demonstrates the
 * decision/apply/unlock flow, which is the part a static screenshot cannot convey.
 */
@Tag("video")
@Import(SeedUpdateReviewVideoDocsE2ETest.SeedReviewVideoAppliers.class)
class SeedUpdateReviewVideoDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private SeedUpdateDecisionRepository decisionRepository;
  @Autowired private SeedReviewGate gate;

  @TestConfiguration
  static class SeedReviewVideoAppliers {

    // No no-op appliers here anymore: PR 4 wired every content domain, so the real appliers for
    // wrestler, npc, and deck all exist — a second bean for any of those domain keys collides at
    // context startup (domain keys are unique). The real appliers are harmless in this throwaway
    // DB: the video's decisions stamp or write throwaway rows the scenario never asserts on.

  }

  @BeforeEach
  void seedPendingDecisions() {
    decisionRepository.deleteAllInBatch();
    decisionRepository.saveAndFlush(
        conflict(
            "wrestler",
            "wrestler:bret-hart",
            "Bret Hart",
            "catchphrase",
            "The Excellence of Execution",
            "The Best There Is",
            "fp-video-1"));
    decisionRepository.saveAndFlush(
        conflict(
            "wrestler",
            "wrestler:bret-hart",
            "Bret Hart",
            "theme music",
            "Barbed Wire 4 Life",
            "Hits from the Bong",
            "fp-video-2"));
    decisionRepository.saveAndFlush(
        conflict(
            "npc",
            "npc:samael",
            "Samael",
            "motive",
            "Harvest souls",
            "Devour souls",
            "fp-video-3"));
    decisionRepository.saveAndFlush(
        removal("deck", "deck:legend-deck", "Legend Deck", "fp-video-4"));
    gate.refresh();
  }

  @AfterEach
  void clearPendingDecisions() {
    decisionRepository.deleteAllInBatch();
    gate.refresh();
  }

  @Test
  void recordSeedReviewWorkflow() {
    setVideoInfo("Admin", "Seed Update Review", "admin-seed-update-review-workflow");

    // 1. Gate intercept: the freeze reroutes the home page to the waiting panel.
    navigateToAndWaitForElement("", By.id("gate-waiting-panel"));
    captureCaption(
        "After upgrading to a new release, the app pauses before gameplay: seed data that"
            + " changed in the release collided with decisions you made in your universe."
            + " Your data is safe — nothing has been changed yet. An administrator clicks"
            + " 'Review now' to resolve the collisions.",
        5000);

    // 2. Review tree overview: domains, entities, and the summary bar.
    clickButtonByText("Review now");
    waitForVaadinElement(driver, By.id("seed-review-summary"));
    captureCaption(
        "The review screen groups every collision by domain and entity. Each field row"
            + " shows the current value on the left and the incoming seed value on the"
            + " right. Entities removed from the source data get their own removal row."
            + " Nothing is applied until you press the single Apply button at the bottom.",
        5500);

    // 3. Expand the entity to expose its per-field rows (entity sections start collapsed).
    clickElement(
        By.xpath(
            "//vaadin-details[@id='entity-section-wrestler-bret-hart']"
                + "/vaadin-details-summary"));
    waitForVaadinElement(driver, By.id("decision-wrestler-wrestler-bret-hart-catchphrase"));
    captureCaption(
        "Expand an entity to see its individual fields. Here Bret Hart's catchphrase and"
            + " theme music both changed upstream. You can keep your version of each field"
            + " ('Keep mine'), adopt the release's new value ('Use new'), or sweep the whole"
            + " entity with the buttons on the right.",
        5000);

    // 4. Per-field decisions: keep the catchphrase, take the new theme music, take Samael's
    // new motive, and keep the removed deck as custom data.
    selectRadio("decision-wrestler-wrestler-bret-hart-catchphrase", "Keep mine");
    selectRadio("decision-wrestler-wrestler-bret-hart-theme-music", "Use new");
    selectRadio("decision-npc-npc-samael-motive", "Use new");
    selectRadio("decision-deck-deck-legend-deck-removal", "Keep as custom");
    captureCaption(
        "Decide each row: 'Keep mine' preserves your customization and stamps it as"
            + " reviewed, 'Use new' adopts the release value, and a removed entity can be"
            + " kept as custom data if your universe still uses it. Removals always need an"
            + " explicit per-entity choice — no bulk button can remove your data. Once every"
            + " row is decided, Apply unlocks.",
        5500);

    // 5. Apply + unlock: the batch is written, the gate lowers, and the app navigates home.
    clickButtonByText("Apply decisions & unlock the app");
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Welcome back,')]"));
    captureCaption(
        "Apply writes the whole batch in one transaction and the review freeze lifts"
            + " immediately — the app is back to normal. Kept fields re-collide only if a"
            + " future release changes them again, so every upgrade lands as a short,"
            + " focused review instead of silent overwrites.",
        5500);
  }

  /**
   * Selects a vaadin-radio-button inside the group with the given id (labels live in the shadow DOM
   * — same technique as the tournament wizard docs test).
   */
  private void selectRadio(String groupId, String optionLabel) {
    waitForVaadinClientToLoad();
    String script =
        """
        const group = document.getElementById(arguments[0]);
        if (!group) throw new Error('Radio group not found: ' + arguments[0]);
        const radios = Array.from(group.querySelectorAll('vaadin-radio-button'));
        const target = radios.find(r => {
          const slot = r.shadowRoot && r.shadowRoot.querySelector('slot');
          const nodes = slot ? slot.assignedNodes() : [];
          const text = nodes.map(n => n.textContent).join('').trim();
          if (text) return text === arguments[1];
          return (r.innerText || r.textContent || '').trim() === arguments[1];
        });
        if (!target) {
          throw new Error('Radio option not found: ' + arguments[1]
            + ' in ' + radios.length + ' radios');
        }
        target.click();
        """;
    ((JavascriptExecutor) driver).executeScript(script, groupId, optionLabel);
    waitForVaadinClientToLoad();
  }

  private SeedUpdateDecision conflict(
      String domainKey,
      String entityKey,
      String displayName,
      String fieldName,
      String currentValue,
      String newValue,
      String fingerprint) {
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setDomainKey(domainKey);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(displayName);
    decision.setFieldName(fieldName);
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_FIELD_CONFLICT);
    decision.setCurrentValue(currentValue);
    decision.setNewValue(newValue);
    decision.setNewFingerprint(fingerprint);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion("2.13.0");
    return decision;
  }

  private SeedUpdateDecision removal(
      String domainKey, String entityKey, String displayName, String fingerprint) {
    SeedUpdateDecision decision = new SeedUpdateDecision();
    decision.setDomainKey(domainKey);
    decision.setEntityKey(entityKey);
    decision.setEntityDisplayName(displayName);
    decision.setFieldName(null); // NULL for removal decisions
    decision.setDecisionType(SeedUpdateDecision.DECISION_TYPE_REMOVAL);
    decision.setCurrentValue(null);
    decision.setNewValue(null);
    decision.setNewFingerprint(fingerprint);
    decision.setStatus(SeedUpdateDecision.STATUS_PENDING);
    decision.setAppVersion("2.13.0");
    return decision;
  }
}

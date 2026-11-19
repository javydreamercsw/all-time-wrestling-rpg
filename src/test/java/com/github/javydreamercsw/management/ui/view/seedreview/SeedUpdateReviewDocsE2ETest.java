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
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Docs screenshot for the admin seed-update review tree (seed-update review feature, Task 11).
 * Seeds pending decisions directly through the repository, navigates to the review screen, and
 * captures the diff-approval tree with pending rows visible.
 */
class SeedUpdateReviewDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private SeedUpdateDecisionRepository decisionRepository;
  @Autowired private SeedReviewGate gate;

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
            "fp-docs-1"));
    decisionRepository.saveAndFlush(
        conflict(
            "wrestler",
            "wrestler:bret-hart",
            "Bret Hart",
            "theme music",
            "Barbed Wire 4 Life",
            "Hits from the Bong",
            "fp-docs-2"));
    decisionRepository.saveAndFlush(
        conflict(
            "npc", "npc:samael", "Samael", "motive", "Harvest souls", "Devour souls", "fp-docs-3"));
    decisionRepository.saveAndFlush(
        removal("deck", "deck:legend-deck", "Legend Deck", "fp-docs-4"));
    // Re-read the snapshot so the gate is up for this session's navigations.
    gate.refresh();
  }

  @AfterEach
  void clearPendingDecisions() {
    decisionRepository.deleteAllInBatch();
    gate.refresh();
  }

  @Test
  void captureSeedReviewTree() {
    // The gate is up, so any non-exempt route lands on the review tree through the freeze.
    navigateToAndWaitForElement(SeedReviewGateView.REVIEW_ROUTE, By.id("seed-review-summary"));

    // Expand the Bret Hart entity so its per-field diff rows (current vs. new) are visible in
    // the capture — entity sections start collapsed.
    clickElement(
        By.xpath(
            "//vaadin-details[@id='entity-section-wrestler-bret-hart']"
                + "/vaadin-details-summary"));
    waitForVaadinElement(driver, By.id("decision-wrestler-wrestler-bret-hart-catchphrase"));

    documentFeature(
        "Admin",
        "Seed Update Review",
        "After a version upgrade, seed-data changes that would overwrite your customizations are"
            + " parked here instead of applied silently. Decisions group by domain and entity:"
            + " field rows show the current value vs. the incoming one with a Keep mine / Use new"
            + " choice per field, and removed-from-source entities show a Keep as custom / Remove"
            + " choice. 'Approve Everything' shortcuts the field conflicts (removals always need a"
            + " per-entity decision), and 'Apply decisions & unlock the app' writes the whole batch"
            + " and lifts the review freeze.",
        "admin-seed-review-tree");
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

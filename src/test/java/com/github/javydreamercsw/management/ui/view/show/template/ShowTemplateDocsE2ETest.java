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
package com.github.javydreamercsw.management.ui.view.show.template;

import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

class ShowTemplateDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private TournamentRepository tournamentRepository;

  private ShowTemplate template;

  @BeforeEach
  public void setupData() {
    cleanupLeagues();

    template =
        showTemplateRepository
            .findByName("Docs Gen Template")
            .orElseGet(
                () -> {
                  return showTemplateService.createOrUpdateTemplate(
                      "Docs Gen Template",
                      "A template for documentation generation.",
                      "Weekly",
                      null,
                      null,
                      5,
                      3,
                      1,
                      RecurrenceType.NONE,
                      null,
                      null,
                      null,
                      null,
                      null);
                });
  }

  @Test
  void testCaptureShowTemplateList() {
    navigateTo("show-template-list");
    waitForGridToPopulate("template-grid");

    documentFeature(
        "Admin",
        "Show Templates",
        """
        Manage templates for your shows. You can define default match and promo counts, and now\
         generate custom AI art for each template.\
        """,
        "admin-show-templates");
  }

  @Test
  void testCaptureShowTemplateImageGeneration() {
    navigateTo("show-template-list");
    waitForGridToPopulate("template-grid");

    // Click "Generate Art" button for our template
    String buttonId = "generate-art-btn-" + template.getId();
    WebElement generateButton = waitForVaadinElement(driver, By.id(buttonId));
    clickElement(generateButton);

    // Wait for Dialog
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("generate-image")));

    documentFeature(
        "Admin",
        "Generate Template Art",
        """
        Create unique branding for your show templates using AI. This art will be used in the\
         booking interface and calendar.\
        """,
        "admin-show-template-art-generation");
  }

  @Test
  void testCaptureTemplateTournamentAssignment() {
    // Seed a tournament and pair it with the docs template's assignment row (ATW-oahn).
    Tournament cup =
        tournamentRepository.findAll().stream()
            .filter(t -> "Docs Crown Cup".equals(t.getName()))
            .findFirst()
            .orElseGet(
                () -> {
                  Tournament t = new Tournament();
                  t.setName("Docs Crown Cup");
                  t.setFormatId("SINGLE_ELIMINATION");
                  t.setStatus(TournamentStatus.SCHEDULED);
                  return tournamentRepository.saveAndFlush(t);
                });

    ShowTemplate managed =
        showTemplateService.getTemplateWithAssignments(template.getId()).orElseThrow();
    managed.getSegmentAssignments().clear();
    ShowTemplateSegmentAssignment row = new ShowTemplateSegmentAssignment();
    row.setTournament(cup);
    row.setMode(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
    managed.getSegmentAssignments().add(row);
    showTemplateService.save(managed);

    navigateTo("show-template-list");
    waitForGridToPopulate("template-grid");

    WebElement editButton = waitForVaadinElement(driver, By.id("edit-btn-" + template.getId()));
    clickElement(editButton);

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(
        ExpectedConditions.presenceOfElementLocated(
            By.xpath("//*[contains(text(), 'Template Assignments')]")));

    documentFeature(
        "Admin",
        "Tournament-Fed Template Assignments",
        """
        Pair a tournament with a show template so its bracket feeds the participants of the\
         auto-attached segment. When a show of this template is approved, the tournament\
         resolves that match's entrants instead of the AI — an in-progress tournament fills\
         the next open bracket match, and a completed one books a winner showcase.\
        """,
        "admin-show-template-tournament-assignment");
  }
}

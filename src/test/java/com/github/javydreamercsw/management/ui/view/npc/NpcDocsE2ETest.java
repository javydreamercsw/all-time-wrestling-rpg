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
package com.github.javydreamercsw.management.ui.view.npc;

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

class NpcDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private NpcService npcService;
  @Autowired private NpcRepository npcRepository;

  private Npc npc;

  @BeforeEach
  public void setupData() {
    cleanupLeagues();

    npc = npcRepository.findByName("Docs Gen NPC")
        .orElseGet(() -> {
          Npc n = new Npc();
          n.setName("Docs Gen NPC");
          n.setNpcType("Manager");
          n.setDescription("A highly professional wrestling manager in a sharp suit.");
          return npcService.save(n);
        });
  }

  @Test
  void testCaptureNpcListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/npc-list");
    waitForVaadinClientToLoad();
    waitForGridToPopulate("npc-grid");

    documentFeature(
        "NPC",
        "NPC List",
        "View and manage Non-Player Characters (NPCs) such as managers, referees, and announcers. You can create new NPCs, edit existing ones, and navigate to their profiles.",
        "npc-list");
  }

  @Test
  void testCaptureNpcProfileView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/npc-profile/" + npc.getId());
    waitForVaadinClientToLoad();
    waitForText("Docs Gen NPC");

    documentFeature(
        "NPC",
        "NPC Profile",
        "Detailed view of an NPC, displaying their image, type, biography, and stats. From here, you can generate a custom image using AI.",
        "npc-profile");
  }

  @Test
  void testCaptureNpcImageGenerationDialog() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/npc-profile/" + npc.getId());
    waitForVaadinClientToLoad();
    waitForText("Docs Gen NPC");

    // Click Generate Image
    clickElement(By.id("generate-image-button"));

    // Wait for Dialog
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    try {
      wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Generate Image"));
    } catch (Exception e) {
      waitForVaadinElement(driver, By.tagName("vaadin-dialog-overlay"));
    }
    
    // Ensure dialog content is visible
    waitForVaadinElement(driver, By.id("generate-image"));

    documentFeature(
        "NPC",
        "Generate NPC Image",
        "Use the integrated AI tools to generate a unique portrait for your NPC based on their description and type. You can regenerate until you find the perfect look.",
        "npc-image-generation-dialog");
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}

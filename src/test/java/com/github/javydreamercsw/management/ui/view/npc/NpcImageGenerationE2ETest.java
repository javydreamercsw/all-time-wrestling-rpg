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

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class NpcImageGenerationE2ETest extends AbstractE2ETest {

  @Autowired private NpcService npcService;
  @Autowired private NpcRepository npcRepository;

  private Npc npc;

  @BeforeEach
  public void setupData() {
    cleanupLeagues();

    Optional<Npc> optionalNpc = npcRepository.findByName("Image Gen NPC");
    if (optionalNpc.isPresent()) {
      npc = optionalNpc.get();
    } else {
      Npc n = new Npc();
      n.setName("Image Gen NPC");
      n.setNpcType("Manager");
      n.setDescription("A visually striking manager.");
      npc = npcService.save(n);
    }
  }

  public String getUsername() {
    return "admin";
  }

  public String getPassword() {
    return "admin123";
  }

  @Test
  void testImageGenerationFlow() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/npc-list");
    waitForGridToPopulate("npc-grid");

    // 1. Click "Generate Image" button in grid
    // We added IDs to the buttons: generate-image-btn-<id>
    String buttonId = "generate-image-btn-" + npc.getId();

    // Wait for button to be present (it's inside a grid cell)
    // Grid scrolling might be needed if many NPCs exist, but we have few in test data.
    // However, AbstractE2ETest.clickElement handles scrolling.

    // We need to find the button. Since it's in a grid, we might need to rely on the grid rendering
    // it.
    // Let's try to find it directly first.

    // Grid cells are in shadow DOM or light DOM depending on rendering.
    // Vaadin 14+ grids render content in light DOM slots usually, but complex renderers might
    // differ.
    // Our NpcListView uses addComponentColumn, so the components are in the light DOM.

    WebElement generateButton = waitForVaadinElement(driver, By.id(buttonId));
    clickElement(generateButton);

    // 2. Wait for Dialog
    // Wait for the dialog to open by checking for its title
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    try {
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Generate Image"));
    } catch (Exception e) {
      // Fallback: check for vaadin-dialog element
      waitForVaadinElement(driver, By.tagName("vaadin-dialog-overlay"));
    }

    // 3. Click Generate
    // The dialog content is inside the overlay.
    // Use JS to find the button in the overlay because it might be in shadow DOM or overlay
    // structure
    WebElement generateButtonInDialog = waitForVaadinElement(driver, By.id("generate-image"));
    clickElement(generateButtonInDialog);

    // 4. Wait for Preview (Image should appear)
    // The image has a src attribute that changes.
    wait.until(
        d ->
            d.findElements(By.tagName("img")).stream()
                .anyMatch(
                    img -> {
                      String src = img.getAttribute("src");
                      return src != null
                          && (src.contains("base64")
                              || src.contains("placeholder")
                              || src.contains("generated"));
                    }));

    // 5. Click Save
    WebElement saveButton = waitForVaadinElement(driver, By.id("save-image"));
    // Ensure enabled
    wait.until(d -> saveButton.getAttribute("disabled") == null);
    clickElement(saveButton);

    // 6. Verify NPC Updated
    wait.until(
        d -> {
          // Reload from DB to check
          assert npc.getId() != null;
          Npc updated = npcRepository.findById(npc.getId()).orElseThrow();
          return updated.getImageUrl() != null && !updated.getImageUrl().isEmpty();
        });

    assert npc.getId() != null;
    Npc updated = npcRepository.findById(npc.getId()).orElseThrow();
    Assertions.assertNotNull(updated.getImageUrl());
    Assertions.assertTrue(
        updated.getImageUrl().contains("generated")
            || updated.getImageUrl().contains("placeholder")
            || updated.getImageUrl().contains("png"));

    // 7. Verify Grid UI Updated
    // We should check if the grid now shows the image URL or if the NPC object in grid has it.
    // The grid has an "Image URL" column.
    // Let's reload the page or check the grid content.
    // Simple verification: The dialog closed.
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("save-image")));
  }

  protected void waitForGridToPopulate(@NonNull String gridId) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        d -> {
          try {
            // Check if any row has text "Image Gen NPC"
            return Objects.requireNonNull(d.getPageSource()).contains("Image Gen NPC");
          } catch (Exception e) {
            return false;
          }
        });
  }
}

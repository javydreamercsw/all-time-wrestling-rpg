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

    // 1. Navigate to Profile
    String viewProfileId = "view-profile-btn-" + npc.getId();
    WebElement viewProfileButton = waitForVaadinElement(driver, By.id(viewProfileId));
    clickElement(viewProfileButton);

    // 2. Wait for Profile View
    waitForVaadinElement(driver, By.id("npc-name"));
    Assertions.assertEquals(npc.getName(), driver.findElement(By.id("npc-name")).getText());

    // 3. Click "Generate Image" button in profile
    clickElement(By.id("generate-image-button"));

    // 4. Wait for Dialog
    // Wait for the dialog to open by checking for its title
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    try {
      wait.until(
          ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Generate Image"));
    } catch (Exception e) {
      // Fallback: check for vaadin-dialog element
      waitForVaadinElement(driver, By.tagName("vaadin-dialog-overlay"));
    }

    // 5. Click Generate
    WebElement generateButtonInDialog = waitForVaadinElement(driver, By.id("generate-image"));
    clickElement(generateButtonInDialog);

    // 6. Wait for Preview (Image should appear)
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

    // 7. Click Save
    WebElement saveButton = waitForVaadinElement(driver, By.id("save-image"));
    // Ensure enabled
    wait.until(d -> saveButton.getAttribute("disabled") == null);
    clickElement(saveButton);

    // 8. Verify NPC Updated
    wait.until(
        d -> {
          // Reload from DB to check
          assert npc.getId() != null;
          Npc updated = npcRepository.findById(npc.getId()).orElseThrow();
          return updated.getImageUrl() != null && !updated.getImageUrl().isEmpty();
        });

    Npc updated = npcRepository.findById(npc.getId()).orElseThrow();
    Assertions.assertNotNull(updated.getImageUrl());
    Assertions.assertTrue(
        updated.getImageUrl().contains("generated")
            || updated.getImageUrl().contains("placeholder")
            || updated.getImageUrl().contains("png"));

    // 9. Verify Grid UI Updated
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("save-image")));

    // 10. Verify Image updated in Profile View
    WebElement npcImage = driver.findElement(By.id("npc-image"));
    String imageSrc = npcImage.getAttribute("src");
    Assertions.assertTrue(imageSrc.contains(updated.getImageUrl()));
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

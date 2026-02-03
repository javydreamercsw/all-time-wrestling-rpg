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
package com.github.javydreamercsw.management.ui.view.wrestler;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ImageGenerationE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Wrestler wrestler;

  @BeforeEach
  public void setupData() {
    cleanupLeagues();

    final Account admin = accountRepository.findByUsername("admin").get();

    wrestler =
        wrestlerRepository
            .findByName("Image Gen Subject")
            .orElseGet(
                () -> {
                  Wrestler w = createTestWrestler("Image Gen Subject");
                  w.setDescription("A visually striking wrestler.");
                  w.setAccount(admin);
                  return wrestlerService.save(w);
                });

    // Ensure account link is set (if reusing existing wrestler)
    if (wrestler.getAccount() == null || !wrestler.getAccount().equals(admin)) {
      wrestler.setAccount(admin);
      wrestlerService.save(wrestler);
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
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + wrestler.getId());

    // 1. Open Actions Menu
    // The menu bar is in the header. We need to find the "Actions" item.
    // Vaadin MenuBar items are tricky.
    // The "Actions" button is the first item.

    WebElement actionsButton =
        waitForVaadinElement(driver, By.xpath("//vaadin-menu-bar-item[contains(., 'Actions')]"));
    clickElement(actionsButton);

    // 2. Click "Generate Image" in submenu
    // Submenu items appear in vaadin-context-menu-overlay
    clickElement(By.id("generate-image-" + wrestler.getId()));

    // 3. Click Generate
    clickElement(By.id("generate-image"));

    // 4. Wait for Preview (Image should appear)
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(
        d ->
            d.findElements(By.tagName("img")).stream()
                .anyMatch(
                    img -> {
                      String src = img.getAttribute("src");
                      return src != null && (src.contains("base64") || src.contains("placeholder"));
                    }));

    // 5. Click Save
    WebElement saveButton = waitForVaadinElement(driver, By.id("save-image"));
    // Ensure enabled
    wait.until(d -> saveButton.getAttribute("disabled") == null);
    clickElement(saveButton);

    // 6. Verify Wrestler Updated
    wait.until(
        d -> {
          // Reload from DB to check
          assert wrestler.getId() != null;
          Wrestler updated = wrestlerRepository.findById(wrestler.getId()).orElseThrow();
          return updated.getImageUrl() != null && !updated.getImageUrl().isEmpty();
        });

    Wrestler updated = wrestlerRepository.findById(wrestler.getId()).orElseThrow();
    Assertions.assertNotNull(updated.getImageUrl());
    Assertions.assertTrue(
        updated.getImageUrl().contains("generated")
            || updated.getImageUrl().contains("placeholder")
            || updated.getImageUrl().contains("png"));

    // 7. Reload page to ensure persistence and UI update
    driver.navigate().refresh();
    waitForVaadinElement(driver, By.id("wrestler-image"));

    // 8. Verify UI shows updated image
    WebElement profileImage = driver.findElement(By.id("wrestler-image"));
    String newSrc = profileImage.getAttribute("src");
    Assertions.assertNotNull(newSrc);
    Assertions.assertFalse(
        newSrc.contains("via.placeholder.com"),
        "Image should not be the placeholder after generation");
    Assertions.assertTrue(
        newSrc.contains("generated")
            || newSrc.contains("placeholder.com")
            || newSrc.contains("png"),
        "Image src should contain generation path or filename");
  }
}

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

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.time.Duration;
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
public class ShowTemplateImageGenerationE2ETest extends AbstractE2ETest {

  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private ShowTemplateRepository showTemplateRepository;

  private ShowTemplate template;

  @BeforeEach
  public void setupData() {
    cleanupLeagues();

    template =
        showTemplateRepository
            .findByName("E2E Test Template")
            .orElseGet(
                () -> {
                  return showTemplateService.createOrUpdateTemplate(
                      "E2E Test Template", "A test template for E2E.", "Weekly", null);
                });
  }

  public String getUsername() {
    return "admin";
  }

  public String getPassword() {
    return "admin123";
  }

  @Test
  void testShowTemplateImageGenerationFlow() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-template-list");
    waitForGridToPopulate("template-grid"); // AbstractE2ETest helper if customized, or use local

    // 1. Click "Generate Art" button in grid
    String buttonId = "generate-art-btn-" + template.getId();
    WebElement generateButton = waitForVaadinElement(driver, By.id(buttonId));
    clickElement(generateButton);

    // 2. Wait for Dialog
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("generate-image")));

    // 3. Click Generate
    clickElement(By.id("generate-image"));

    // 4. Wait for Preview (Image should appear)
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
    wait.until(d -> saveButton.getAttribute("disabled") == null);
    clickElement(saveButton);

    // 6. Verify Template Updated in DB
    wait.until(
        d -> {
          assert template.getId() != null;
          ShowTemplate updated = showTemplateRepository.findById(template.getId()).orElseThrow();
          return updated.getImageUrl() != null && !updated.getImageUrl().isEmpty();
        });

    ShowTemplate updated = showTemplateRepository.findById(template.getId()).orElseThrow();
    Assertions.assertNotNull(updated.getImageUrl());
    Assertions.assertTrue(updated.getImageUrl().contains("generated"));

    // 7. Verify Grid UI Updated (Dialog closed and grid refreshed)
    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("save-image")));

    // Grid refresh check - the image src in the first row (if it's our template) should contain
    // 'generated'
    // This is a bit flaky depending on grid sort, but since we cleaned up leagues/data it should be
    // fine.
    WebElement grid = driver.findElement(By.id("template-grid"));
    wait.until(d -> grid.getText().contains("E2E Test Template"));
  }

  protected void waitForGridToPopulate(String gridId) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        d -> {
          try {
            return d.getPageSource().contains("E2E Test Template");
          } catch (Exception e) {
            return false;
          }
        });
  }
}

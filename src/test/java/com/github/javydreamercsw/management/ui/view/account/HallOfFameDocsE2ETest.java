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
package com.github.javydreamercsw.management.ui.view.account;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class HallOfFameDocsE2ETest extends AbstractE2ETest {

  @Autowired private AchievementRepository achievementRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  void testHallOfFameAchievementsDialog() {
    // Grant a few achievements to the admin account inside a transaction so
    // the lazy achievements collection can be initialized and modified.
    transactionTemplate.execute(
        status -> {
          var admin = accountRepository.findByUsername("admin").orElseThrow();
          achievementRepository.findAll().stream()
              .limit(3)
              .forEach(
                  a -> {
                    if (!admin.getAchievements().contains(a)) {
                      admin.getAchievements().add(a);
                    }
                  });
          accountRepository.saveAndFlush(admin);
          return null;
        });

    navigateTo("hall-of-fame");
    waitForVaadinElement(driver, By.id("hall-of-fame-grid"));

    documentFeature(
        "Community",
        "Hall of Fame — Achievement Trophies",
        "Each player row shows a trophy icon when they have unlocked achievements. Hover the icon"
            + " to see a quick summary; click it to open the full achievement history.",
        "community-hall-of-fame-trophies");

    // Click the first visible trophy button to open the dialog
    clickElement(By.xpath("//vaadin-button[.//vaadin-icon[@icon='vaadin:trophy']]"));

    waitForVaadinElement(driver, By.tagName("vaadin-dialog"));

    documentFeature(
        "Community",
        "Hall of Fame — Achievement Details",
        "The achievement dialog lists every unlocked achievement with its category, XP reward,"
            + " and the date and time it was earned.",
        "community-hall-of-fame-achievement-details");
  }
}

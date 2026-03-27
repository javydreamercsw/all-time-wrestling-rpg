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
package com.github.javydreamercsw.management.ui.view.image;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EntityImageE2ETest extends AbstractE2ETest {

  @BeforeEach
  void setUp() {
    cleanupLeagues();
  }

  @Test
  void testWrestlerProfileImage() {
    Wrestler wrestler =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Princess Aussie"));

    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + wrestler.getId());

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement image =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("wrestler-image")));

    String src = image.getAttribute("src");
    assertTrue(
        src.contains("images/wrestlers/Princess%20Aussie.png"),
        "Should show specific image for Princess Aussie. Found: " + src);
  }

  @Test
  void testWrestlerFallbackImage() {
    Wrestler wrestler =
        wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Unknown Wrestler"));

    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/wrestler-profile/"
            + wrestler.getId());

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement image =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("wrestler-image")));

    String src = image.getAttribute("src");
    assertTrue(
        src.contains("images/generic-wrestler.png"),
        "Should show generic wrestler image. Found: " + src);
  }

  @Test
  void testNpcFallbackImage() {
    Npc npc = new Npc();
    npc.setName("Unknown NPC");
    npc.setNpcType("Manager");
    npc = npcRepository.saveAndFlush(npc);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/npc-profile/" + npc.getId());

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement image = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("npc-image")));

    String src = image.getAttribute("src");
    assertTrue(
        src.contains("images/generic-npc.png"),
        "Should show generic npc image when AI is disabled. Found: " + src);
  }

  @Test
  void testChampionshipImage() {
    // ATW Tag Team is selected by default (alphabetically first in championships.json)
    driver.get("http://localhost:" + serverPort + getContextPath() + "/championship-rankings");

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Check image resolution
    WebElement image =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("championship-image")));
    wait.until(d -> image.getAttribute("src") != null && !image.getAttribute("src").isEmpty());

    String src = image.getAttribute("src");
    assertTrue(
        src.contains("images/championships/atw-tag-team.png"),
        "Should show specific image for ATW Tag Team championship by default. Found: " + src);
  }
}

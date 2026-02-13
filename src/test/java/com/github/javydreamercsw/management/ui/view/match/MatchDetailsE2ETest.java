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
package com.github.javydreamercsw.management.ui.view.match;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

class MatchDetailsE2ETest extends AbstractE2ETest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private InjuryRepository injuryRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SeasonRepository seasonRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;

  @Test
  void testInjuryDisplayInMatchDetails() {
    // 1. Setup data
    Account admin = accountRepository.findByUsername("admin").get();
    Wrestler wrestler =
        Wrestler.builder()
            .name("Injury Test Wrestler")
            .startingHealth(100)
            .startingStamina(100)
            .account(admin)
            .active(true)
            .build();
    wrestler = wrestlerRepository.saveAndFlush(wrestler);

    // Add some bumps
    wrestler.setBumps(2);
    wrestler = wrestlerRepository.saveAndFlush(wrestler);

    // Add an active injury
    Injury activeInjury = new Injury();
    activeInjury.setWrestler(wrestler);
    activeInjury.setName("Broken Arm");
    activeInjury.setSeverity(InjurySeverity.SEVERE);
    activeInjury.setHealthPenalty(20);
    activeInjury.setIsActive(true);
    activeInjury.setInjuryDate(Instant.now());
    injuryRepository.saveAndFlush(activeInjury);

    // Add a healed injury
    Injury healedInjury = new Injury();
    healedInjury.setWrestler(wrestler);
    healedInjury.setName("Twisted Ankle");
    healedInjury.setSeverity(InjurySeverity.MINOR);
    healedInjury.setHealthPenalty(5);
    healedInjury.setIsActive(false);
    healedInjury.setHealedDate(Instant.now().minusSeconds(3600));
    healedInjury.setInjuryDate(Instant.now().minusSeconds(7200));
    injuryRepository.saveAndFlush(healedInjury);

    // Create a match for this wrestler
    Season season = new Season();
    season.setName("Test Season");
    season.setDescription("Test Description");
    season = seasonRepository.saveAndFlush(season);

    ShowType showType = showTypeRepository.findAll().get(0);
    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Description");
    show.setSeason(season);
    show.setType(showType);
    show = showRepository.saveAndFlush(show);

    SegmentType segmentType = segmentTypeRepository.findByName("One on One").get();
    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment.addParticipant(wrestler);
    segment = segmentRepository.saveAndFlush(segment);

    // 2. Navigate to Match View
    driver.get("http://localhost:" + serverPort + getContextPath() + "/match/" + segment.getId());
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("match-view-injuries");

    // 3. Verify active injury and bumps are visible
    waitForText("Broken Arm");
    waitForText("📉 -2 bumps");

    // 4. Verify healed injury section is present but collapsed
    WebElement healedDetails =
        waitForVaadinElement(driver, By.xpath("//vaadin-details[contains(., 'Healed Injuries')]"));
    assertTrue(healedDetails.isDisplayed());

    // Check if it's opened - should be false
    Boolean isOpened =
        (Boolean)
            ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return arguments[0].opened;", healedDetails);
    assertFalse(Boolean.TRUE.equals(isOpened));

    // 5. Verify healed injury text is NOT immediately visible
    // We'll rely on the 'opened' property and then check visibility after click.

    // 6. Open it and verify healed injury is there
    // Try to click the summary part specifically
    ((org.openqa.selenium.JavascriptExecutor) driver)
        .executeScript(
            "const summary = arguments[0].shadowRoot.querySelector('[part=\"summary\"]');"
                + "if (summary) { summary.click(); } else { arguments[0].click(); }",
            healedDetails);

    // Wait for the property to change
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
        .until(
            d ->
                Boolean.TRUE.equals(
                    ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript("return arguments[0].opened;", healedDetails)));

    // Verify content is visible
    WebElement healedInjurySpan =
        healedDetails.findElement(By.xpath(".//span[contains(text(), 'Twisted Ankle')]"));
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
        .until(d -> healedInjurySpan.isDisplayed());

    isOpened =
        (Boolean)
            ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return arguments[0].opened;", healedDetails);
    assertTrue(Boolean.TRUE.equals(isOpened));
  }

  private void waitForText(String text) {
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
        .until(d -> d.getPageSource().contains(text));
  }
}

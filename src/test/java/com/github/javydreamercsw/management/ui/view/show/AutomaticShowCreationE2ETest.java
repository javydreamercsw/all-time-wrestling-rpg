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
package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "admin", roles = "ADMIN")
class AutomaticShowCreationE2ETest extends AbstractE2ETest {

  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private SeasonService seasonService;
  @Autowired private ShowService showService;
  @Autowired private GameSettingService gameSettingService;

  private ShowType weeklyShowType;
  private ShowType pleShowType;

  @BeforeEach
  void setup() {
    // Ensure clean state
    cleanupLeagues();
    showService
        .findAll()
        .forEach(
            s -> {
              assert s.getId() != null;
              showService.deleteShow(s.getId());
            });
    seasonService
        .getAllSeasons(PageRequest.of(0, Integer.MAX_VALUE))
        .forEach(
            s -> { // Corrected line
              assert s.getId() != null;
              seasonService.deleteSeason(s.getId());
            });
    showTemplateService
        .findAll()
        .forEach(
            t -> {
              assert t.getId() != null;
              showTemplateService.deleteTemplate(t.getId());
            });

    // Set a consistent game date for testing purposes
    gameSettingService.saveCurrentGameDate(LocalDate.of(2026, 1, 15)); // Mid-January

    // Create show types
    weeklyShowType =
        showTypeService
            .findByName("Weekly")
            .orElseGet(
                () -> showTypeService.createOrUpdateShowType("Weekly", "Weekly Event", 5, 2));
    pleShowType =
        showTypeService
            .findByName("Premium Live Event (PLE)")
            .orElseGet(
                () ->
                    showTypeService.createOrUpdateShowType(
                        "Premium Live Event (PLE)", "Premium Live Event", 7, 3));

    // Create a season that spans at least two months
    Season currentSeason = new Season();
    currentSeason.setName("Test Auto Season");
    currentSeason.setDescription("Season for automatic show creation testing");
    currentSeason.setStartDate(
        LocalDate.of(2026, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    currentSeason.setEndDate(
        LocalDate.of(2026, 3, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
    currentSeason.setIsActive(true);
    seasonService.save(currentSeason);
  }

  @Test
  void testAutomaticShowCreation() {
    // Create templates
    showTemplateService.createOrUpdateTemplate(
        "Monday Night Mayhem",
        "Weekly Monday show",
        weeklyShowType.getName(),
        null,
        null,
        5,
        2,
        1,
        RecurrenceType.WEEKLY,
        DayOfWeek.MONDAY,
        null,
        null,
        null);

    showTemplateService.createOrUpdateTemplate(
        "Friday Night Fire",
        "Weekly Friday show",
        weeklyShowType.getName(),
        null,
        null,
        5,
        2,
        1,
        RecurrenceType.WEEKLY,
        DayOfWeek.FRIDAY,
        null,
        null,
        null);

    showTemplateService.createOrUpdateTemplate(
        "WrestleFest",
        "Monthly PLE",
        pleShowType.getName(),
        null,
        null,
        7,
        3,
        2, // 2-day PLE
        RecurrenceType.MONTHLY,
        DayOfWeek.SUNDAY,
        null,
        -1, // Last Sunday
        Month.JANUARY);

    // Navigate to Season Settings view
    driver.get(
        "http://localhost:"
            + serverPort
            + getContextPath()
            + "/admin"); // Admin view contains Season Settings
    waitForVaadinClientToLoad();

    // Click the "Season Settings" tab
    click("vaadin-tab", "Season Settings");
    waitForVaadinElement(
        driver, By.id("generate-schedule-button")); // Wait for element in Season Settings

    documentFeature(
        "Admin",
        "Season Settings - Before Schedule Generation",
        "The Season Settings tab showing the 'Generate Season Schedule' button, before execution.",
        "admin-season-settings-before-generation");

    // Click the generate button
    WebElement generateButton = waitForVaadinElement(driver, By.id("generate-schedule-button"));
    clickElement(generateButton);

    // Confirm the dialog
    WebElement confirmButton =
        waitForVaadinElement(driver, By.id("confirm-generate-schedule-button"));
    clickElement(confirmButton);

    // Wait for success notification
    waitForNotification("Season schedule generated successfully.");

    documentFeature(
        "Admin",
        "Season Settings - After Schedule Generation",
        "The Season Settings tab after successfully generating the season schedule.",
        "admin-season-settings-after-generation");

    // Verify shows were created
    List<com.github.javydreamercsw.management.domain.show.Show> allShows = showService.findAll();
    assertFalse(allShows.isEmpty());
    assertEquals(32, allShows.size());

    // Verify specific shows exist
    Optional<com.github.javydreamercsw.management.domain.show.Show> mondayJan19 =
        showService.findByName("Monday Night Mayhem").stream()
            .filter(s -> s.getShowDate().equals(LocalDate.of(2026, 1, 19)))
            .findFirst();
    assertTrue(mondayJan19.isPresent());

    Optional<com.github.javydreamercsw.management.domain.show.Show> fridayFeb6 =
        showService.findByName("Friday Night Fire").stream()
            .filter(s -> s.getShowDate().equals(LocalDate.of(2026, 2, 6)))
            .findFirst();
    assertTrue(fridayFeb6.isPresent());

    // Verify PLEs - Last Sunday of Jan 2026 is Jan 25
    Optional<com.github.javydreamercsw.management.domain.show.Show> wrestleFestJanNight1 =
        showService.findByName("WrestleFest - Night 1").stream()
            .filter(s -> s.getShowDate().equals(LocalDate.of(2026, 1, 25)))
            .findFirst();
    assertTrue(wrestleFestJanNight1.isPresent());

    Optional<com.github.javydreamercsw.management.domain.show.Show> wrestleFestJanNight2 =
        showService.findByName("WrestleFest - Night 2").stream()
            .filter(s -> s.getShowDate().equals(LocalDate.of(2026, 1, 26)))
            .findFirst();
    assertTrue(wrestleFestJanNight2.isPresent());

    // Verify PLEs - Last Sunday of Feb 2026 is Feb 22
    Optional<com.github.javydreamercsw.management.domain.show.Show> wrestleFestFebNight1 =
        showService.findByName("WrestleFest - Night 1").stream()
            .filter(s -> s.getShowDate().equals(LocalDate.of(2026, 2, 22)))
            .findFirst();
    assertTrue(wrestleFestFebNight1.isPresent());

    Optional<com.github.javydreamercsw.management.domain.show.Show> wrestleFestFebNight2 =
        showService.findByName("WrestleFest - Night 2").stream()
            .filter(s -> s.getShowDate().equals(LocalDate.of(2026, 2, 23)))
            .findFirst();
    assertTrue(wrestleFestFebNight2.isPresent());
  }
}

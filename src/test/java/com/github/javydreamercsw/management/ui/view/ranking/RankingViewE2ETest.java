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
package com.github.javydreamercsw.management.ui.view.ranking;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class RankingViewE2ETest extends AbstractE2ETest {

  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private CampaignRepository campaignRepository;
  @Autowired private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Autowired private CampaignStateRepository campaignStateRepository;
  @Autowired private BackstageActionHistoryRepository backstageActionHistoryRepository;
  @Autowired private CampaignEncounterRepository campaignEncounterRepository;
  @Autowired private CacheManager cacheManager;

  @Autowired
  private com.github.javydreamercsw.management.service.show.type.ShowTypeService showTypeService;

  @BeforeEach
  void setUp() {
    cleanupLeagues();
    if (cacheManager != null) {

      cacheManager
          .getCacheNames()
          .forEach(
              name -> {
                Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                  cache.clear();
                }
              });
    }

    // Targeted cleanup for entities created in this test
    titleReignRepository.deleteAllInBatch();
    titleRepository
        .findAll()
        .forEach(
            t -> {
              t.setChampion(null);
              titleRepository.save(t);
            });
    titleRepository.deleteAllInBatch();
    segmentRepository.deleteAllInBatch();
    showRepository.deleteAllInBatch();

    // Ensure Weekly show type exists for the test
    showTypeService.createOrUpdateShowType("Weekly", "Weekly Show", 4, 2);
  }

  @Test
  void testChampionshipHistoryIsVisible() {
    // Given
    Wrestler champion = wrestlerRepository.saveAndFlush(TestUtils.createWrestler("Legacy Champ"));

    Title title =
        titleService.createTitle(
            "Legacy Title",
            "The historic title",
            WrestlerTier.MAIN_EVENTER,
            ChampionshipType.SINGLE);

    Season season = seasonService.createSeason("Legacy Season", "Season for legacy", 10);
    Show show =
        showService.createShow(
            "Historic Event",
            "Big Event",
            showTypeRepository.findByName("Weekly").get().getId(),
            null,
            season.getId(),
            null,
            null,
            null,
            null);

    SegmentType matchType = segmentTypeService.findByName("One on One").get();
    Segment segment =
        segmentService.createSegment(show, matchType, Instant.now().minusSeconds(1000));
    segment.addParticipant(champion);
    segment.setWinners(List.of(champion));
    segment = segmentRepository.saveAndFlush(segment);

    // Award title
    title.awardTitleTo(List.of(champion), Instant.now().minusSeconds(500), segment);
    titleRepository.saveAndFlush(title);

    // When
    driver.get("http://localhost:" + serverPort + getContextPath() + "/championship-rankings");

    // Then
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Explicitly select the championship to trigger update
    WebElement comboBox = waitForVaadinElement(driver, By.id("championship-combo-box"));
    selectFromVaadinComboBox(comboBox, "Legacy Title");
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h3[text()='Championship History']")));

    // Verify timeline presence
    assertNotNull(waitForVaadinElement(driver, By.xpath("//span[text()='CURRENT']")));

    // Verify card presence
    assertNotNull(waitForVaadinElement(driver, By.xpath("//span[text()='Legacy Champ']")));

    // Verify match link
    WebElement link =
        waitForVaadinElement(driver, By.xpath("//a[contains(text(), 'Won at: Historic Event')]"));
    assertNotNull(link);

    // Click and verify navigation
    clickElement(link);
    wait.until(ExpectedConditions.urlContains("show-detail/" + show.getId()));
  }
}

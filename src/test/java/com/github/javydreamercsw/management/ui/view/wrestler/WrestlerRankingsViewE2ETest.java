package com.github.javydreamercsw.management.ui.view.wrestler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

public class WrestlerRankingsViewE2ETest extends AbstractE2ETest {

  @Autowired private TitleRepository titleRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Autowired private TitleService titleService;

  @BeforeEach
  public void setupChampion() {
    // Check via API if a champion is assigned
    if (!isChampionAssigned()) {
      assignChampionViaApi();
    }
  }

  @Test
  public void testChampionIcon() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-rankings");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for the grid to be present
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    assertNotNull(grid);

    WebElement trophyIcon =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("vaadin-icon[icon*='trophy']")));
    assertNotNull(trophyIcon);
    String iconAttr = trophyIcon.getAttribute("icon");
    assertNotNull(iconAttr);
    assertTrue(iconAttr.contains("trophy"));
  }

  // Use the API to check if a champion is assigned
  private boolean isChampionAssigned() {
    return titleRepository.findAll().stream()
        .anyMatch(title -> title.getChampion() != null && !title.getChampion().isEmpty());
  }

  // Use the API to assign a champion if not present
  private void assignChampionViaApi() {
    List<Title> titles = titleRepository.findAll();
    if (!titles.isEmpty()) {
      Title title = titles.get(0);
      titleService.awardTitleTo(
          title,
          Arrays.asList(
              wrestlerRepository
                  .findAll()
                  .get(new Random().nextInt(wrestlerRepository.findAll().size()))));
    }
  }
}

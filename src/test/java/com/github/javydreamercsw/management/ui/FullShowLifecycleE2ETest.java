package com.github.javydreamercsw.management.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class FullShowLifecycleE2ETest extends AbstractE2ETest {
  private static final String SHOW_TYPE_NAME = "Weekly";
  private static final String SEASON_NAME = "Test Season";
  private static final String TEMPLATE_NAME = "Continuum";

  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private SeasonRepository seasonRepository;
  @Autowired private ShowTypeRepository showTypeRepository;

  @BeforeEach
  public void setupTestData() {
    // Clear and insert required ShowType
    Optional<ShowType> st = showTypeRepository.findByName(SHOW_TYPE_NAME);
    if (st.isEmpty()) {
      ShowType showType = new ShowType();
      showType.setName(SHOW_TYPE_NAME);
      showTypeRepository.save(showType);
    }

    // Clear and insert required ShowTemplate
    Optional<ShowTemplate> t = showTemplateRepository.findByName(TEMPLATE_NAME);
    if (t.isEmpty()) {
      ShowTemplate template = new ShowTemplate();
      template.setName(TEMPLATE_NAME);
      showTemplateRepository.save(template);
    }

    // Clear and insert required Season
    Optional<Season> s = seasonRepository.findByName(SEASON_NAME);
    if (s.isEmpty()) {
      Season season = new Season();
      season.setName(SEASON_NAME);
      seasonRepository.save(season);
    }
  }

  @Test
  public void testFullShowLifecycle() {
    // Navigate to the Show List view
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-list");

    final String showName = "My E2E Show";

    // Click the "Create" button
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Fill in the form
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("vaadin-text-field")))
        .sendKeys(showName);
    List<WebElement> comboBoxes = driver.findElements(By.cssSelector("vaadin-combo-box"));
    comboBoxes.get(0).sendKeys(SHOW_TYPE_NAME);
    comboBoxes.get(1).sendKeys(SEASON_NAME);
    comboBoxes.get(2).sendKeys(TEMPLATE_NAME);
    driver
        .findElement(By.cssSelector("vaadin-date-picker"))
        .sendKeys(LocalDate.now().format(DateTimeFormatter.ofPattern("M/d/yyyy")));
    // Click the "Create" button
    WebElement createButton = driver.findElement(By.xpath("//vaadin-button[text()='Create']"));
    scrollIntoView(createButton);
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", createButton);

    RetryPolicy<Object> retryPolicy =
        RetryPolicy.builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(10))
            .handleResultIf(result -> !((WebElement) result).getText().contains(showName))
            .build();
    Failsafe.with(retryPolicy)
        .get(
            () -> {
              // Re-fetch the grid to ensure it's up-to-date
              WebElement refreshedGrid = driver.findElement(By.tagName("vaadin-grid"));
              assertTrue(refreshedGrid.getText().contains(showName), refreshedGrid.getText());
              return refreshedGrid;
            });
  }
}

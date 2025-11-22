package com.github.javydreamercsw.management.ui.view.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@EnabledIf("com.github.javydreamercsw.base.util.EnvironmentVariableUtil#isNotionTokenAvailable")
public class NotionSyncViewE2ETest extends AbstractE2ETest {

  @MockitoBean private NotionSyncScheduler notionSyncScheduler;

  @Test
  public void testControlAlignment() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/notion-sync");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    WebElement controlSection =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath(
                    "//vaadin-horizontal-layout[.//vaadin-button[text()='Sync All Entities']]")));

    assertEquals("baseline", controlSection.getCssValue("align-items"));
  }
}

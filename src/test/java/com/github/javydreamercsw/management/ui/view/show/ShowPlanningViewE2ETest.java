package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.AbstractE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ShowPlanningViewE2ETest extends AbstractE2ETest {

  @Test
  public void testNavigateToShowPlanningView() {
    driver.get("http://localhost:8080/show-planning");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Check that the "Select Show" ComboBox is present
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//vaadin-combo-box[@label='Select Show']")));
    assertNotNull(driver.findElement(By.xpath("//vaadin-combo-box[@label='Select Show']")));
  }
}

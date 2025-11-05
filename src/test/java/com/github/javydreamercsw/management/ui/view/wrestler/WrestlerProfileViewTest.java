package com.github.javydreamercsw.management.ui.view.wrestler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

class WrestlerProfileViewTest extends AbstractE2ETest {

  @Autowired private WrestlerService wrestlerService;

  private Wrestler testWrestler;

  @BeforeEach
  void setUp() {
    testWrestler = new Wrestler();
    testWrestler.setName("Test Wrestler");
    testWrestler.setGender(Gender.MALE);
    testWrestler.setFans(1000L);
    testWrestler = wrestlerService.save(testWrestler);
  }

  @Test
  void testWrestlerProfileLoads() {
    driver.get("http://localhost:" + serverPort + "/wrestler-profile/" + testWrestler.getId());

    // Wait for the view to load
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h2")));

    WebElement wrestlerName = driver.findElement(By.tagName("h2"));
    assertEquals(testWrestler.getName(), wrestlerName.getText());

    WebElement wrestlerDetails = driver.findElement(By.tagName("p"));
    assertTrue(wrestlerDetails.getText().contains("Gender: " + testWrestler.getGender()));
    assertTrue(wrestlerDetails.getText().contains("Fans: " + testWrestler.getFans()));
  }
}

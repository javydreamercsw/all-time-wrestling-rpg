package com.github.javydreamercsw.management.ui.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class MainLayoutE2ETest extends AbstractE2ETest {

  @Test
  void testGithubLink() {
    driver.get("http://localhost:" + serverPort + getContextPath());
    waitForVaadinClientToLoad();

    // Wait for the footer to be present
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-app-layout")));

    // Find the link
    WebElement link = driver.findElement(By.linkText("Source Code"));

    // Assert that the link is visible and enabled
    assertTrue(link.isDisplayed());
    assertTrue(link.isEnabled());

    // Assert that the href is correct
    assertEquals(
        "https://github.com/javydreamercsw/all-time-wrestling-rpg", link.getAttribute("href"));
  }
}

/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw;

import com.github.javydreamercsw.base.config.TestE2ESecurityConfig;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(UITestWatcher.class)
@Slf4j
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = Application.class)
@ActiveProfiles(value = "e2e", inheritProfiles = false)
@Import(TestE2ESecurityConfig.class)
@WithMockUser(username = "admin", roles = "ADMIN")
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

  protected WebDriver driver;
  @LocalServerPort protected int serverPort;

  @Value("${server.servlet.context-path}")
  @Getter
  private String contextPath;

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
      return (web) -> web.ignoring().anyRequest();
    }
  }

  @BeforeEach
  public void setup() {
    WebDriverManager.chromedriver().setup();
    log.info("Waiting for application to be ready on port {}", serverPort);
    waitForAppToBeReady();
    log.info("Application is ready on port {}", serverPort);
    ChromeOptions options = new ChromeOptions();
    if (isHeadless()) {
      options.addArguments("--headless=new");
      options.addArguments("--disable-gpu");
      options.addArguments("--window-size=1920,1080");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");
    }

    driver = new ChromeDriver(options);
    driver.get("http://localhost:" + serverPort + getContextPath() + "/login");
    waitForAppToBeReady();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement loginFormHost =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("vaadinLoginFormWrapper")));
    WebElement usernameField = loginFormHost.findElement(By.id("vaadinLoginUsername"));
    usernameField.sendKeys("admin");
    WebElement passwordField = loginFormHost.findElement(By.id("vaadinLoginPassword"));
    passwordField.sendKeys("admin123");
    WebElement signInButton =
        loginFormHost.findElement(By.cssSelector("vaadin-button[slot='submit']"));
    clickElement(signInButton);
    waitForAppToBeReady();
    wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("login")));
  }

  @AfterEach
  public void teardown() {
    if (driver != null) {
      driver.quit();
    }
  }

  private boolean isHeadless() {
    String headlessProp = System.getProperty("headless");
    String headlessEnv = System.getenv("HEADLESS");
    String githubActions = System.getenv("GITHUB_ACTIONS");
    if (headlessProp != null) {
      return headlessProp.equalsIgnoreCase("true");
    }
    if (headlessEnv != null) {
      return headlessEnv.equalsIgnoreCase("true");
    }
    // Default to headless in CI
    return "true".equalsIgnoreCase(githubActions);
  }

  /** Waits for the application to be ready by polling the root URL. */
  private void waitForAppToBeReady() {
    int maxAttempts = 60;
    int attempt = 0;
    while (attempt < maxAttempts) {
      try {
        URL url = new URL("http://localhost:" + serverPort + getContextPath());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(1000);
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
          return;
        }
      } catch (Exception e) {
        // Ignore and retry
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ignored) {
      }
      attempt++;
    }
    throw new RuntimeException(
        "Application did not start within timeout. "
            + "Please check if the server is running on port "
            + serverPort);
  }

  /**
   * Waits for the Vaadin components to load by checking for the presence of a vaadin-grid element.
   *
   * @param driver the WebDriver instance
   */
  protected void waitForVaadinToLoad(@NonNull WebDriver driver) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));
  }

  /** Waits for the Vaadin client-side application to fully load. */
  protected void waitForVaadinClientToLoad() {
    WebDriverWait wait =
        new WebDriverWait(driver, Duration.ofSeconds(30)); // Increased timeout for Vaadin client

    // Wait for document.readyState to be 'complete'
    wait.until(
        webDriver ->
            Objects.equals(
                ((JavascriptExecutor) webDriver).executeScript("return document.readyState"),
                "complete"));

    // Wait for the main Vaadin app layout element to be present
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-app-layout")));
  }

  /**
   * Scrolls the given WebElement into view and clicks it using JavaScript.
   *
   * @param selector the WebElement to scroll into view and click
   */
  protected void clickElement(@NonNull By selector) {
    clickElement(driver.findElement(selector));
  }

  /**
   * Scrolls the given WebElement into view and clicks it using JavaScript.
   *
   * @param element the WebElement to scroll into view and click
   */
  protected void clickElement(@NonNull WebElement element) {
    scrollIntoView(element);
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
  }

  /**
   * Scrolls the given WebElement into view using JavaScript.
   *
   * @param element the WebElement to scroll into view
   */
  protected void scrollIntoView(@NonNull WebElement element) {
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
  }

  /**
   * Selects an item from a Vaadin ComboBox by clicking it and selecting the item from the overlay.
   *
   * @param comboBox the Vaadin ComboBox WebElement
   * @param itemText the text of the item to select
   */
  protected void selectFromVaadinComboBox(@NonNull WebElement comboBox, @NonNull String itemText) {
    // Click the combo box to open it
    comboBox.click();

    // Wait for the overlay to appear and the item to be clickable
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement item =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//vaadin-combo-box-overlay//*[text()='" + itemText + "']")));

    // Click the item (item is guaranteed to be non-null by wait.until)
    if (item != null) {
      item.click();
    }
  }

  /**
   * Selects an item from a Vaadin MultiSelectComboBox by clicking its input/toggle button and
   * selecting the item from the overlay. Handles shadow DOM for Vaadin components. Uses correct
   * overlay for MultiSelectComboBox.
   *
   * @param comboBox the Vaadin MultiSelectComboBox WebElement
   * @param itemText the text of the item to select
   */
  protected void selectFromVaadinMultiSelectComboBox(
      @NonNull WebElement comboBox, @NonNull String itemText) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    int attempts = 0;
    boolean itemClicked = false;
    while (attempts < 3 && !itemClicked) {
      attempts++;
      // Try to click the input or toggle button inside the shadow root
      try {
        Object input =
            ((JavascriptExecutor) driver)
                .executeScript(
                    "return arguments[0].shadowRoot ? arguments[0].shadowRoot.querySelector('input,"
                        + " [part=\"toggle-button\"]') : null;",
                    comboBox);
        if (input != null && input instanceof WebElement) {
          ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", input);
          try {
            ((WebElement) input).click();
          } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", input);
          }
        } else {
          // Fallback: scroll and click the comboBox itself
          ((JavascriptExecutor) driver)
              .executeScript("arguments[0].scrollIntoView(true);", comboBox);
          try {
            comboBox.click();
          } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", comboBox);
          }
        }
      } catch (Exception e) {
        // Fallback: scroll and click the comboBox itself
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", comboBox);
        try {
          comboBox.click();
        } catch (Exception ex) {
          ((JavascriptExecutor) driver).executeScript("arguments[0].click();", comboBox);
        }
      }
      // Wait a bit for overlay to render
      try {
        Thread.sleep(300);
      } catch (InterruptedException ignored) {
      }
      // Wait for overlay to be visible (correct overlay for MultiSelectComboBox)
      try {
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("vaadin-multi-select-combo-box-overlay")));
        // Use contains(text(), ...) for more robust matching
        WebElement item =
            wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath(
                        "//vaadin-multi-select-combo-box-overlay//*[contains(text(), '"
                            + itemText
                            + "')]")));
        // Scroll into view if needed
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", item);
        item.click();
        itemClicked = true;
      } catch (Exception e) {
        // If not found, try again (overlay may not have opened)
        if (attempts >= 3) throw e;
      }
    }
  }

  /**
   * Returns all the data from a specific column in a Vaadin grid.
   *
   * @param grid the Vaadin grid WebElement
   * @param columnIndex the index of the column (0-based)
   * @return List of Strings representing the data in the specified column
   */
  protected List<String> getColumnData(@NonNull WebElement grid, int columnIndex) {
    List<WebElement> cells =
        grid.findElements(
            By.xpath(
                "//vaadin-grid-cell-content[count(ancestor::vaadin-grid-column) = "
                    + (columnIndex + 1)
                    + "]"));
    return cells.stream().map(WebElement::getText).collect(Collectors.toList());
  }

  /**
   * Returns all the row elements of a Vaadin grid.
   *
   * @param grid the Vaadin grid WebElement
   * @return List of WebElements, each representing a row
   */
  protected List<WebElement> getGridRows(@NonNull WebElement grid) {
    return (List<WebElement>)
        ((JavascriptExecutor) driver)
            .executeScript(
                "return arguments[0].shadowRoot.querySelectorAll('[part~=\"row\"]')", grid);
  }

  protected void takeScreenshot(@NonNull String filePath) {
    File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
    try {
      FileUtils.copyFile(scrFile, new File(filePath));
      log.info("Screenshot saved to: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to save screenshot to: {}", filePath, e);
    }
  }

  protected void takeElementScreenshot(@NonNull WebElement element, @NonNull String filePath) {
    File scrFile = element.getScreenshotAs(OutputType.FILE);
    try {
      FileUtils.copyFile(scrFile, new File(filePath));
      log.info("Screenshot saved to: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to save screenshot to: {}", filePath, e);
    }
  }
}

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
package com.github.javydreamercsw;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"e2e", "test"})
@Slf4j
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

  @LocalServerPort protected int serverPort;

  protected WebDriver driver;
  private static final AtomicInteger docOrder = new AtomicInteger(1);
  private int screenshotCounter = 0;
  private Path testArtifactsDir;

  @BeforeEach
  public void setup(TestInfo testInfo) throws Exception {
    if (cacheManager != null) {
      cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    cleanupLeagues();

    // Create artifact directory
    screenshotCounter = 0;
    try {
      String testClassName =
          testInfo.getTestClass().map(Class::getSimpleName).orElse("unknown-class");
      String testMethodName =
          testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown-method");
      testArtifactsDir = Paths.get("target", "test-failures", testClassName, testMethodName);
      // Clean up directory from previous runs
      if (Files.exists(testArtifactsDir)) {
        FileUtils.cleanDirectory(testArtifactsDir.toFile());
      }
      Files.createDirectories(testArtifactsDir);
    } catch (IOException e) {
      log.error("Unable to create test artifact directory", e);
      testArtifactsDir = null; // So we don't try to use it
    }

    log.info("Waiting for server to be ready on port {}...", serverPort);
    waitForServerToBeReady();
    log.info("Server is ready.");

    WebDriverManager.chromedriver().setup();

    ChromeOptions options = new ChromeOptions();
    if (isHeadless()) {
      options.addArguments("--headless=new");
    }
    options.addArguments("--disable-gpu");
    if (Boolean.getBoolean("generate.docs")) {
      // Consistent size for documentation screenshots
      options.addArguments("--window-size=1920,1080");
    } else {
      options.addArguments("--window-size=1920,1080");
    }

    // Set non-interactive flags for Vaadin
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--no-sandbox");

    if (driver != null) {
      try {
        driver.quit();
      } catch (Exception ignored) {
      } finally {
        driver = null;
      }
    }

    driver = new ChromeDriver(options);
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

    loginAs(getUsername());
    login();
  }

  /** Waits for the server to respond with 200 OK on the root URL. */
  protected void waitForServerToBeReady() {
    int maxAttempts = 300;
    int attempt = 0;
    while (attempt < maxAttempts) {
      try {
        java.net.URL url =
            new java.net.URL("http://localhost:" + serverPort + getContextPath() + "/health");
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(1000);
        int responseCode = connection.getResponseCode();
        log.info("Server readiness check (attempt {}): HTTP {}", attempt + 1, responseCode);
        if (responseCode == 200) {
          return;
        }
      } catch (Exception e) {
        log.info("Server readiness check (attempt {}): {}", attempt + 1, e.getMessage());
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ignored) {
      }
      attempt++;
    }
    throw new RuntimeException(
        "Server did not start within timeout. "
            + "Please check if the server is running on port "
            + serverPort);
  }

  protected String getContextPath() {
    return "/atw-rpg";
  }

  protected void takeDocScreenshot(@NonNull String fileName) {
    if (Boolean.getBoolean("generate.docs")) {
      try {
        Path docsDir = Paths.get("docs", "screenshots");
        if (!Files.exists(docsDir)) {
          Files.createDirectories(docsDir);
        }
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String finalName = fileName.endsWith(".png") ? fileName : fileName + ".png";
        FileUtils.copyFile(screenshot, docsDir.resolve(finalName).toFile());
      } catch (IOException e) {
        log.error("Failed to take documentation screenshot", e);
      }
    }
  }

  protected void documentFeature(
      @NonNull String category,
      @NonNull String title,
      @NonNull String description,
      @NonNull String screenshotName) {
    if (Boolean.getBoolean("generate.docs")) {
      takeDocScreenshot(screenshotName);
    }
  }

  protected void login() {
    login(getUsername(), getPassword());
  }

  protected void login(@NonNull String username, @NonNull String password) {
    int maxRetries = 3;
    int attempt = 0;
    while (attempt < maxRetries) {
      try {
        log.info("Login attempt {} for user: {}", attempt + 1, username);
        driver.get("http://localhost:" + serverPort + getContextPath() + "/login");
        waitForVaadinClientToLoad();
        takeSequencedScreenshot("on-login-page");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement loginFormHost;
        try {
          loginFormHost =
              wait.until(
                  ExpectedConditions.presenceOfElementLocated(By.id("vaadinLoginFormWrapper")));
        } catch (Exception e) {
          log.error("Login form not found at URL: {}", driver.getCurrentUrl());
          log.error("Page source: {}", driver.getPageSource());
          throw e;
        }

        WebElement usernameField = loginFormHost.findElement(By.id("vaadinLoginUsername"));
        WebElement usernameInput =
            (WebElement)
                ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].querySelector('input');", usernameField);
        if (usernameInput == null) {
          usernameInput = usernameField;
        }

        WebElement passwordField = loginFormHost.findElement(By.id("vaadinLoginPassword"));
        WebElement passwordInput =
            (WebElement)
                ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].querySelector('input');", passwordField);
        if (passwordInput == null) {
          passwordInput = passwordField;
        }

        // Use standard sendKeys after ensuring focus and clear state
        clickElement(usernameInput);
        clearField(usernameInput);
        usernameInput.sendKeys(username);

        clickElement(passwordInput);
        clearField(passwordInput);
        passwordInput.sendKeys(password);

        // Explicitly fire events to ensure Vaadin picks up the values
        ((JavascriptExecutor) driver)
            .executeScript(
                "arguments[0].dispatchEvent(new CustomEvent('input', { bubbles: true })); "
                    + "arguments[0].dispatchEvent(new CustomEvent('change', { bubbles: true }));",
                usernameInput);
        ((JavascriptExecutor) driver)
            .executeScript(
                "arguments[0].dispatchEvent(new CustomEvent('input', { bubbles: true })); "
                    + "arguments[0].dispatchEvent(new CustomEvent('change', { bubbles: true }));",
                passwordInput);

        try {
          Thread.sleep(500); // Small wait for events to process
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        takeSequencedScreenshot("after-filling-credentials");
        WebElement signInButton =
            loginFormHost.findElement(By.cssSelector("vaadin-button[slot='submit']"));
        clickElement(signInButton);

        // Wait for successful login (logout button appears or URL changes)
        new WebDriverWait(driver, Duration.ofSeconds(60))
            .until(
                d -> {
                  try {
                    return d.findElements(By.id("logout-button")).size() > 0
                        || !d.getCurrentUrl().endsWith("/login");
                  } catch (Exception e) {
                    return false;
                  }
                });
        takeSequencedScreenshot("after-successful-login");
        log.info("Login successful for user: {}", username);
        return; // Success!
      } catch (Exception e) {
        attempt++;
        log.warn("Login attempt {} failed: {}", attempt, e.getMessage());
        if (attempt >= maxRetries) {
          log.error("All login attempts failed for user: {}", username);
          takeSequencedScreenshot("on-login-final-failure");
          throw e;
        }
      }
    }
  }

  @AfterEach
  public void teardown() {
    if (driver != null) {
      try {
        driver.quit();
      } catch (Exception e) {
        log.warn("Error during driver teardown: {}", e.getMessage());
      } finally {
        driver = null;
      }
    }
    System.clearProperty("simulateFailure");
  }

  protected boolean isHeadless() {
    String headlessProp = System.getProperty("headless");
    String headlessEnv = System.getenv("HEADLESS");
    String githubActions = System.getenv("GITHUB_ACTIONS");
    if (headlessProp != null) {
      return headlessProp.equalsIgnoreCase("true");
    }
    if (headlessEnv != null) {
      return headlessEnv.equalsIgnoreCase("true");
    }
    return "true".equalsIgnoreCase(githubActions);
  }

  protected void waitForAppToBeReady() {
    log.info("Waiting for Vaadin application to be ready...");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
    wait.until(
        d -> {
          try {
            Object result =
                ((JavascriptExecutor) d)
                    .executeScript(
                        "return window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients"
                            + " && Object.keys(window.Vaadin.Flow.clients).length > 0;");
            return Boolean.TRUE.equals(result);
          } catch (Exception e) {
            return false;
          }
        });
    log.info("Vaadin application is ready.");
  }

  protected void waitForVaadinToLoad(WebDriver driver) {
    waitForAppToBeReady();
  }

  protected void waitForVaadinClientToLoad() {
    waitForAppToBeReady();
  }

  protected void waitForVaadin(@NonNull Duration timeout) {
    new WebDriverWait(driver, timeout)
        .until(
            d ->
                (Boolean)
                    ((JavascriptExecutor) d)
                        .executeScript("return window.Vaadin.Flow.clients.isActive() === false;"));
  }

  protected String getUsername() {
    return "admin";
  }

  protected String getPassword() {
    return "admin123";
  }

  protected WebElement waitForVaadinElement(@NonNull WebDriver driver, @NonNull By locator) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
  }

  protected WebElement waitForVaadinElementVisible(@NonNull By locator) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
  }

  protected void click(@NonNull String tagName, @NonNull String text) {
    try {
      WebElement element =
          waitForVaadinElement(driver, By.xpath("//" + tagName + "[text()='" + text + "']"));
      clickElement(element);
    } catch (Exception e) {
      WebElement element =
          waitForVaadinElement(driver, By.xpath("//div[@role='tab'][text()='" + text + "']"));
      clickElement(element);
    }
  }

  protected void clickButtonByText(@NonNull String text) {
    WebElement element =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='" + text + "']"));
    clickElement(element);
  }

  protected void clickElement(@NonNull WebElement element) {
    try {
      element.click();
    } catch (Exception e) {
      ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }
  }

  protected void clickElement(@NonNull By locator) {
    WebElement element = waitForVaadinElement(driver, locator);
    clickElement(element);
  }

  protected void selectFromVaadinComboBox(@NonNull String id, @NonNull String itemText) {
    WebElement comboBox = waitForVaadinElement(driver, By.id(id));
    selectFromVaadinComboBox(comboBox, itemText);
  }

  protected void selectFromVaadinComboBox(@NonNull WebElement comboBox, @NonNull String itemText) {
    clickElement(comboBox);
    WebElement overlay = waitForVaadinElement(driver, By.tagName("vaadin-combo-box-overlay"));
    WebElement item =
        waitForVaadinElement(
            driver, By.xpath("//vaadin-combo-box-item[contains(text(), '" + itemText + "')]"));
    clickElement(item);
  }

  protected void selectFromVaadinMultiSelectComboBox(
      @NonNull WebElement comboBox, @NonNull String itemText) {
    clickElement(comboBox);
    WebElement item =
        waitForVaadinElement(
            driver,
            By.xpath("//vaadin-multi-select-combo-box-item[contains(text(), '" + itemText + "')]"));
    clickElement(item);
    // Click outside to close
    ((JavascriptExecutor) driver).executeScript("document.body.click();");
  }

  protected void selectFromVaadinMenuBar(@NonNull WebElement menuBar, @NonNull String itemText) {
    clickElement(menuBar);
    WebElement item =
        waitForVaadinElement(
            driver, By.xpath("//vaadin-context-menu-item[contains(text(), '" + itemText + "')]"));
    clickElement(item);
  }

  protected void toggleVaadinCheckbox(@NonNull By locator) {
    WebElement checkbox = waitForVaadinElement(driver, locator);
    clickElement(checkbox);
  }

  protected void waitForGridToPopulate(@NonNull String gridId) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        d -> {
          WebElement grid = d.findElement(By.id(gridId));
          List<WebElement> rows = grid.findElements(By.tagName("vaadin-grid-cell-content"));
          return !rows.isEmpty();
        });
  }

  protected void assertGridContains(@NonNull String gridId, @NonNull String text) {
    WebElement grid = driver.findElement(By.id(gridId));
    assertTrue(grid.getText().contains(text), "Grid " + gridId + " should contain text: " + text);
  }

  protected List<WebElement> getGridRows(@NonNull String gridId) {
    WebElement grid = driver.findElement(By.id(gridId));
    return grid.findElements(By.cssSelector("vaadin-grid-cell-content"));
  }

  protected String getColumnData(@NonNull WebElement row, int colIndex) {
    List<WebElement> cells = row.findElements(By.tagName("vaadin-grid-cell-content"));
    if (colIndex < cells.size()) {
      return cells.get(colIndex).getText();
    }
    return "";
  }

  @SuppressWarnings("unchecked")
  protected List<String> getGridColumnData(@NonNull WebElement grid, int colIndex) {
    return (List<String>)
        ((JavascriptExecutor) driver)
            .executeScript(
                "const grid = arguments[0];"
                    + "const colIndex = arguments[1];"
                    + "return Array.from(grid.querySelectorAll('vaadin-grid-cell-content'))"
                    + "  .filter(cell => {"
                    + "    const row = cell.assignedSlot.parentElement.parentElement;"
                    + "    return !row.hasAttribute('header');"
                    + "  })"
                    + "  .map(cell => cell.innerText);",
                grid,
                colIndex);
  }

  protected WebElement findButtonInGridRow(
      @NonNull String gridId, @NonNull String rowText, @NonNull By buttonLocator) {
    WebElement grid = driver.findElement(By.id(gridId));
    List<WebElement> rows = grid.findElements(By.tagName("vaadin-grid-row"));
    for (WebElement row : rows) {
      if (row.getText().contains(rowText)) {
        return row.findElement(buttonLocator);
      }
    }
    throw new RuntimeException("Could not find row with text: " + rowText);
  }

  protected void waitForGridToSettle(@NonNull String gridId, @NonNull Duration timeout) {
    new WebDriverWait(driver, timeout)
        .until(
            d -> {
              WebElement grid = d.findElement(By.id(gridId));
              return !(Boolean)
                  ((JavascriptExecutor) d)
                      .executeScript("return arguments[0].loading || arguments[0].pending;", grid);
            });
  }

  protected void waitForNotification(@NonNull String text) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        ExpectedConditions.presenceOfElementLocated(
            By.xpath("//vaadin-notification-card[contains(text(), '" + text + "')]")));
  }

  protected void waitForPageSourceToContain(@NonNull String text) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(d -> d.getPageSource().contains(text));
  }

  protected void scrollIntoView(@NonNull WebElement element) {
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
  }

  protected void takeSequencedScreenshot(@NonNull String label) {
    if (testArtifactsDir != null) {
      try {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String fileName = String.format("%03d-%s.png", ++screenshotCounter, label);
        FileUtils.copyFile(screenshot, testArtifactsDir.resolve(fileName).toFile());
        log.info("Screenshot saved to: {}", testArtifactsDir.resolve(fileName));
      } catch (IOException e) {
        log.error("Failed to take sequenced screenshot: {}", label, e);
      }
    }
  }

  protected void logout() {
    driver.get("http://localhost:" + serverPort + getContextPath());
    waitForVaadinElement(driver, By.id("logout-button"));
    WebElement logoutButton = driver.findElement(By.id("logout-button"));
    clickElement(logoutButton);
  }

  protected void clearField(@NonNull WebElement field) {
    takeSequencedScreenshot("before-clear");
    String os = System.getProperty("os.name").toLowerCase();
    Keys modifier = os.contains("mac") ? Keys.COMMAND : Keys.CONTROL;
    String selectAll = Keys.chord(modifier, "a");
    field.sendKeys(selectAll);
    field.sendKeys(Keys.BACK_SPACE);
    takeSequencedScreenshot("after-clear");
  }
}

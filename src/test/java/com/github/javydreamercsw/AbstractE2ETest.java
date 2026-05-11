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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.config.TestE2ESecurityConfig;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "e2e", inheritProfiles = false)
@Import(TestE2ESecurityConfig.class)
@Slf4j
@WithCustomMockUser(roles = {"ADMIN"})
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

  @Autowired protected ObjectMapper objectMapper;

  protected static WebDriver driver;
  private static boolean appReady = false;
  private static String lastLoggedInUser = null;

  @LocalServerPort protected int serverPort;

  private int screenshotCounter = 0;
  protected Path testArtifactsDir;

  static {
    // Register shutdown hook to close the shared driver
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (driver != null) {
                    log.info("Closing shared WebDriver...");
                    try {
                      driver.quit();
                    } catch (Exception e) {
                      log.warn("Error closing WebDriver in shutdown hook: {}", e.getMessage());
                    }
                  }
                }));
  }

  @BeforeEach
  public void setup(TestInfo testInfo) throws Exception {
    if (cacheManager != null) {
      cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    if (!appReady) {
      WebDriverManager.chromedriver().setup();
      log.info("Waiting for application to be ready on port {}", serverPort);
      waitForAppToBeReady();
      log.info("Application is ready on port {}", serverPort);
      appReady = true;
    }

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

    if (driver == null) {
      ChromeOptions options = new ChromeOptions();
      if (isHeadless()) {
        options.addArguments("--headless=new");
      }
      options.addArguments("--disable-gpu");
      options.addArguments("--window-size=1920,1080");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");
      options.addArguments("--reduce-security-for-testing");
      options.addArguments("--disable-notifications");
      options.addArguments("--disable-save-password-bubble");
      options.addArguments("--disable-infobars");
      options.addArguments("--disable-extensions");

      Map<String, Object> prefs = new HashMap<>();
      prefs.put("credentials_enable_service", false);
      prefs.put("profile.password_manager_enabled", false);
      prefs.put("profile.password_manager_leak_detection", false);
      options.setExperimentalOption("prefs", prefs);
      options.setExperimentalOption("excludeSwitches", new String[] {"enable-automation"});

      driver = new ChromeDriver(options);
      driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    // Only login if needed
    String currentUser = getUsername();
    boolean needsLogin = !Objects.equals(lastLoggedInUser, currentUser);

    if (!needsLogin) {
      // Even if the user matches, verify we are actually still logged in
      try {
        driver.findElement(By.id("logout-button"));
        log.debug("User {} is already logged in, skipping login flow.", currentUser);
      } catch (Exception e) {
        log.info("Logout button not found for user {}, forcing re-login.", currentUser);
        needsLogin = true;
      }
    }

    if (needsLogin) {
      // Clear cookies to ensure a fresh session if re-logging in
      if (driver != null) {
        try {
          driver.manage().deleteAllCookies();
        } catch (Exception e) {
          log.warn("Failed to clear cookies: {}", e.getMessage());
        }
      }
      login();
    } else // Ensure we are not on the login page
    if (Objects.requireNonNull(driver.getCurrentUrl()).endsWith("/login")) {
      driver.get("http://localhost:" + serverPort + getContextPath());
    }
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
      updateManifest(category, title, description, screenshotName);
    }
  }

  /**
   * Updates the documentation manifest with feature metadata.
   *
   * @param category Feature category
   * @param title Feature title
   * @param description Feature description
   * @param screenshotName Screenshot name (without path or extension)
   */
  @SuppressWarnings("unchecked")
  protected synchronized void updateManifest(
      String category, String title, String description, String screenshotName) {
    try {
      // Use relative path from the project root
      Path manifestPath = Paths.get("docs", "manifest.json");
      if (!Files.exists(manifestPath)) {
        log.warn("Manifest file not found at: {}", manifestPath.toAbsolutePath());
        return;
      }

      Map<String, Object> manifest =
          objectMapper.readValue(manifestPath.toFile(), new TypeReference<>() {});
      List<Map<String, Object>> features = (List<Map<String, Object>>) manifest.get("features");
      if (features == null) {
        features = new ArrayList<>();
        manifest.put("features", features);
      }

      String fileName = screenshotName.endsWith(".png") ? screenshotName : screenshotName + ".png";
      String imagePath = "screenshots/" + fileName;
      String id = category.toLowerCase().replace(" ", "-") + "-" + fileName.replace(".png", "");

      boolean found = false;
      for (Map<String, Object> feature : features) {
        if (id.equals(feature.get("id")) || imagePath.equals(feature.get("imagePath"))) {
          feature.put("category", category);
          feature.put("title", title);
          feature.put("description", description);
          feature.put("imagePath", imagePath);
          found = true;
          log.debug("Updated existing feature in manifest: {}", id);
          break;
        }
      }

      if (!found) {
        Map<String, Object> newFeature = new LinkedHashMap<>();
        newFeature.put("id", id);
        newFeature.put("category", category);
        newFeature.put("title", title);
        newFeature.put("description", description);
        newFeature.put("imagePath", imagePath);
        newFeature.put("order", 0);
        features.add(newFeature);
        log.info("Added new feature to manifest: {}", id);
      }

      objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);
    } catch (IOException e) {
      log.error("Failed to update documentation manifest", e);
    }
  }

  protected void login() {
    login(getUsername(), getPassword());
  }

  protected void login(@NonNull String username, @NonNull String password) {
    int maxRetries = 1;
    int attempt = 0;
    while (attempt++ < maxRetries) {
      try {
        log.info("Login attempt {} for user: {}", attempt, username);
        driver.get("http://localhost:" + serverPort + getContextPath() + "/login");
        waitForVaadinClientToLoad();
        takeSequencedScreenshot("on-login-page");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(2));
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
        WebElement passwordField = loginFormHost.findElement(By.id("vaadinLoginPassword"));

        // Determine the correct modifier key for the current OS
        String os = System.getProperty("os.name").toLowerCase();
        Keys modifier = os.contains("mac") ? Keys.COMMAND : Keys.CONTROL;

        // Use sendKeys which is more reliable for Vaadin components
        usernameField.click();
        usernameField.sendKeys(Keys.chord(modifier, "a"), Keys.BACK_SPACE);
        usernameField.sendKeys(username);

        passwordField.click();
        passwordField.sendKeys(Keys.chord(modifier, "a"), Keys.BACK_SPACE);
        passwordField.sendKeys(password);

        takeSequencedScreenshot("after-filling-credentials");
        WebElement signInButton =
            loginFormHost.findElement(By.cssSelector("vaadin-button[slot='submit']"));
        clickElement(signInButton);

        // Wait for successful login (logout button appears)
        WebDriverWait loginWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        loginWait.until(ExpectedConditions.presenceOfElementLocated(By.id("logout-button")));

        takeSequencedScreenshot("after-successful-login");
        log.info("Login successful for user: {}", username);
        lastLoggedInUser = username;
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
    // We no longer quit the driver here to allow reuse.
    // Instead, we navigate to a blank page and clear the state to ensure isolation.
    if (driver != null) {
      try {
        // Navigate to about:blank to stop any pending requests/scripts
        driver.get("about:blank");
        // Ensure no leftover dialogs are open
        ((JavascriptExecutor) driver)
            .executeScript(
                "const overlays = document.querySelectorAll('vaadin-dialog-overlay,"
                    + " vaadin-context-menu-overlay, vaadin-select-overlay,"
                    + " vaadin-combo-box-overlay');overlays.forEach(o => { try { o.opened = false;"
                    + " o.remove(); } catch(e){} });");
      } catch (Exception e) {
        log.warn("Error during browser reset in teardown: {}", e.getMessage());
        // If the driver is in a really bad state, force it to be recreated next time
        try {
          driver.quit();
        } catch (Exception ignored) {
        }
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
    int maxAttempts = 300; // Increased from 60 to handle slow production builds
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
   * Waits for the Vaadin components to load and become idle.
   *
   * @param driver the WebDriver instance
   */
  protected void waitForVaadinToLoad(@NonNull WebDriver driver) {
    waitForVaadinClientToLoad();
  }

  protected WebElement waitForVaadinElement(@NonNull WebDriver driver, @NonNull By selector) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    return wait.until(ExpectedConditions.presenceOfElementLocated(selector));
  }

  protected WebElement waitForVaadinElementVisible(@NonNull By selector) {
    waitForVaadinClientToLoad();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    return wait.until(ExpectedConditions.visibilityOfElementLocated(selector));
  }

  protected void waitForGridToPopulate(@NonNull String gridId) {
    // Delegate to the more robust 'settled' wait.
    waitForGridToSettle(gridId, Duration.ofSeconds(30));

    // Keep the previous semantics: we expect at least one row.
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        d -> {
          try {
            WebElement grid = d.findElement(By.id(gridId));
            List<WebElement> rows = grid.findElements(By.tagName("vaadin-grid-cell-content"));
            return !rows.isEmpty();
          } catch (Exception e) {
            return false;
          }
        });
  }

  protected void waitForVaadinClientToLoad() {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

    // Wait for document.readyState to be 'complete'
    wait.until(
        webDriver ->
            Objects.equals(
                ((JavascriptExecutor) webDriver).executeScript("return document.readyState"),
                "complete"));

    // Wait for Vaadin to be present and idle
    wait.until(
        webDriver -> {
          try {
            return (Boolean)
                ((JavascriptExecutor) webDriver)
                    .executeScript(
                        "return !!(window.Vaadin && window.Vaadin.Flow &&"
                            + " window.Vaadin.Flow.clients &&"
                            + " Object.values(window.Vaadin.Flow.clients).every(client =>"
                            + " !client.isActive()));");
          } catch (Exception e) {
            return false;
          }
        });

    // Wait for the main Vaadin app layout element to be present (best effort)
    try {
      wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-app-layout")));
    } catch (Exception ignored) {
      // Not all pages might have vaadin-app-layout
    }
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

  /**
   * Scrolls the given WebElement into view and clicks it using JavaScript.
   *
   * @param element the WebElement to scroll into view and click
   */
  protected void clickElement(@NonNull WebElement element) {
    scrollIntoView(element);
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("before-click");
    // First, wait for the element to be visible.
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(ExpectedConditions.visibilityOf(element));
    wait.until(ExpectedConditions.elementToBeClickable(element));

    // Ensure the drawer is closed if it might intercept clicks
    try {
      Boolean drawerWasOpened =
          (Boolean)
              ((JavascriptExecutor) driver)
                  .executeScript(
                      "const layout = document.querySelector('vaadin-app-layout');"
                          + "if (layout && layout.drawerOpened) { "
                          + "  layout.drawerOpened = false; "
                          + "  return true; "
                          + "} return false;");
      if (Boolean.TRUE.equals(drawerWasOpened)) {
        // Give drawer time to close only if it was actually opened
        Thread.sleep(300);
      }
    } catch (Exception e) {
      log.warn("Could not ensure drawer was closed", e);
    }

    try {
      // Attempt native Selenium click first
      element.click();
    } catch (Exception e) {
      log.warn("Native click failed, falling back to JS click: {}", e.getMessage());
      // Use a simple JavaScript click as it's more reliable
      try {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        Thread.sleep(200); // Give time for the click to register
      } catch (Exception jsError) {
        log.error("JavaScript click also failed, using event dispatch", jsError);
        // Last resort: dispatch events
        ((JavascriptExecutor) driver)
            .executeScript(
                "const el = arguments[0];"
                    + "const opts = {view: window, bubbles: true, cancelable: true};"
                    + "el.dispatchEvent(new MouseEvent('mousedown', opts));"
                    + "el.dispatchEvent(new MouseEvent('mouseup', opts));"
                    + "el.dispatchEvent(new MouseEvent('click', opts));",
                element);
      }
    }
    takeSequencedScreenshot("after-click");
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
    scrollIntoView(comboBox);
    waitForVaadinClientToLoad();

    // Open the dropdown via JS (shadow DOM prevents normal click from opening it)
    ((JavascriptExecutor) driver).executeScript("arguments[0].opened = true;", comboBox);

    // Wait for the overlay to appear in the DOM (may be attached to document body)
    new WebDriverWait(driver, Duration.ofSeconds(30))
        .until(
            d ->
                (Boolean)
                    ((JavascriptExecutor) d)
                        .executeScript(
                            "var el = arguments[0]; return el.opened === true"
                                + " && !!el._overlayElement;",
                            comboBox));

    WebElement item =
        waitForVaadinElement(
            driver, By.xpath("//vaadin-combo-box-item[contains(text(), '" + itemText + "')]"));
    clickElement(item);
  }

  protected void selectFromVaadinMultiSelectComboBox(
      @NonNull WebElement comboBox, @NonNull String itemText) {
    log.info("Selecting item '{}' from MultiSelectComboBox", itemText);
    JavascriptExecutor js = (JavascriptExecutor) driver;

    // Open the dropdown via JS property
    js.executeScript("arguments[0].opened = true;", comboBox);

    // Wait for an item that is both present AND visible (non-zero bounding box).
    // Checking visibility avoids matching items in previously-closed overlays
    // that are still in the DOM but hidden.
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    WebElement item =
        wait.until(
            d -> {
              Object found =
                  js.executeScript(
                      "const items=Array.from(document.querySelectorAll("
                          + "'vaadin-multi-select-combo-box-item'));"
                          + "return items.find(el=>{"
                          + "const r=el.getBoundingClientRect();"
                          + "return r.width>0&&r.height>0"
                          + "&&(el.innerText||el.textContent).trim().includes(arguments[0]);"
                          + "})||null;",
                      itemText);
              return found instanceof WebElement ? (WebElement) found : null;
            });

    // Use native Selenium click so Vaadin's event handlers fire properly
    scrollIntoView(item);
    item.click();

    // Close the overlay
    js.executeScript("arguments[0].opened = false;", comboBox);
  }

  protected void selectFromVaadinMenuBar(@NonNull WebElement menuBar, @NonNull String itemText) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    // In Vaadin 25, vaadin-menu-bar has vaadin-menu-bar-button in its light DOM.
    // Click the first one (the "Actions" trigger) to open the submenu.
    js.executeScript(
        "const bar=arguments[0];"
            + "const btn=bar.querySelector('vaadin-menu-bar-button')||bar;"
            + "btn.click();",
        menuBar);

    // After click, the open vaadin-menu-bar-submenu contains a DIV with a
    // vaadin-menu-bar-list-box whose children are the clickable menu items.
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    WebElement item =
        wait.until(
            d -> {
              Object found =
                  js.executeScript(
                      "const"
                          + " openSub=Array.from(document.querySelectorAll('vaadin-menu-bar-submenu')).find(s=>s.opened);if(!openSub)return"
                          + " null;const"
                          + " listBox=openSub.querySelector('vaadin-menu-bar-list-box');if(!listBox)return"
                          + " null;return"
                          + " Array.from(listBox.children).find(el=>el.textContent.trim().includes(arguments[0]))||null;",
                      itemText);
              return found instanceof WebElement ? (WebElement) found : null;
            });
    js.executeScript("arguments[0].click();", item);
  }

  protected void toggleVaadinCheckbox(@NonNull By locator) {
    WebElement checkbox = waitForVaadinElement(driver, locator);
    clickElement(checkbox);
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
                "const grid=arguments[0];const colIndex=arguments[1];const"
                    + " allCells=Array.from(grid.querySelectorAll('vaadin-grid-cell-content'));const"
                    + " rowCellMap=new Map();for(const cell of allCells){const"
                    + " slot=cell.assignedSlot;if(!slot)continue;const"
                    + " gridCell=slot.parentElement;if(!gridCell)continue;const"
                    + " part=gridCell.getAttribute('part')||'';"
                    + "if(!part.includes('body-cell'))continue;const"
                    + " row=gridCell.parentElement;if(!row)continue;const"
                    + " ci=Array.from(row.children).indexOf(gridCell);"
                    + "if(!rowCellMap.has(row))rowCellMap.set(row,{});"
                    + "rowCellMap.get(row)[ci]=cell.innerText.trim();}const"
                    + " result=[];for(const[row,cells]of rowCellMap){const"
                    + " hasContent=Object.values(cells).some(v=>v!=='');if(hasContent&&colIndex in"
                    + " cells)result.push(cells[colIndex]);}return result;",
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
              Object result =
                  ((JavascriptExecutor) d)
                      .executeScript(
                          "return !!(arguments[0].loading || arguments[0].pending);", grid);
              return result == null || !(Boolean) result;
            });
  }

  protected void waitForNotification(@NonNull String text) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        ExpectedConditions.presenceOfElementLocated(
            By.xpath("//vaadin-notification-card[contains(text(), '" + text + "')]")));
  }

  protected void scrollIntoView(@NonNull WebElement element) {
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
  }

  protected void takeSequencedScreenshot(@NonNull String context) {
    if (Boolean.getBoolean("enable.screenshots") && driver != null && testArtifactsDir != null) {
      screenshotCounter++;
      String screenshotName = String.format("%03d-%s.png", screenshotCounter, context);
      Path destFile = testArtifactsDir.resolve(screenshotName);
      takeScreenshot(destFile.toString());
    }
  }

  protected void takeScreenshot(@NonNull String filePath) {
    if (driver == null) {
      log.warn("Cannot take screenshot: WebDriver is null");
      return;
    }
    try {
      File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
      FileUtils.copyFile(scrFile, new File(filePath));
      log.debug("Screenshot saved to: {}", filePath);
    } catch (org.openqa.selenium.WebDriverException e) {
      log.warn(
          "WebDriverException while taking screenshot: {}. This might happen during page"
              + " navigation.",
          e.getMessage());
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

  protected void savePageSource(@NonNull String filePath) {
    try {
      FileUtils.writeStringToFile(new File(filePath), driver.getPageSource(), "UTF-8");
      log.info("Page source saved to: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to save page source to: {}", filePath, e);
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

  protected String getVaadinTextFieldErrorMessage(@NonNull String textFieldId) {
    WebElement textFieldElement = driver.findElement(By.id(textFieldId));

    // Check if the field is invalid
    Boolean isInvalid =
        (Boolean)
            ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].hasAttribute('invalid');", textFieldElement);

    if (Boolean.TRUE.equals(isInvalid)) {
      // Access the shadow root of the vaadin-text-field
      SearchContext shadowRoot = textFieldElement.getShadowRoot();

      // Find the error message element within the shadow root
      List<WebElement> errorMessageElements =
          shadowRoot.findElements(By.cssSelector("[part='error-message']"));

      if (!errorMessageElements.isEmpty()) {
        WebElement errorMessageElement = errorMessageElements.getFirst();
        if (errorMessageElement.isDisplayed()) {
          return errorMessageElement.getText();
        }
      }
    }
    return null; // Or throw an exception if the field is not invalid
  }

  protected void waitForPageSourceToContain(@NonNull String text) {
    new WebDriverWait(driver, java.time.Duration.ofSeconds(30))
        .until(d -> Objects.requireNonNull(d.getPageSource()).contains(text));
  }
}

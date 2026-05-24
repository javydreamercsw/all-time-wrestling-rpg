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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
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
@ExtendWith(UITestWatcher.class)
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

  @Autowired protected ObjectMapper objectMapper;

  protected static WebDriver driver;
  private static boolean appReady = false;
  private static String lastLoggedInUser = null;

  @LocalServerPort protected int serverPort;

  private int screenshotCounter = 0;
  protected Path testArtifactsDir;

  private String videoCategory;
  private String videoTitle;
  private String videoName;

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
  public void setup(final TestInfo testInfo) throws Exception {
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

      for (int attempt = 1; ; attempt++) {
        try {
          driver = new ChromeDriver(options);
          break;
        } catch (Exception ex) {
          if (attempt >= 3) {
            throw ex;
          }
          log.warn(
              "ChromeDriver start failed (attempt {}), retrying: {}", attempt, ex.getMessage());
          Thread.sleep(2000);
        }
      }
      driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
      driver.manage().timeouts().pageLoadTimeout(Duration.ofMinutes(2));
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
        log.debug("Logout button not found for user {}, forcing re-login.", currentUser);
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

  protected void takeDocScreenshot(@NonNull final String fileName) {
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
      @NonNull final String category,
      @NonNull final String title,
      @NonNull final String description,
      @NonNull final String screenshotName) {
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
      final String category,
      final String title,
      final String description,
      final String screenshotName) {
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

  protected void login(@NonNull final String username, @NonNull final String password) {
    int maxRetries = 3;
    String loginUrl = "http://localhost:" + serverPort + getContextPath() + "/login";
    Exception lastException = null;
    for (int attempt = 0; attempt < maxRetries; attempt++) {
      try {
        log.debug("Login attempt {}/{} for user: {}", attempt + 1, maxRetries, username);
        // Skip navigation if already on the login page (e.g. right after logout())
        if (!loginUrl.equals(driver.getCurrentUrl())) {
          driver.get(loginUrl);
        }
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
        log.debug("Login successful for user: {}", username);
        lastLoggedInUser = username;
        return; // Success!
      } catch (Exception e) {
        lastException = e;
        log.warn("Login attempt {}/{} failed: {}", attempt + 1, maxRetries, e.getMessage());
        // Force a fresh navigation before retrying
        try {
          driver.get(loginUrl);
        } catch (Exception ignored) {
        }
        // Brief pause before retrying
        try {
          Thread.sleep(2000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
    log.error("All {} login attempts failed for user: {}", maxRetries, username);
    takeSequencedScreenshot("on-login-final-failure");
    throw new RuntimeException("Login failed after " + maxRetries + " attempts", lastException);
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
                """
                const overlays = document.querySelectorAll('vaadin-dialog-overlay,\
                 vaadin-context-menu-overlay, vaadin-select-overlay,\
                 vaadin-combo-box-overlay');overlays.forEach(o => { try { o.opened = false;\
                 o.remove(); } catch(e){} });\
                """);
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
      return "true".equalsIgnoreCase(headlessProp);
    }
    if (headlessEnv != null) {
      return "true".equalsIgnoreCase(headlessEnv);
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

  protected WebElement waitForVaadinElementVisible(@NonNull final By selector) {
    waitForVaadinClientToLoad();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    return wait.until(ExpectedConditions.visibilityOfElementLocated(selector));
  }

  protected void waitForGridToPopulate(@NonNull final String gridId) {
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
                        """
                        return !!(window.Vaadin && window.Vaadin.Flow &&\
                         window.Vaadin.Flow.clients &&\
                         Object.values(window.Vaadin.Flow.clients).every(client =>\
                         !client.isActive()));\
                        """);
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

    // Wait for at least two animation frames so headless Chrome completes its CSS layout pass.
    // In headless mode, Vite's async CSS injection may finish after Vaadin's Flow clients go idle,
    // causing screenshots to be taken before utility classes (e.g. flex layout) take effect.
    try {
      ((JavascriptExecutor) driver)
          .executeAsyncScript(
              "const done = arguments[arguments.length - 1];"
                  + "requestAnimationFrame(() => requestAnimationFrame(done));");
    } catch (Exception ignored) {
      // Non-critical; proceed even if rAF is unavailable
    }
  }

  protected void waitForVaadin(@NonNull final Duration timeout) {
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

  protected void click(@NonNull final String tagName, @NonNull final String text) {
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

  protected void clickButtonByText(@NonNull final String text) {
    WebElement element =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='" + text + "']"));
    clickElement(element);
  }

  /**
   * Scrolls the given WebElement into view and clicks it using JavaScript.
   *
   * @param element the WebElement to scroll into view and click
   */
  protected void clickElement(@NonNull final WebElement element) {
    scrollIntoView(element);
    waitForVaadinClientToLoad();
    takeSequencedScreenshot("before-click");
    // Wait for the element to become visible (short window — elements inside vaadin-details or
    // animated containers may be present but hidden; fall through to JS click in that case).
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    boolean visible;
    try {
      new WebDriverWait(driver, Duration.ofSeconds(5))
          .until(ExpectedConditions.visibilityOf(element));
      visible = true;
    } catch (org.openqa.selenium.TimeoutException ignored) {
      visible = false;
    } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
      visible = false;
    }
    if (visible) {
      wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    // Ensure the drawer is closed if it might intercept clicks
    try {
      Boolean drawerWasOpened =
          (Boolean)
              ((JavascriptExecutor) driver)
                  .executeScript(
                      """
                      const layout = document.querySelector('vaadin-app-layout');\
                      if (layout && layout.drawerOpened) { \
                        layout.drawerOpened = false; \
                        return true; \
                      } return false;\
                      """);
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
                """
                const el = arguments[0];\
                const opts = {view: window, bubbles: true, cancelable: true};\
                el.dispatchEvent(new MouseEvent('mousedown', opts));\
                el.dispatchEvent(new MouseEvent('mouseup', opts));\
                el.dispatchEvent(new MouseEvent('click', opts));\
                """,
                element);
      }
    }
    takeSequencedScreenshot("after-click");
  }

  protected void clickElement(@NonNull final By locator) {
    int maxRetries = 3;
    for (int attempt = 1; attempt < maxRetries + 1; attempt++) {
      try {
        WebElement element = waitForVaadinElement(driver, locator);
        clickElement(element);
        return;
      } catch (org.openqa.selenium.StaleElementReferenceException e) {
        if (attempt == maxRetries) {
          throw e;
        }
        log.warn(
            "StaleElementReferenceException in clickElement(By), retry {}/{}", attempt, maxRetries);
      }
    }
  }

  protected void selectFromVaadinComboBox(
      @NonNull final String id, @NonNull final String itemText) {
    WebElement comboBox = waitForVaadinElement(driver, By.id(id));
    selectFromVaadinComboBox(comboBox, itemText);
  }

  protected void selectFromVaadinComboBox(
      @NonNull final WebElement comboBox, @NonNull final String itemText) {
    scrollIntoView(comboBox);
    waitForVaadinClientToLoad();

    JavascriptExecutor js = (JavascriptExecutor) driver;

    // Click the shadow-root input to trigger Vaadin's overlay lifecycle, then filter
    js.executeScript(
        """
        const el = arguments[0];
        const input = el.shadowRoot && el.shadowRoot.querySelector('input');
        if (input) { input.focus(); input.click(); }
        el.opened = true;
        el.filter = arguments[1];
        """,
        comboBox,
        itemText);

    // Wait for a visible combo-box item whose text matches.
    // If no items appear, retry opening (handles lazy data providers or slow Vaadin push).
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    WebElement item =
        wait.until(
            d -> {
              Object found =
                  js.executeScript(
                      """
                      const items = Array.from(document.querySelectorAll(\
                      'vaadin-combo-box-item'));\
                      const visible = items.filter(el => {\
                        const r = el.getBoundingClientRect();\
                        return r.width > 0 && r.height > 0;\
                      });\
                      if (visible.length === 0) {\
                        const cb = arguments[1];\
                        const inp = cb.shadowRoot && cb.shadowRoot.querySelector('input');\
                        if (inp) { inp.click(); }\
                        cb.opened = true;\
                      }\
                      return visible.find(\
                        el => (el.innerText || el.textContent).trim().includes(arguments[0])\
                      ) || null;\
                      """,
                      itemText,
                      comboBox);
              return found instanceof WebElement ? (WebElement) found : null;
            });

    scrollIntoView(item);
    item.click();
  }

  protected void selectFromVaadinMultiSelectComboBox(
      @NonNull final WebElement comboBox, @NonNull final String itemText) {
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
                      """
                      const items=Array.from(document.querySelectorAll(\
                      'vaadin-multi-select-combo-box-item'));\
                      return items.find(el=>{\
                      const r=el.getBoundingClientRect();\
                      return r.width>0&&r.height>0\
                      &&(el.innerText||el.textContent).trim().includes(arguments[0]);\
                      })||null;\
                      """,
                      itemText);
              return found instanceof WebElement ? (WebElement) found : null;
            });

    // Use native Selenium click so Vaadin's event handlers fire properly
    scrollIntoView(item);
    item.click();

    // Close the overlay
    js.executeScript("arguments[0].opened = false;", comboBox);
  }

  protected void selectFromVaadinMenuBar(
      @NonNull final WebElement menuBar, @NonNull final String itemText) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    // In Vaadin 25, vaadin-menu-bar has vaadin-menu-bar-button in its light DOM.
    // Click the first one (the "Actions" trigger) to open the submenu.
    js.executeScript(
        """
        const bar=arguments[0];\
        const btn=bar.querySelector('vaadin-menu-bar-button')||bar;\
        btn.click();\
        """,
        menuBar);

    // After click, the open vaadin-menu-bar-submenu contains a DIV with a
    // vaadin-menu-bar-list-box whose children are the clickable menu items.
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    WebElement item =
        wait.until(
            d -> {
              Object found =
                  js.executeScript(
                      """
                      const\
                       openSub=Array.from(document.querySelectorAll('vaadin-menu-bar-submenu')).find(s=>s.opened);if(!openSub)return\
                       null;const\
                       listBox=openSub.querySelector('vaadin-menu-bar-list-box');if(!listBox)return\
                       null;return\
                       Array.from(listBox.children).find(el=>el.textContent.trim().includes(arguments[0]))||null;\
                      """,
                      itemText);
              return found instanceof WebElement ? (WebElement) found : null;
            });
    js.executeScript("arguments[0].click();", item);
  }

  protected void toggleVaadinCheckbox(@NonNull final By locator) {
    WebElement checkbox = waitForVaadinElement(driver, locator);
    scrollIntoView(checkbox);
    waitForVaadinClientToLoad();
    // Click the shadow DOM <input> directly; outer-element clicks don't always reach it in
    // headless Chrome when the vaadin-checkbox shadow root intercepts the event.
    ((JavascriptExecutor) driver)
        .executeScript(
            """
            const cb = arguments[0];
            const input = cb.shadowRoot ? cb.shadowRoot.querySelector('input') : null;
            if (input) { input.click(); } else { cb.click(); }
            """,
            checkbox);
  }

  protected void assertGridContains(@NonNull final String gridId, @NonNull final String text) {
    WebElement grid = driver.findElement(By.id(gridId));
    JavascriptExecutor js = (JavascriptExecutor) driver;
    // Scroll grid to end so Vaadin virtualizes all rows into the DOM, then scan cell text.
    js.executeScript(
        "arguments[0].scrollToIndex(arguments[0].items ? arguments[0].items.length - 1 : 9999);",
        grid);
    waitForVaadinClientToLoad();
    Boolean found =
        (Boolean)
            js.executeScript(
                """
                const grid=arguments[0];const needle=arguments[1];
                return Array.from(grid.querySelectorAll('vaadin-grid-cell-content'))
                  .some(c=>c.innerText&&c.innerText.includes(needle));
                """,
                grid,
                text);
    assertEquals(Boolean.TRUE, found, "Grid " + gridId + " should contain text: " + text);
  }

  protected List<WebElement> getGridRows(@NonNull final String gridId) {
    WebElement grid = driver.findElement(By.id(gridId));
    return grid.findElements(By.cssSelector("vaadin-grid-cell-content"));
  }

  protected String getColumnData(@NonNull final WebElement row, final int colIndex) {
    List<WebElement> cells = row.findElements(By.tagName("vaadin-grid-cell-content"));
    if (colIndex < cells.size()) {
      return cells.get(colIndex).getText();
    }
    return "";
  }

  @SuppressWarnings("unchecked")
  protected List<String> getGridColumnData(@NonNull final WebElement grid, final int colIndex) {
    return (List<String>)
        ((JavascriptExecutor) driver)
            .executeScript(
                """
                const grid=arguments[0];const colIndex=arguments[1];const\
                 allCells=Array.from(grid.querySelectorAll('vaadin-grid-cell-content'));const\
                 rowCellMap=new Map();for(const cell of allCells){const\
                 slot=cell.assignedSlot;if(!slot)continue;const\
                 gridCell=slot.parentElement;if(!gridCell)continue;const\
                 part=gridCell.getAttribute('part')||'';\
                if(!part.includes('body-cell'))continue;const\
                 row=gridCell.parentElement;if(!row)continue;const\
                 ci=Array.from(row.children).indexOf(gridCell);\
                if(!rowCellMap.has(row))rowCellMap.set(row,{});\
                rowCellMap.get(row)[ci]=cell.innerText.trim();}const\
                 result=[];for(const[row,cells]of rowCellMap){const\
                 hasContent=Object.values(cells).some(v=>v!=='');if(hasContent&&colIndex in\
                 cells)result.push(cells[colIndex]);}return result;\
                """,
                grid,
                colIndex);
  }

  protected WebElement findButtonInGridRow(
      @NonNull final String gridId,
      @NonNull final String rowText,
      @NonNull final By buttonLocator) {
    WebElement grid = driver.findElement(By.id(gridId));
    JavascriptExecutor js = (JavascriptExecutor) driver;

    // Scroll grid to end so all rows are virtualized into the DOM
    js.executeScript(
        "arguments[0].scrollToIndex(arguments[0].items ? arguments[0].items.length - 1 : 9999);",
        grid);
    waitForVaadinClientToLoad();

    // Vaadin Grid uses slot-based rendering: vaadin-grid-cell-content elements in the light
    // DOM carry a slot attribute like "vaadin-grid-cell-content-{rowIdx}-{colIdx}". Extract
    // the row prefix from the matching cell and then check all sibling cells (same row) for
    // the target button — no shadow DOM traversal needed.
    String buttonCss = buttonLocator.toString().replace("By.cssSelector: ", "");
    Object raw =
        js.executeScript(
            """
            const grid = arguments[0];
            const needle = arguments[1];
            const css = arguments[2];
            const cells = Array.from(grid.querySelectorAll('vaadin-grid-cell-content'));
            const matchCell = cells.find(c => (c.innerText || c.textContent || '').includes(needle));
            if (!matchCell) return 'ERR:no_cell';
            const slotName = matchCell.getAttribute('slot') || '';
            // slot name format: vaadin-grid-cell-content-<rowIdx>-<colIdx>
            const parts = slotName.split('-');
            // last two parts are colIdx and rowIdx; row prefix is everything except last segment
            if (parts.length < 2) return 'ERR:bad_slot:' + slotName;
            const rowPrefix = parts.slice(0, parts.length - 1).join('-');
            // Find all sibling cells in the same row
            const rowCells = cells.filter(c => {
              const s = c.getAttribute('slot') || '';
              return s.startsWith(rowPrefix + '-');
            });
            for (const cell of rowCells) {
              const found = cell.querySelector(css);
              if (found) return found;
            }
            return 'ERR:no_button_in_row:' + rowPrefix + ':cells=' + rowCells.length;
            """,
            grid,
            rowText,
            buttonCss);

    if (!(raw instanceof WebElement button)) {
      Object debugResult =
          js.executeScript(
              """
              const grid = arguments[0];
              const cells = Array.from(grid.querySelectorAll('vaadin-grid-cell-content'));
              return cells.map(c => (c.getAttribute('slot') || '') + '|' + (c.innerText || c.textContent || '').substring(0, 60)).join('\\n');
              """,
              grid);
      throw new RuntimeException(
          "Could not find row with text: "
              + rowText
              + " | debug="
              + raw
              + " | cells="
              + debugResult);
    }
    return button;
  }

  protected void waitForGridToSettle(
      @NonNull final String gridId, @NonNull final Duration timeout) {
    new WebDriverWait(driver, timeout)
        .until(
            d -> {
              try {
                WebElement grid = d.findElement(By.id(gridId));
                Object result =
                    ((JavascriptExecutor) d)
                        .executeScript(
                            "return !!(arguments[0].loading || arguments[0].pending);", grid);
                return result == null || !(Boolean) result;
              } catch (org.openqa.selenium.NoSuchElementException e) {
                return false;
              }
            });
  }

  protected void waitForNotification(@NonNull final String text) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        ExpectedConditions.presenceOfElementLocated(
            By.xpath("//vaadin-notification-card[contains(text(), '" + text + "')]")));
  }

  protected void scrollIntoView(@NonNull final WebElement element) {
    try {
      ((JavascriptExecutor) driver)
          .executeScript(
              "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", element);
    } catch (org.openqa.selenium.StaleElementReferenceException e) {
      log.warn("StaleElementReferenceException in scrollIntoView — element was re-rendered");
    }
  }

  protected void takeSequencedScreenshot(@NonNull final String context) {
    if (Boolean.getBoolean("enable.screenshots") && driver != null && testArtifactsDir != null) {
      screenshotCounter++;
      String screenshotName = "%03d-%s.png".formatted(screenshotCounter, context);
      Path destFile = testArtifactsDir.resolve(screenshotName);
      takeScreenshot(destFile.toString());
    }
  }

  protected void takeScreenshot(@NonNull final String filePath) {
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
          """
          WebDriverException while taking screenshot: {}. This might happen during page\
           navigation.\
          """,
          e.getMessage());
    } catch (IOException e) {
      log.error("Failed to save screenshot to: {}", filePath, e);
    }
  }

  protected void takeElementScreenshot(
      @NonNull final WebElement element, @NonNull final String filePath) {
    File scrFile = element.getScreenshotAs(OutputType.FILE);
    try {
      FileUtils.copyFile(scrFile, new File(filePath));
      log.info("Screenshot saved to: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to save screenshot to: {}", filePath, e);
    }
  }

  protected void savePageSource(@NonNull final String filePath) {
    try {
      FileUtils.writeStringToFile(new File(filePath), driver.getPageSource(), "UTF-8");
      log.info("Page source saved to: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to save page source to: {}", filePath, e);
    }
  }

  protected void navigateTo(@NonNull final String route) {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/" + route);
    waitForVaadinClientToLoad();
  }

  protected void logout() {
    driver.get("http://localhost:" + serverPort + getContextPath());
    waitForVaadinElement(driver, By.id("logout-button"));
    WebElement logoutButton = driver.findElement(By.id("logout-button"));
    clickElement(logoutButton);
    // Wait for the logout redirect to complete naturally before navigating. Calling driver.get()
    // immediately after the click can abort the in-flight redirect and leave the Vaadin session
    // in an inconsistent state, causing the login page not to render.
    String loginUrl = "http://localhost:" + serverPort + getContextPath() + "/login";
    try {
      new WebDriverWait(driver, Duration.ofSeconds(10))
          .until(d -> Objects.requireNonNull(d.getCurrentUrl()).contains("/login"));
    } catch (org.openqa.selenium.TimeoutException ignored) {
      // Vaadin logout may not redirect in E2E mode (anyRequest().permitAll()), so navigate directly
      driver.get(loginUrl);
    }
    lastLoggedInUser = null;
  }

  protected void clearField(@NonNull final WebElement field) {
    takeSequencedScreenshot("before-clear");
    String os = System.getProperty("os.name").toLowerCase();
    Keys modifier = os.contains("mac") ? Keys.COMMAND : Keys.CONTROL;
    String selectAll = Keys.chord(modifier, "a");
    field.sendKeys(selectAll);
    field.sendKeys(Keys.BACK_SPACE);
    takeSequencedScreenshot("after-clear");
  }

  protected String getVaadinTextFieldErrorMessage(@NonNull final String textFieldId) {
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

  protected void waitForPageSourceToContain(@NonNull final String text) {
    new WebDriverWait(driver, java.time.Duration.ofSeconds(30))
        .until(d -> Objects.requireNonNull(d.getPageSource()).contains(text));
  }

  // -------------------------------------------------------------------------
  // Video documentation helpers (generate.videos system property gates all)
  // -------------------------------------------------------------------------

  /** One timed caption: description text starts at this frame index. */
  private record CaptionSegment(int startFrame, String description) {}

  private static final class FrameCaptureSession {
    final Path frameDir;
    final AtomicInteger frameIndex = new AtomicInteger(0);
    final AtomicBoolean running = new AtomicBoolean(true);
    final List<CaptionSegment> captions = new ArrayList<>();
    final long startMs = System.currentTimeMillis();
    Thread captureThread;

    FrameCaptureSession(Path frameDir) {
      this.frameDir = frameDir;
    }
  }

  // Per-test video session — started by AbstractVideoDocsE2ETest @BeforeEach, finished by
  // @AfterEach
  private FrameCaptureSession activeVideoSession;

  /**
   * Starts frame capture for the current test. Called by AbstractVideoDocsE2ETest before each test
   * when {@code generate.videos=true}.
   */
  protected void startVideoCapture(@NonNull String testName) {
    if (!Boolean.getBoolean("generate.videos")) {
      return;
    }
    Path frameDir;
    try {
      frameDir = Files.createTempDirectory("atw-video-" + testName + "-");
    } catch (IOException e) {
      log.warn("Cannot create frame temp dir, video will be skipped: {}", e.getMessage());
      return;
    }
    FrameCaptureSession session = new FrameCaptureSession(frameDir);
    Thread t =
        new Thread(
            () -> {
              while (session.running.get()) {
                try {
                  File frame = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                  String name = "frame-%04d.png".formatted(session.frameIndex.getAndIncrement());
                  FileUtils.copyFile(frame, session.frameDir.resolve(name).toFile());
                } catch (Exception e) {
                  log.warn("Frame capture error: {}", e.getMessage());
                }
                try {
                  Thread.sleep(125); // ~8 fps target (actual rate limited by screenshot speed)
                } catch (InterruptedException ie) {
                  Thread.currentThread().interrupt();
                  break;
                }
              }
            },
            "video-frame-capture");
    t.setDaemon(true);
    session.captureThread = t;
    t.start();
    activeVideoSession = session;
  }

  /**
   * Marks a timed caption at the current frame index. Call this from a video test after navigating
   * to a new state to describe what is visible on screen.
   */
  protected void captureCaption(@NonNull String description) {
    if (activeVideoSession != null) {
      activeVideoSession.captions.add(
          new CaptionSegment(activeVideoSession.frameIndex.get(), description));
    }
  }

  /**
   * Marks a timed caption and then holds on the current screen state for {@code dwellMs}
   * milliseconds before returning. Use this in video tests where you want the viewer to have more
   * time to read the caption and absorb what is on screen.
   */
  protected void captureCaption(@NonNull String description, int dwellMs) {
    captureCaption(description);
    try {
      Thread.sleep(dwellMs);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Stops frame capture and assembles the MP4. Called by AbstractVideoDocsE2ETest after each test.
   *
   * @param category manifest category
   * @param title manifest title
   * @param videoName output file base name (no extension)
   */
  protected void finishVideoCapture(
      @NonNull String category, @NonNull String title, @NonNull String videoName) {
    if (activeVideoSession == null) {
      return;
    }
    FrameCaptureSession session = activeVideoSession;
    activeVideoSession = null;

    // Hold 2s after the last captureCaption() so the final state is visible in the video
    long elapsedMs = System.currentTimeMillis() - session.startMs;
    long holdMs = Math.max(0, 2000 - elapsedMs);
    if (holdMs > 0) {
      try {
        Thread.sleep(holdMs);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }

    session.running.set(false);
    try {
      session.captureThread.join(3000);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }

    int frameCount = session.frameIndex.get();
    if (frameCount == 0) {
      log.warn("No frames captured for video '{}', skipping assembly", videoName);
      return;
    }

    // Compute actual capture rate from wall time so ffmpeg plays at real speed
    long totalMs = System.currentTimeMillis() - session.startMs;
    double actualFps = Math.max(1.0, frameCount / Math.max(1.0, totalMs / 1000.0));

    try {
      Path videosDir = Paths.get("docs", "videos");
      Files.createDirectories(videosDir);

      burnCaptionsOntoFrames(session.captions, session.frameDir, frameCount);

      Optional<Path> audioFile = Optional.empty();
      if (Boolean.getBoolean("generate.video.voice") && !session.captions.isEmpty()) {
        String fullScript =
            session.captions.stream()
                .map(CaptionSegment::description)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ". " + b);
        audioFile = generateNarrationAudio(fullScript, session.frameDir);
      }

      Path outputMp4 = videosDir.resolve(videoName + ".mp4");
      runFfmpeg(session.frameDir, audioFile, outputMp4, actualFps);

      String description =
          session.captions.isEmpty()
              ? ""
              : session.captions.get(session.captions.size() - 1).description();
      updateVideoManifest(category, title, description, videoName);
      log.info("Video documentation saved: {}", outputMp4);
    } catch (Exception e) {
      log.error("Failed to assemble video '{}': {}", videoName, e.getMessage(), e);
    }
  }

  /** Burns timed captions onto frames using Java2D — no libass/freetype needed. */
  private void burnCaptionsOntoFrames(List<CaptionSegment> captions, Path frameDir, int frameCount)
      throws IOException {
    if (captions.isEmpty()) {
      return;
    }
    // Build lookup: for each frame, which caption applies
    for (int i = 0; i < frameCount; i++) {
      String text = captionForFrame(captions, i);
      if (text == null) {
        continue;
      }
      Path framePath = frameDir.resolve("frame-%04d.png".formatted(i));
      if (!Files.exists(framePath)) {
        continue;
      }
      java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(framePath.toFile());
      if (img == null) {
        continue;
      }
      burnTextOntoImage(img, text);
      javax.imageio.ImageIO.write(img, "png", framePath.toFile());
    }
  }

  private String captionForFrame(List<CaptionSegment> captions, int frameIndex) {
    String active = null;
    for (CaptionSegment seg : captions) {
      if (seg.startFrame() <= frameIndex) {
        active = seg.description();
      } else {
        break;
      }
    }
    return active;
  }

  private void burnTextOntoImage(java.awt.image.BufferedImage img, String text) {
    int w = img.getWidth();
    int h = img.getHeight();
    java.awt.Graphics2D g = img.createGraphics();
    g.setRenderingHint(
        java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    int fontSize = Math.max(16, w / 60);
    java.awt.Font font = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, fontSize);
    g.setFont(font);
    java.awt.FontMetrics fm = g.getFontMetrics();

    int maxWidth = (int) (w * 0.90);
    java.util.List<String> lines = wrapText(text.replace('\n', ' ').trim(), fm, maxWidth);

    int lineHeight = fm.getHeight();
    int totalTextHeight = lines.size() * lineHeight + 8;
    int boxY = h - totalTextHeight - 20;
    int boxX = (int) (w * 0.05);
    int boxW = (int) (w * 0.90);

    g.setColor(new java.awt.Color(0, 0, 0, 180));
    g.fillRoundRect(boxX - 8, boxY - 4, boxW + 16, totalTextHeight + 8, 10, 10);

    g.setColor(java.awt.Color.WHITE);
    int y = boxY + fm.getAscent();
    for (String line : lines) {
      int x = boxX + (boxW - fm.stringWidth(line)) / 2;
      g.drawString(line, x, y);
      y += lineHeight;
    }
    g.dispose();
  }

  private java.util.List<String> wrapText(String text, java.awt.FontMetrics fm, int maxWidth) {
    java.util.List<String> lines = new java.util.ArrayList<>();
    String[] words = text.split(" ");
    StringBuilder current = new StringBuilder();
    for (String word : words) {
      String candidate = current.isEmpty() ? word : current + " " + word;
      if (fm.stringWidth(candidate) <= maxWidth) {
        current = new StringBuilder(candidate);
      } else {
        if (!current.isEmpty()) {
          lines.add(current.toString());
        }
        current = new StringBuilder(word);
      }
    }
    if (!current.isEmpty()) {
      lines.add(current.toString());
    }
    return lines;
  }

  private Optional<Path> generateNarrationAudio(String text, Path dir) {
    String os = System.getProperty("os.name", "").toLowerCase();
    Path audioFile = dir.resolve("narration.wav");
    try {
      ProcessBuilder pb;
      if (os.contains("mac")) {
        Path aiff = dir.resolve("narration.aiff");
        new ProcessBuilder("/usr/bin/say", "-o", aiff.toString(), text)
            .redirectErrorStream(true)
            .start()
            .waitFor();
        pb = new ProcessBuilder("ffmpeg", "-y", "-i", aiff.toString(), audioFile.toString());
      } else {
        pb = new ProcessBuilder("espeak-ng", "-w", audioFile.toString(), text);
      }
      pb.redirectErrorStream(true);
      int rc = pb.start().waitFor();
      if (rc == 0 && Files.exists(audioFile)) {
        return Optional.of(audioFile);
      }
      log.warn("TTS command exited with code {}", rc);
    } catch (Exception e) {
      log.warn("TTS generation failed (voice will be skipped): {}", e.getMessage());
    }
    return Optional.empty();
  }

  private void runFfmpeg(Path frameDir, Optional<Path> audioFile, Path outputMp4, double fps)
      throws Exception {
    List<String> cmd = new ArrayList<>();
    cmd.add("ffmpeg");
    cmd.add("-y");
    cmd.add("-framerate");
    // Round to one decimal; ffmpeg accepts fractional framerates
    cmd.add("%.2f".formatted(fps));
    cmd.add("-i");
    cmd.add(frameDir.resolve("frame-%04d.png").toString());
    if (audioFile.isPresent()) {
      cmd.add("-i");
      cmd.add(audioFile.get().toString());
    }
    // Ensure even dimensions and convert rgb24 PNG frames to yuv420p (required by libx264)
    cmd.add("-vf");
    cmd.add("scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p");
    cmd.add("-c:v");
    cmd.add("libx264");
    cmd.add("-crf");
    cmd.add("28");
    if (audioFile.isPresent()) {
      cmd.add("-c:a");
      cmd.add("libmp3lame");
      cmd.add("-shortest");
    }
    cmd.add(outputMp4.toString());

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(true);
    Process process = pb.start();
    // Drain stdout/stderr on a separate thread to prevent pipe-buffer deadlock
    StringBuilder output = new StringBuilder();
    Thread reader =
        new Thread(
            () -> {
              try (java.io.BufferedReader br =
                  new java.io.BufferedReader(
                      new java.io.InputStreamReader(process.getInputStream()))) {
                br.lines().forEach(line -> output.append(line).append('\n'));
              } catch (IOException ignored) {
              }
            });
    reader.start();
    int rc = process.waitFor();
    reader.join(5000);
    if (rc != 0) {
      throw new RuntimeException("ffmpeg failed (exit " + rc + "): " + output);
    }
  }

  @SuppressWarnings("unchecked")
  protected synchronized void updateVideoManifest(
      final String category, final String title, final String description, final String videoName) {
    try {
      Path manifestPath = Paths.get("docs", "video-manifest.json");
      Files.createDirectories(manifestPath.getParent());

      Map<String, Object> manifest;
      if (Files.exists(manifestPath)) {
        manifest = objectMapper.readValue(manifestPath.toFile(), new TypeReference<>() {});
      } else {
        manifest = new LinkedHashMap<>();
      }

      List<Map<String, Object>> videos = (List<Map<String, Object>>) manifest.get("videos");
      if (videos == null) {
        videos = new ArrayList<>();
        manifest.put("videos", videos);
      }

      String videoPath = "videos/" + videoName + ".mp4";
      String id = category.toLowerCase().replace(" ", "-") + "-" + videoName;

      boolean found = false;
      for (Map<String, Object> entry : videos) {
        if (id.equals(entry.get("id")) || videoPath.equals(entry.get("videoPath"))) {
          entry.put("category", category);
          entry.put("title", title);
          entry.put("description", description);
          entry.put("videoPath", videoPath);
          found = true;
          break;
        }
      }

      if (!found) {
        Map<String, Object> newEntry = new LinkedHashMap<>();
        newEntry.put("id", id);
        newEntry.put("category", category);
        newEntry.put("title", title);
        newEntry.put("description", description);
        newEntry.put("videoPath", videoPath);
        newEntry.put("order", 0);
        videos.add(newEntry);
      }

      objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);
    } catch (IOException e) {
      log.error("Failed to update video manifest", e);
    }
  }

  protected void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Sets the video metadata for the current test. Must be called once per {@code @Test} method
   * before any interactions so the assembled MP4 gets the correct filename and manifest entry.
   */
  protected void setVideoInfo(
      @NonNull String category, @NonNull String title, @NonNull String videoName) {
    this.videoCategory = category;
    this.videoTitle = title;
    this.videoName = videoName;
  }

  @BeforeEach
  final void startVideoRecording(TestInfo testInfo) {
    videoCategory = null;
    videoTitle = null;
    videoName = null;
    String safeName =
        testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
    startVideoCapture(safeName);
  }

  @AfterEach
  final void stopVideoRecording() {
    if (videoName != null) {
      finishVideoCapture(videoCategory, videoTitle, videoName);
    } else {
      // setVideoInfo() was never called — stop capture thread and discard frames
      discardVideoCapture();
    }
  }

  private void discardVideoCapture() {
    FrameCaptureSession session = activeVideoSession;
    activeVideoSession = null;
    if (session == null) {
      return;
    }
    session.running.set(false);
    try {
      session.captureThread.join(3000);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
    try {
      if (Files.exists(session.frameDir)) {
        try (var stream = Files.walk(session.frameDir)) {
          stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
      }
    } catch (IOException e) {
      log.debug("Could not clean up discard frame dir: {}", e.getMessage());
    }
  }
}

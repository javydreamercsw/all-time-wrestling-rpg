package com.github.javydreamercsw;

import com.github.javydreamercsw.base.test.AbstractIntegrationTest;
import com.github.javydreamercsw.management.domain.feud.MultiWrestlerFeudRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.title.TitleService;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(UITestWatcher.class)
@Slf4j
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

  protected WebDriver driver;
  protected static int serverPort;
  private static final ConfigurableApplicationContext context;
  protected static InboxRepository inboxRepository; // Added static InboxRepository
  protected static WrestlerRepository wrestlerRepository;
  protected static MultiWrestlerFeudService multiWrestlerFeudService;
  protected static SeasonRepository seasonRepository;
  protected static SegmentService segmentService;
  protected static SeasonService seasonService;
  protected static RivalryService rivalryService;
  protected static TitleService titleService;
  protected static ShowService showService;
  protected static SegmentTypeService segmentTypeService;
  protected static SegmentRuleService segmentRuleService;
  protected static SegmentRepository segmentRepository;
  protected static MultiWrestlerFeudRepository multiWrestlerFeudRepository;

  @Value("${server.servlet.context-path}")
  @Getter
  private String contextPath;

  static {
    serverPort = Integer.parseInt(System.getProperty("server.port", "9090"));
    String[] args = {
      "--server.port=" + serverPort, "--spring.profiles.active=test",
    };
    log.info("Attempting to start Spring Boot application for E2E tests on port {}", serverPort);
    context = SpringApplication.run(Application.class, args);
    inboxRepository = context.getBean(InboxRepository.class); // Get InboxRepository from context
    wrestlerRepository = context.getBean(WrestlerRepository.class);
    multiWrestlerFeudService = context.getBean(MultiWrestlerFeudService.class);
    seasonRepository = context.getBean(SeasonRepository.class);
    segmentService = context.getBean(SegmentService.class);
    seasonService = context.getBean(SeasonService.class);
    rivalryService = context.getBean(RivalryService.class);
    titleService = context.getBean(TitleService.class);
    showService = context.getBean(ShowService.class);
    segmentTypeService = context.getBean(SegmentTypeService.class);
    segmentRuleService = context.getBean(SegmentRuleService.class);
    segmentRepository = context.getBean(SegmentRepository.class);
    multiWrestlerFeudRepository = context.getBean(MultiWrestlerFeudRepository.class);
    log.info("Spring Boot application started for E2E tests.");
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Shutting down Spring Boot application for E2E tests.");
                  context.close();
                }));
  }

  @BeforeEach
  public void setup() throws java.io.IOException {
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
    if (driver == null) {
      driver = new ChromeDriver(options);
    }
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

  protected void waitForVaadinToLoad(@NonNull WebDriver driver) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));
  }

  protected void clickVaadinButton(@NonNull String vaadinSelector) {
    WebElement button = getVaadinElementInShadowRoot(vaadinSelector, "button");
    if (button != null) {
      button.click();
    } else {
      throw new RuntimeException("Vaadin button not found: " + vaadinSelector);
    }
  }

  protected void scrollIntoView(@NonNull WebElement element) {
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
  }

  protected WebElement getVaadinElementInShadowRoot(
      @NonNull String vaadinSelector, @NonNull String shadowRootSelector) {
    return (WebElement)
        ((JavascriptExecutor) driver)
            .executeScript(
                "return"
                    + " document.querySelector(arguments[0]).shadowRoot.querySelector(arguments[1]);",
                vaadinSelector,
                shadowRootSelector);
  }
}

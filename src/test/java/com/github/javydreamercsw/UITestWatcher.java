package com.github.javydreamercsw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class UITestWatcher implements AfterTestExecutionCallback {

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    if (context.getExecutionException().isPresent()) {
      context
          .getTestInstance()
          .ifPresent(
              testInstance -> {
                if (testInstance instanceof AbstractE2ETest) {
                  WebDriver driver = ((AbstractE2ETest) testInstance).driver;
                  if (driver != null) {
                    // Print the page source to the console
                    System.out.println("--- Page Source on Failure ---");
                    String pageSource = driver.getPageSource();
                    System.out.println(pageSource);
                    System.out.println("--- End Page Source ---");

                    try {
                      // Create a directory for the test output
                      Path outputDir =
                          Paths.get(
                              "target", "test-failures", context.getRequiredTestMethod().getName());
                      Files.createDirectories(outputDir);

                      // Capture screenshot
                      File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                      Path screenshotPath = outputDir.resolve("failure.png");
                      Files.copy(screenshot.toPath(), screenshotPath);
                      System.out.println("Screenshot saved to: " + screenshotPath);

                      // Save page source to file
                      Path pageSourcePath = outputDir.resolve("failure.html");
                      Files.writeString(pageSourcePath, pageSource);
                      System.out.println("Page source saved to: " + pageSourcePath);

                    } catch (IOException e) {
                      System.err.println(
                          "Failed to capture screenshot or page source: " + e.getMessage());
                    }
                  }
                }
              });
    }
  }
}

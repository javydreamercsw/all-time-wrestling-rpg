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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

@Slf4j
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
                    log.debug("--- Page Source on Failure ---");
                    String pageSource = driver.getPageSource();
                    log.debug(pageSource);
                    log.debug("--- End Page Source ---");

                    try {
                      // Create a directory for the test output
                      Path outputDir =
                          Paths.get(
                              "target",
                              "test-failures",
                              context.getTestClass().get().getSimpleName(),
                              context.getRequiredTestMethod().getName());
                      Files.createDirectories(outputDir);

                      String testName = context.getDisplayName().replaceAll("[^a-zA-Z0-9.-]", "_");

                      // Capture screenshot
                      File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                      Path screenshotPath = outputDir.resolve("failure-" + testName + ".png");
                      Files.copy(
                          screenshot.toPath(), screenshotPath, StandardCopyOption.REPLACE_EXISTING);
                      log.debug("Screenshot saved to: {}", screenshotPath);

                      // Save page source to file
                      Path pageSourcePath = outputDir.resolve("failure-" + testName + ".html");
                      if (pageSource == null) {
                        pageSource = "";
                        log.debug("Page source was null!");
                      }
                      Files.writeString(pageSourcePath, pageSource);
                      log.debug("Page source saved to: {}", pageSourcePath);
                    } catch (IOException e) {
                      log.error("Failed to capture screenshot or page source: {}", e.getMessage());
                    }
                  }
                }
              });
    }
  }
}

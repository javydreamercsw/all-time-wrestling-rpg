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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.security.test.context.support.WithMockUser;

public class DataTransferE2ETest extends AbstractE2ETest {

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testNavigateToDataTransferView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/data-transfer");
    WebElement dataTransferWizard = waitForVaadinElement(driver, By.id("data-transfer-wizard"));
    assertNotNull(dataTransferWizard);
  }
}

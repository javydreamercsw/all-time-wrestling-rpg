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
package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EntityDependencyAnalyzerTest extends ManagementIntegrationTest {

  @Autowired private EntityDependencyAnalyzer entityDependencyAnalyzer;

  @Test
  void testGetAutomaticSyncOrder() {
    List<String> automaticSyncOrder = entityDependencyAnalyzer.getAutomaticSyncOrder();
    Assertions.assertFalse(automaticSyncOrder.isEmpty());
  }
}

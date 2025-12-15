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

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class EntityDependencyAnalyzerTest extends ManagementIntegrationTest {

  @Autowired private EntityDependencyAnalyzer entityDependencyAnalyzer;
  @MockBean private DataInitializer dataInitializer; // Exclude DataInitializer

  @Test
  void testGetAutomaticSyncOrder() {
    List<SyncEntityType> automaticSyncOrder = entityDependencyAnalyzer.getAutomaticSyncOrder();
    Assertions.assertFalse(automaticSyncOrder.isEmpty());
  }

  @Test
  void testDetermineSyncOrder() {
    Set<Class<?>> entities =
        new HashSet<>(Arrays.asList(EntityA.class, EntityB.class, EntityC.class, EntityD.class));

    List<String> syncOrder = entityDependencyAnalyzer.determineSyncOrder(entities);

    Assertions.assertEquals(4, syncOrder.size());
    Assertions.assertEquals("entityd", syncOrder.get(0));
    Assertions.assertEquals("entityb", syncOrder.get(1));
    Assertions.assertEquals("entityc", syncOrder.get(2));
    Assertions.assertEquals("entitya", syncOrder.get(3));
  }

  @Entity
  private static class EntityA {
    @Id private Long id;

    @ManyToOne private EntityB b;

    @ManyToOne private EntityC c;
  }

  @Entity
  private static class EntityB {
    @Id private Long id;

    @ManyToOne private EntityD d;
  }

  @Entity
  private static class EntityC {
    @Id private Long id;

    @ManyToOne private EntityD d;
  }

  @Entity
  private static class EntityD {
    @Id private Long id;
  }
}

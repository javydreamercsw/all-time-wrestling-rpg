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
package com.github.javydreamercsw.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DataInitializerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private DataInitializer dataInitializer;
  @Autowired private WrestlerRepository wrestlerRepository;

  @Test
  void testWrestlerDataIsNotLostOnSync() {
    // 1. Create a Wrestler object with specific values.
    Wrestler existingWrestler = new Wrestler();
    existingWrestler.setName(UUID.randomUUID().toString());
    existingWrestler.setFans(100L);
    existingWrestler.setBumps(5);
    existingWrestler.setImageUrl("http://example.com/image.png");
    existingWrestler.setDeckSize(10);
    existingWrestler.setStartingHealth(10);
    existingWrestler.setLowHealth(1);
    existingWrestler.setStartingStamina(10);
    existingWrestler.setLowStamina(1);
    existingWrestler.setDescription("desc");
    existingWrestler.setGender(Gender.MALE);
    wrestlerRepository.save(existingWrestler);

    // 2. dataInitializer.init() will be called automatically on startup,
    //    but we call it here explicitly to make sure it runs after our setup.
    //    The wrestlers.json in test resources should have a "Test Wrestler"
    //    without fans, bumps, and imageUrl.
    TestUtils.runAsAdmin(() -> dataInitializer.init());

    // 3. Verify the wrestler data is not lost and other data is updated.
    Wrestler updatedWrestler = wrestlerRepository.findByName(existingWrestler.getName()).get();
    assertNotNull(updatedWrestler);

    // Check that the values from the JSON file were updated
    assertEquals(existingWrestler.getDeckSize(), updatedWrestler.getDeckSize());
    assertEquals(existingWrestler.getStartingHealth(), updatedWrestler.getStartingHealth());

    // Check that the values not in the JSON file were preserved
    assertEquals(existingWrestler.getFans(), updatedWrestler.getFans());
    assertEquals(existingWrestler.getBumps(), updatedWrestler.getBumps());
    assertEquals(existingWrestler.getImageUrl(), updatedWrestler.getImageUrl());
  }
}

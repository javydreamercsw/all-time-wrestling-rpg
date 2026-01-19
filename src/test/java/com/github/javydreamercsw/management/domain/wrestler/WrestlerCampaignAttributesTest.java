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
package com.github.javydreamercsw.management.domain.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class WrestlerCampaignAttributesTest extends AbstractIntegrationTest {

  @Test
  @Transactional
  void testCampaignAttributesPersistence() {
    Wrestler wrestler = createTestWrestler("Attribute Tester");
    wrestler.setDrive(4);
    wrestler.setResilience(5);
    wrestler.setCharisma(6);
    wrestler.setBrawl(3);

    wrestler = wrestlerRepository.save(wrestler);

    Wrestler fetched = wrestlerRepository.findById(wrestler.getId()).orElseThrow();

    assertThat(fetched.getDrive()).isEqualTo(4);
    assertThat(fetched.getResilience()).isEqualTo(5);
    assertThat(fetched.getCharisma()).isEqualTo(6);
    assertThat(fetched.getBrawl()).isEqualTo(3);
  }

  @Test
  @Transactional
  void testDefaultAttributes() {
    Wrestler wrestler = createTestWrestler("Default Tester");
    // Don't set attributes, check defaults
    wrestler = wrestlerRepository.save(wrestler);

    Wrestler fetched = wrestlerRepository.findById(wrestler.getId()).orElseThrow();

    assertThat(fetched.getDrive()).isEqualTo(1);
    assertThat(fetched.getResilience()).isEqualTo(1);
    assertThat(fetched.getCharisma()).isEqualTo(1);
    assertThat(fetched.getBrawl()).isEqualTo(1);
  }
}

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

import com.github.javydreamercsw.base.AccountInitializer;
import com.github.javydreamercsw.base.service.ranking.RankingService;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base class for {@link DataJpaTest}s in the management module. Provides mock beans for common
 * services that are not part of the JPA context.
 */
@DataJpaTest
public abstract class ManagementDataJpaTest {
  @MockitoBean protected AccountInitializer accountInitializer;
  @MockitoBean protected DataInitializer dataInitializer;
  @MockitoBean protected RankingService rankingService;
}
